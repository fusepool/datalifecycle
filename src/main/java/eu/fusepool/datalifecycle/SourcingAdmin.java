package eu.fusepool.datalifecycle;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.security.AccessController;
import java.security.AllPermission;
import java.security.Permission;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.locks.Lock;

import javax.ws.rs.FormParam;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.apache.clerezza.jaxrs.utils.RedirectUtil;
import org.apache.clerezza.jaxrs.utils.TrailingSlash;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.NonLiteral;
import org.apache.clerezza.rdf.core.Resource;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.access.EntityAlreadyExistsException;
import org.apache.clerezza.rdf.core.access.LockableMGraph;
import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.rdf.core.access.security.TcAccessController;
import org.apache.clerezza.rdf.core.access.security.TcPermission;
import org.apache.clerezza.rdf.core.impl.PlainLiteralImpl;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.clerezza.rdf.core.serializedform.Parser;
import org.apache.clerezza.rdf.core.serializedform.Serializer;
import org.apache.clerezza.rdf.core.serializedform.SupportedFormat;
import org.apache.clerezza.rdf.ontologies.DCTERMS;
import org.apache.clerezza.rdf.ontologies.OWL;
import org.apache.clerezza.rdf.ontologies.RDF;
import org.apache.clerezza.rdf.ontologies.RDFS;
import org.apache.clerezza.rdf.utils.GraphNode;
import org.apache.clerezza.rdf.utils.UnionMGraph;
//import org.apache.clerezza.rdf.utils.smushing.SameAsSmusher;
import org.apache.commons.io.IOUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.indexedgraph.IndexedMGraph;
import org.apache.stanbol.commons.web.viewable.RdfViewable;
import org.apache.stanbol.entityhub.servicesapi.site.SiteManager;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Uses the SiteManager to resolve entities. Every requested is recorded to a
 * graph. The client gets information and meta-information about the resource
 * and sees all previous requests for that resource.
 */
@Component
@Property(name = "javax.ws.rs", boolValue = true)
@Service(Object.class)
@Path("sourcing")
public class SourcingAdmin {

    /**
     * Using slf4j for normal logging
     */
    private static final Logger log = LoggerFactory.getLogger(SourcingAdmin.class);

    @Reference
    private Parser parser;

    @Reference
    private Serializer serializer;

    /**
     * This service allows accessing and creating persistent triple collections
     */
    @Reference
    private TcManager tcManager;

    private Interlinker interlinker;
    
    @Reference(target="(extractorType=patent)")
    private RdfDigester patentDigester;
    
    @Reference(target="(extractorType=pubmed)")
    private RdfDigester pubmedDigester;
    /**
     * This is the name of the graph in which we "log" the requests
     */
    //private UriRef REQUEST_LOG_GRAPH_NAME = new UriRef("http://example.org/resource-resolver-log.graph");
    /**
     * Name of the data life cycle graph. It is used as a register of other
     * graphs to manage their life cycle
     */
    private UriRef DATA_LIFECYCLE_GRAPH_REFERENCE = new UriRef("urn:x-localinstance:/datalifecycle/meta.graph");
    
    /**
     * Reference of the patent dataset to which all the patent RDF data will be moved after
     * reconciliation, smushing and URI rewrite. 
     */
    private UriRef PATENT_DATA_GRAPH_REFERENCE = new UriRef("http://fusepool.info/patent.graph");

    /**
     * Register graph referencing graphs for life cycle monitoring;
     */
    private final String CONTENT_GRAPH_NAME = "urn:x-localinstance:/content.graph";

    private UriRef CONTENT_GRAPH_REF = new UriRef(CONTENT_GRAPH_NAME);

    private final int ADD_TRIPLES_OPERATION = 1;
    private final int REMOVE_ALL_TRIPLES_OPERATION = 2;
    private final int DELETE_GRAPH_OPERATION = 3;
    private final int RECONCILE_GRAPH_OPERATION = 4;
    private final int SMUSH_GRAPH_OPERATION = 5;
    private final int RECONCILE_SMUSH_OPERATION = 6;
    private final int RECONCILE_AGAINST_DATASET_GRAPH = 7;
    private final int MOVE_DOCSET_TO_DATASET_GRAPH = 8;
    private final int PATENT_TEXT_EXTRACTION = 9;
    private final int PUBMED_TEXT_EXTRACTION = 10;
    
    //TODO make this a component parameter
    // URI for rewriting from urn scheme to http
    private String baseURI = "http://beta.fusepool.com/ecs/content/";
    // Union of all the equivalence sets (linksets)
    private UriRef OWL_SAME_AS_GRAPH = new UriRef("urn:x-localinstance:/datalifecycle/sameas.graph");

    @Activate
    protected void activate(ComponentContext context) {

        log.info("The Data Life Cycle service is being activated");
        try {
            if (interlinker != null) {
                log.info("Silk interlinker service available");
            } else {
                log.info("Silk interlinker service NOT available");
            }
            // Creates the data lifecycle graph if it doesn't exists. This graph contains references to graphs and linksets  
            try {
                createDlcGraph();
                log.info("Created Data Lifecycle Register Graph. This graph will reference all graphs during their lifecycle");
            } catch (EntityAlreadyExistsException ex) {
                log.info("Data Lifecycle Graph already exists.");
            }
            // Creates a graph to store the union of all the equivalence sets of the reconciliation task that have been done 
            // on a graph against itself and the dataset graph
            try {
                tcManager.createMGraph(OWL_SAME_AS_GRAPH);
                log.info("Created OWL Same As Graph. This graphs will contain owl:sameAs statemenets");
            } catch (EntityAlreadyExistsException ex) {
                log.info("OWL Same As Graph already exists.");
            }
            // Creates a graph to store all the patent data 
            try {
                tcManager.createMGraph(PATENT_DATA_GRAPH_REFERENCE);
                log.info("Created Patent Dataset Graph. This graphs will contain all the patent data that will be published on the platform");
            } catch (EntityAlreadyExistsException ex) {
                log.info("Patent Dataset Graph already exists.");
            }

        } catch (EntityAlreadyExistsException ex) {
            log.debug("The graph for the request log already exists");
        }

    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        log.info("The Data Life Cycle service is being deactivated");
    }

    /**
     * This method return an RdfViewable, this is an RDF serviceUri with
     * associated presentational information.
     */
    @GET
    public RdfViewable serviceEntry(@Context final UriInfo uriInfo,
            @QueryParam("url") final UriRef url,
            @HeaderParam("user-agent") String userAgent) throws Exception {
        //this maks sure we are nt invoked with a trailing slash which would affect
        //relative resolution of links (e.g. css)
        TrailingSlash.enforcePresent(uriInfo);

        final String resourcePath = uriInfo.getAbsolutePath().toString();
        if (url != null) {
            String query = url.toString();
            log.info(query);
        }

        //The URI at which this service was accessed, this will be the 
        //central serviceUri in the response
        final UriRef serviceUri = new UriRef(resourcePath);
        //the in memory graph to which the triples for the response are added
        //final MGraph responseGraph = new IndexedMGraph();
        //This GraphNode represents the service within our result graph
        //final GraphNode node = new GraphNode(serviceUri, responseGraph);
        //node.addProperty(Ontology.graph, new UriRef("http://fusepool.com/graphs/patentdata"));
        //node.addPropertyValue(RDFS.label, "A graph of patent data");
        //What we return is the GraphNode we created with a template path
        final GraphNode node = new GraphNode(DATA_LIFECYCLE_GRAPH_REFERENCE, getDlcGraph());

        return new RdfViewable("SourcingAdmin", node, SourcingAdmin.class);
    }

    /**
     * Creates a new empty graph
     *
     * @param uriInfo
     * @param graphName
     * @return
     * @throws Exception
     */
    @POST
    @Path("create_graph")
    @Produces("text/plain")
    public Response createGraphCommand(@Context final UriInfo uriInfo,
            @FormParam("graph") final String graphName,
            @FormParam("graph_label") final String graphLabel) throws Exception {
        AccessController.checkPermission(new AllPermission());
        //some simplicistic (and too restrictive) validation
        try {
            new URI(graphName);
        } catch (URISyntaxException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Graphname is not a valid URI: " + e.getReason()).build();
        }
        if (!graphName.contains(":")) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Graphname is not a valid URI: No colon separating scheme").build();
        }
        AccessController.checkPermission(new AllPermission());
        if (createGraph(graphName, graphLabel)) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Graph " + graphName + " already exists.").build();
        } else {
            return RedirectUtil.createSeeOtherResponse("./", uriInfo);

        }

    }

    /**
     * Creates a new graph and adds its uri and label to the data life cycle graph and to a dataset graph 
     * (currently just patent dataset graph but should be passed as argument)
     *
     * @return
     */
    private boolean createGraph(String graphName, String graphLabel) {
        boolean graphExists = false;
        String label = "";
        if (graphLabel != null) {
            label = graphLabel;
        }
        UriRef graphRef = new UriRef(graphName);
        try {
            if (graphExists(new UriRef(graphName))) {
                graphExists = true;
            } else {
                tcManager.createMGraph(graphRef);
                GraphNode dlcGraphNode = new GraphNode(DATA_LIFECYCLE_GRAPH_REFERENCE, getDlcGraph());
                dlcGraphNode.addProperty(DCTERMS.hasPart, graphRef);
                getDlcGraph().add(new TripleImpl(graphRef, RDFS.label, new PlainLiteralImpl("This graph will be moved to the its dataset graph")));
                // add a reference to the patent graph that will store the graph after any operation
                // this reference will be a reference to the content-graph.
                getDlcGraph().add(new TripleImpl(PATENT_DATA_GRAPH_REFERENCE, DCTERMS.source, graphRef));
                
            }
        } catch (UnsupportedOperationException uoe) {
            log.error("Error while creating a graph");
        }

        return graphExists;
    }

    /**
     * Applies one of the following operations to a graph: - add triples
     * (operation code: 1) - remove all triples (operation code: 2) - delete
     * graph (operation code: 3) - reconcile (operation code: 4) - smush
     * (operation code: 5)
     */
    @POST
    @Path("operate")
    @Produces("text/plain")
    public String operateOnGraphCommand(@Context final UriInfo uriInfo,
            @FormParam("graph") final UriRef graphRef,
            @FormParam("operation_code") final int operationCode,
            @FormParam("data_url") final URL dataUrl,
            @HeaderParam("Content-Type") String mediaType) throws Exception {
        AccessController.checkPermission(new AllPermission());

        // validate arguments and handle all the connection exceptions
        return operateOnGraph(graphRef, operationCode, dataUrl, mediaType);

    }

    private String operateOnGraph(UriRef graphRef, int operationCode, URL dataUrl, String mediaType) throws Exception {
        AccessController.checkPermission(new AllPermission());
        String message = "";
        if (graphExists(graphRef)) {
            switch (operationCode) {
                case ADD_TRIPLES_OPERATION:
                    message = addTriples(graphRef, dataUrl, mediaType);
                    break;
                case REMOVE_ALL_TRIPLES_OPERATION:
                    message = emptyGraph(graphRef);
                    break;
                case DELETE_GRAPH_OPERATION:
                    message = deleteGraph(graphRef);
                    break;
                case RECONCILE_GRAPH_OPERATION:
                    message = reconcile(graphRef, null);
                    break;
                case SMUSH_GRAPH_OPERATION:
                    message = smush(graphRef);
                    break;
                case RECONCILE_SMUSH_OPERATION:
                    message = reconcileSmush(graphRef, dataUrl, mediaType);
                    break;
                case RECONCILE_AGAINST_DATASET_GRAPH:
                	message = reconcile(graphRef, PATENT_DATA_GRAPH_REFERENCE);
                	break;
                case MOVE_DOCSET_TO_DATASET_GRAPH:
                	message = moveToDatasetGraph(graphRef, PATENT_DATA_GRAPH_REFERENCE);
                	break;
                case PATENT_TEXT_EXTRACTION:
                	message = extractTextFromPatent(graphRef);
                	break;
                case PUBMED_TEXT_EXTRACTION:
                	message = extractTextFromPubMed(graphRef);
                	break;
            }
        } else {
            message = "The graph " + graphRef.getUnicodeString() + " does not exist.";
        }

        return message;

    }
    
    /**
     * Load RDF data into an existing graph from a URL (schemes: "file://" or "http://"). 
     * The arguments to be passed are: 
     * 1) graph in which the RDF data must be stored
     * 2) url of the dataset 
     * After the upload the input graph is sent to a digester to extract text for indexing and 
     * adding entities found by NLP components (in the default chain) as subject
     */
    private String addTriples(UriRef graphRef, URL dataUrl, String mediaType) throws Exception {
    	AccessController.checkPermission(new AllPermission());
        String message = "";

        // add the triples of the temporary graph into the graph selected by the user
        if (graphExists(graphRef)) {
        	
        	MGraph updatedGraph = addTriplesCommand(graphRef, dataUrl, mediaType);

            message = "Added " + updatedGraph.size() + " triples to " + graphRef.getUnicodeString() + "\n";

        } else {
            message = "The graph " + graphRef.getUnicodeString() + " does not exist. It must be created before adding triples.\n";
        }
       
        log.info(message);
        return message;

    	
    }

    private MGraph addTriplesCommand(UriRef graphRef, URL dataUrl, String mediaType) throws Exception {
        AccessController.checkPermission(new AllPermission());
        MGraph graph = null;
        URLConnection connection = dataUrl.openConnection();
        connection.addRequestProperty("Accept", "application/rdf+xml; q=.9, text/turte;q=1");
        String currentTime = String.valueOf(System.currentTimeMillis());

        // create a temporary graph to store the data        
        SimpleMGraph tempGraph = new SimpleMGraph();

        InputStream data = connection.getInputStream();
        if (data != null) {
            if (mediaType.equals("application/x-www-form-urlencoded")) {
                mediaType = getContentTypeFromUrl(dataUrl);
            }
            parser.parse(tempGraph, data, mediaType);

            // add the triples of the temporary graph into the graph selected by the user
            if (graphExists(graphRef)) {
                graph = tcManager.getMGraph(graphRef);

                graph.addAll(tempGraph);
                
            } 
        }
        
        return graph;
    }
    
    

    /**
     * Removes all the triples from the graph
     *
     */
    private String emptyGraph(UriRef graphRef) {
        // removes all the triples from the graph
        MGraph graph = tcManager.getMGraph(graphRef);
        graph.clear();
        return "Graph " + graphRef.getUnicodeString() + " is now empty.";
    }

    /**
     * Deletes a graph, the reference to it in the DLC graph and deletes all the
     * derived graphs linked to it by the dcterms:source property.
     *
     * @param graphRef
     * @return
     */
    private String deleteGraph(UriRef graphRef) {
        tcManager.deleteTripleCollection(graphRef);
        GraphNode dlcGraphNode = new GraphNode(DATA_LIFECYCLE_GRAPH_REFERENCE, getDlcGraph());
        //remove the relation with the data lifecycle graph and all the information (triples) about the deleted graph (label).
        dlcGraphNode.deleteProperty(DCTERMS.hasPart, graphRef);

        return "Graph " + graphRef.getUnicodeString() + " has been deleted.";
    }

    /**
     * Reconciles a source graph with a target graph. The result of the reconciliation is an equivalence set 
     * stored in a new graph which is related to the source graph with the dcterms:source property.
     * @param sourceGraphRef the URI of the referenced graph, ie. the graph for which the reconciliation should be performed.
     * @param targetGraphRef teh URI of the target graph. If null the target graph is the same as the source graph.
     * @return
     * @throws Exception 
     */
    private String reconcile(UriRef sourceGraphRef, UriRef targetGraphRef) throws Exception {
        String message = "";
        if (graphExists(sourceGraphRef)) {
            String currentTime = String.valueOf(System.currentTimeMillis());
            
            //if target graph is not provided the reconciliation will be against the source graph itself
            if(targetGraphRef == null){
            	targetGraphRef = sourceGraphRef;
            }
            
            // reconcile the source graph with the target graph 
            UriRef owlSameAsRef =  reconcileCommand(sourceGraphRef, targetGraphRef);
            
            TripleCollection owlSameAs = tcManager.getMGraph(owlSameAsRef);

            if (owlSameAs.size() > 0) {

                message = "A reconciliation task has been done between " + sourceGraphRef.getUnicodeString() + " and " + targetGraphRef.getUnicodeString() + ".\n"
                        + owlSameAs.size() + " owl:sameAs statements have been created and stored in " + owlSameAsRef.getUnicodeString();
            } 
            else {
                message = "A reconciliation task has been done between " + sourceGraphRef.getUnicodeString() + " and " + targetGraphRef.getUnicodeString() + ".\n"
                        + "No equivalent entities have been found.";
            }
            
        } 
        else {
            message = "The source graph does not exist.";
        }
        
        log.info(message);
        return message;

    }
    
    private UriRef reconcileCommand(UriRef sourceGraphRef, UriRef targetGraphRef) throws Exception {
    	
    	TripleCollection owlSameAs = null;
    	UriRef sameAsGraphRef = null;
        
    	if (graphExists(sourceGraphRef)) {
            String currentTime = String.valueOf(System.currentTimeMillis());

            TripleCollection sourceGrah = tcManager.getMGraph(sourceGraphRef);
            
            // reconcile the source graph with the target graph 
            owlSameAs =  interlinker.interlink(sourceGrah, targetGraphRef);

            if (owlSameAs.size() > 0) {

                // create a graph (equivalence set) to store the result of the reconciliation task
                String sameAsGraphName = sourceGraphRef.getUnicodeString() + "-eq-" + currentTime + ".graph";
                sameAsGraphRef = new UriRef(sameAsGraphName);
                LockableMGraph sameAsGraph = tcManager.createMGraph(sameAsGraphRef);
                sameAsGraph.addAll(owlSameAs);

                // log the result (the equivalence set should be serialized and stored)
                Lock l = sameAsGraph.getLock().readLock();
                l.lock();
                try {
	                Iterator<Triple> isameas = owlSameAs.iterator();
	                while (isameas.hasNext()) {
	                    Triple t = isameas.next();
	                    NonLiteral s = t.getSubject();
	                    UriRef p = t.getPredicate();
	                    Resource o = t.getObject();
	                    log.info(s.toString() + p.getUnicodeString() + o.toString() + " .\n");
	                }
                }
                finally {
                	l.unlock();
                }

                // the result of the reconciliation is an equivalent set (not exactly a Linkset)
                getDlcGraph().add(new TripleImpl(sameAsGraphRef, RDF.type, Ontology.voidLinkset));
                // add a reference property of the equivalence set to the source graph in the DLC graph to state from which source it is derived 
                getDlcGraph().add(new TripleImpl(sameAsGraphRef, DCTERMS.source, sourceGraphRef));
                // add a reference of the equivalence set to the target graph if it is different from the source graph
                if(targetGraphRef != sourceGraphRef) {
                	getDlcGraph().add(new TripleImpl(sameAsGraphRef, DCTERMS.source, targetGraphRef));
                }

               
            }         
    	}
            
        return sameAsGraphRef;

    }
    
    /**
     * Performs two operations one (smush) after the other (reconciliation)
     * @param graphRef
     * @param dataUrl
     * @param mediaType
     * @return
     * @throws Exception
     */
    private String reconcileSmush(UriRef graphRef, URL dataUrl, String mediaType) throws Exception {
    	String message = "";
    	
    	return message;    	
    }

    /**
     * Looks for all the equivalence sets that have been created from the graph
     * and smush the graph using the union of the equivalence sets.
     * @param graphToSmushRef
     * @return
     */
    private String smush(UriRef graphToSmushRef) {
        String message = "Smushing task.\n";
        //SimpleMGraph dlcGraphCopy = new SimpleMGraph();
        //dlcGraphCopy.addAll(getDlcGraph()); //this solves a java.util.ConcurrentModificationException
        Set<LockableMGraph> sameAsGraphs = new HashSet<LockableMGraph>();
        LockableMGraph dlcGraph = getDlcGraph();
        Lock l = dlcGraph.getLock().readLock();
        l.lock();
        try {
            Iterator<Triple> ilinksets = dlcGraph.filter(null, DCTERMS.source, graphToSmushRef);      
            while (ilinksets.hasNext()) {            	
            	UriRef equivSetRef = (UriRef) ilinksets.next().getSubject(); 
            	if( dlcGraph.getGraph().contains(new TripleImpl(equivSetRef,RDF.type,Ontology.voidLinkset)) ) {       
            		//UriRef equivalenceSet = (UriRef) ilinksets.next().getSubject();
            		sameAsGraphs.add(tcManager.getMGraph(equivSetRef));
            	}
            }
        } finally {
            l.unlock();
        }
        sameAsGraphs.add(getOwlSameAsGraph());
        if (!sameAsGraphs.isEmpty()) {
            UnionMGraph sameAsUnionGraph = new UnionMGraph(sameAsGraphs.toArray(new MGraph[sameAsGraphs.size()]));
            String smushSetResult = smush(graphToSmushRef, sameAsUnionGraph);
            message += smushSetResult + "\n";
        } else {
            message = "No equivalence set available for " + graphToSmushRef.toString() + "\n"
                    + "Start a reconciliation task before.";
        }

        return message;
    }

    /**
     * Smush a graph using one equivalence set (or linkset)
     *
     */
    private String smush(UriRef graphRef, LockableMGraph eqset) {
        String message = "";
        
        LockableMGraph smushedGraph = smushCommand(graphRef,eqset);

        message = "Smushing of " + graphRef.getUnicodeString()
                + " with linkset completed. "
                + "Smushed graph size = " + smushedGraph.size() + "\n";
        return message;
    }
    
    private LockableMGraph smushCommand(UriRef graphRef, LockableMGraph linkset) {
        
        LockableMGraph graph = tcManager.getMGraph(graphRef);
        SimpleMGraph tempLinkset = new SimpleMGraph();
        tempLinkset.addAll(linkset);
        IriSmusher smusher = new CanonicalizingSameAsSmusher();
        smusher.smush(graph, tempLinkset, true);
        serializer.serialize(System.out, graph, SupportedFormat.RDF_XML);
        
        return graph;

    }
    
    private String moveToDatasetGraph(UriRef sourceGraphRef, UriRef datasetGraphRef) {
    	String message = "To be implemented";
    	
    	return message;
    }
    
    private String extractTextFromPatent(UriRef graphRef){
    	String message = "Extract text from patents and adding a sioc:content property.\n";
    	MGraph graph = tcManager.getMGraph(graphRef);
		    		
    	patentDigester.extractText(graph);
    	message += "Extracted text from " + graphRef.getUnicodeString();
    	
    	return message;
    }
    
    private String extractTextFromPubMed(UriRef graphRef){
    	String message = "Extract text from PubMed articles and adding a sioc:content property.\n";
    	MGraph graph = tcManager.getMGraph(graphRef);
		    		
    	pubmedDigester.extractText(graph);
    	message += "Extracted text from " + graphRef.getUnicodeString();
    	
    	return message;
    }

    /**
     * Extracts the content type from the file extension
     *
     * @param url
     * @return
     */
    private String getContentTypeFromUrl(URL url) {
        String contentType = null;
        if (url.getFile().endsWith("ttl")) {
            contentType = "text/turtle";
        } else if (url.getFile().endsWith("nt")) {
            contentType = "text/turtle";
        } else {
            contentType = "application/rdf+xml";
        }
        return contentType;
    }

    /**
     * Returns the data life cycle graph containing all the monitored graphs. It
     * creates it if doesn't exit yet.
     *
     * @return
     */
    private LockableMGraph getDlcGraph() {
        return tcManager.getMGraph(DATA_LIFECYCLE_GRAPH_REFERENCE);
    }

    /**
     * Checks if a graph exists and returns a boolean value.
     *
     * @param graph_ref
     * @return
     */
    private boolean graphExists(UriRef graph_ref) {
        Set<UriRef> graphs = tcManager.listMGraphs();
        Iterator<UriRef> igraphs = graphs.iterator();
        while (igraphs.hasNext()) {
            UriRef graphRef = igraphs.next();
            if (graph_ref.toString().equals(graphRef.toString())) {
                return true;
            }
        }

        return false;

    }

    /**
     * Creates the data lifecycle register graph. Must be called at the bundle
     * activation if the graph doesn't exists yet.
     */
    private MGraph createDlcGraph() {
        MGraph dlcGraph = tcManager.createMGraph(DATA_LIFECYCLE_GRAPH_REFERENCE);
        TcAccessController tca = new TcAccessController(tcManager);
        tca.setRequiredReadPermissions(DATA_LIFECYCLE_GRAPH_REFERENCE,
                Collections.singleton((Permission) new TcPermission(
                                "urn:x-localinstance:/content.graph", "read")));
        return dlcGraph;
    }

    private UriRef generateNewHttpUri(Set<UriRef> uriRefs) {
        UriRef bestNonHttp = chooseBest(uriRefs);
        String nonHttpString = bestNonHttp.getUnicodeString();
        if (!nonHttpString.startsWith("urn:x-temp:")) {
            throw new RuntimeException("Sorry we current assume all non-http "
                    + "URIs to be canonicalized to be urn:x-temp");
        }
        String httpUriString = nonHttpString.replaceFirst("urn:x-temp:", baseURI);
        UriRef result = new UriRef(httpUriString);
        getOwlSameAsGraph().add(new TripleImpl(bestNonHttp, OWL.sameAs, result));
        return result;
    }

    private UriRef chooseBest(Set<UriRef> httpUri) {
        Iterator<UriRef> iter = httpUri.iterator();
        UriRef best = iter.next();
        while (iter.hasNext()) {
            UriRef next = iter.next();
            if (next.getUnicodeString().compareTo(best.getUnicodeString()) < 0) {
                best = next;
            }
        }
        return best;
    }

    private LockableMGraph getOwlSameAsGraph() {
        return tcManager.getMGraph(OWL_SAME_AS_GRAPH);
    }

    private class CanonicalizingSameAsSmusher extends IriSmusher {

        @Override
        protected UriRef getPreferedIri(Set<UriRef> uriRefs) {
            Set<UriRef> httpUri = new HashSet<UriRef>();
            for (UriRef uriRef : uriRefs) {
                if (uriRef.getUnicodeString().startsWith("http")) {
                    httpUri.add(uriRef);
                }
            }
            if (httpUri.size() == 1) {
                return httpUri.iterator().next();
            }
            if (httpUri.size() == 0) {
                return generateNewHttpUri(uriRefs);
            }
            if (httpUri.size() > 1) {
                return chooseBest(httpUri);
            }
            throw new Error("Negative size set.");
        }

    }

}
