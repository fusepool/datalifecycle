/*.
 * Copyright 2013 Fusepool Project.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.fusepool.datalifecycle;

import eu.fusepool.datalifecycle.utils.FileUtil;
import eu.fusepool.datalifecycle.utils.LinksRetriever;
//import eu.fusepool.rdfizer.marec.Ontology;


import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.security.AccessController;
import java.security.AllPermission;
import java.security.Permission;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.clerezza.jaxrs.utils.RedirectUtil;
import org.apache.clerezza.jaxrs.utils.TrailingSlash;
import org.apache.clerezza.rdf.core.BNode;
import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.NonLiteral;
import org.apache.clerezza.rdf.core.Resource;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.TypedLiteral;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.access.EntityAlreadyExistsException;
import org.apache.clerezza.rdf.core.access.LockableMGraph;
import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.rdf.core.access.NoSuchEntityException;
import org.apache.clerezza.rdf.core.access.security.TcAccessController;
import org.apache.clerezza.rdf.core.access.security.TcPermission;
import org.apache.clerezza.rdf.core.impl.PlainLiteralImpl;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.clerezza.rdf.core.serializedform.Parser;
import org.apache.clerezza.rdf.core.serializedform.Serializer;
import org.apache.clerezza.rdf.ontologies.DC;
import org.apache.clerezza.rdf.ontologies.DCTERMS;
import org.apache.clerezza.rdf.ontologies.OWL;
import org.apache.clerezza.rdf.ontologies.RDF;
import org.apache.clerezza.rdf.ontologies.RDFS;
import org.apache.clerezza.rdf.ontologies.SIOC;
import org.apache.clerezza.rdf.utils.GraphNode;
import org.apache.clerezza.rdf.utils.UnionMGraph;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.indexedgraph.IndexedMGraph;
import org.apache.stanbol.commons.web.viewable.RdfViewable;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.ContentItemFactory;
import org.apache.stanbol.enhancer.servicesapi.ContentSource;
import org.apache.stanbol.enhancer.servicesapi.EnhancementException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementJobManager;
import org.apache.stanbol.enhancer.servicesapi.impl.ByteArraySource;
import org.apache.stanbol.enhancer.servicesapi.rdf.TechnicalClasses;
import org.apache.stanbol.entityhub.model.clerezza.RdfValueFactory;
import org.apache.stanbol.entityhub.servicesapi.model.Entity;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.site.SiteManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.clerezza.rdf.utils.smushing.SameAsSmusher;

/**
 * This is the controller class of the fusepool data life cycle component. The
 * main functionalities provided are 1) XML2RDF transformation 2) Indexing and
 * Information Extraction 3) Reconciliation/Interlinking 4) Smushing
 */
@Component(immediate = true, metatype = true,
        policy = ConfigurationPolicy.OPTIONAL)
@Properties(value = {
    @Property(name = "javax.ws.rs", boolValue = true),
    @Property(name = Constants.SERVICE_RANKING, intValue = SourcingAdmin.DEFAULT_SERVICE_RANKING)
})

@Service(Object.class)
@Path("sourcing")
public class SourcingAdmin {

    // Service property attributes
    public static final int DEFAULT_SERVICE_RANKING = 101;

    // Base URI property attributes. This property is used to canonicalize URIs of type urn:x-temp.
    // The value of the property is updated at service activation from the service configuration panel.
    public static final String BASE_URI_DESCRIPTION = "Base http URI to be used when publishing data ( e.g. http://mydomain.com )";
    public static final String BASE_URI_LABEL = "Base URI";
    public static final String DEFAULT_BASE_URI = "http://localhost:8080";
    @Property(label = BASE_URI_LABEL, value = DEFAULT_BASE_URI, description = BASE_URI_DESCRIPTION)
    public static final String BASE_URI = "baseUri";
    // base uri updated at service activation from the service property in the osgi console
    private String baseUri;
    
    
    // Confidence threshold for enhencements attributes. This property is used to set the minimum value of acceptance of
    // computed enhancements
    public static final String CONFIDENCE_THRESHOLD_DESCRIPTION = "Minimum value for acceptance of computed enhancements";
    public static final String CONFIDENCE_THRESHOLD_LABEL = "Confidence threshold";
    public static final String DEFAULT_CONFIDENCE_VALUE = "0.5";
    @Property(label = CONFIDENCE_THRESHOLD_LABEL, value = DEFAULT_CONFIDENCE_VALUE, description = CONFIDENCE_THRESHOLD_DESCRIPTION)   
    public static final String CONFIDENCE_THRESHOLD = "confidenceThreshold";
    // confidence threshold value updated at service activation from the service property in the osgi console
    private double confidenceThreshold = 0.5;

   

    // Scheme of non-http URI that will be canonicalized in the publishing phase 
    //(i.e. the prefix will be replaced by the base URI)
    public static final String [] URN_SCHEMES = {"urn:x-temp:/","urn:uuid:"};

    /**
     * Using slf4j for normal logging
     */
    private static final Logger log = LoggerFactory.getLogger(SourcingAdmin.class);

    BundleContext bundleCtx = null;

    @Reference
    private Parser parser;

    @Reference
    private Serializer serializer;
    
    @Reference
    private ContentItemFactory contentItemFactory;
    
    @Reference
    private EnhancementJobManager enhancementJobManager;
    
    /**
     * This service allows to get entities from configures sites
     */
    @Reference
    private SiteManager siteManager;

    /**
     * This service allows accessing and creating persistent triple collections
     */
    @Reference
    private TcManager tcManager;

    // Stores bindings to different implementations of RdfDigester
    @Reference(cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE,
            policy = ReferencePolicy.DYNAMIC,
            referenceInterface = eu.fusepool.datalifecycle.RdfDigester.class)
    private final Map<String, RdfDigester> digesters = new HashMap<String, RdfDigester>();

    // Stores bindings to different implementations of Rdfizer
    @Reference(cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE,
            policy = ReferencePolicy.DYNAMIC,
            referenceInterface = eu.fusepool.datalifecycle.Rdfizer.class)
    private final Map<String, Rdfizer> rdfizers = new HashMap<String, Rdfizer>();

    // Stores bindings to different instances of Interlinker
    @Reference(cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE,
            policy = ReferencePolicy.DYNAMIC,
            referenceInterface = eu.fusepool.datalifecycle.Interlinker.class)
    private final Map<String, Interlinker> interlinkers = new HashMap<String, Interlinker>();

    /**
     * This is the name of the graph in which we "log" the requests
     */
    //private UriRef REQUEST_LOG_GRAPH_NAME = new UriRef("http://example.org/resource-resolver-log.graph");
    /**
     * Name of the data life cycle graph. It is used as a register of other
     * graphs to manage their life cycle
     */
    public static final UriRef DATA_LIFECYCLE_GRAPH_REFERENCE = new UriRef("urn:x-localinstance:/dlc/meta.graph");

    /**
     * Register graph referencing graphs for life cycle monitoring;
     */
    public static final String CONTENT_GRAPH_NAME = "urn:x-localinstance:/content.graph";

    private UriRef CONTENT_GRAPH_REF = new UriRef(CONTENT_GRAPH_NAME);

    // data upload codes
    private final int UPLOAD_XML = 1;
    private final int UPLOAD_RDF = 2;
    // tasks codes
    private final int TEXT_EXTRACTION = 1;
    private final int COMPUTE_ENHANCEMENTS = 2;
    private final int RECONCILE_GRAPH_OPERATION = 3;
    private final int SMUSH_GRAPH_OPERATION = 4;
    private final int PUBLISH_DATA = 5;

    // base graph uri
    public static final String GRAPH_URN_PREFIX = "urn:x-localinstance:/dlc/";
    // graph suffix
    public static final String SOURCE_GRAPH_URN_SUFFIX = "/rdf.graph";
    // digest graph suffix
    public static final String DIGEST_GRAPH_URN_SUFFIX = "/digest.graph";
    // enhancements graph suffix
    public static final String ENHANCE_GRAPH_URN_SUFFIX = "/enhance.graph";
    // log graph suffix
    public static final String LOG_GRAPH_URN_SUFFIX = "/log.graph";
    // interlink graph suffix
    public static final String INTERLINK_GRAPH_URN_SUFFIX = "/interlink.graph";
    // smushed graph suffix
    public static final String SMUSH_GRAPH_URN_SUFFIX = "/smush.graph";
    // published graph suffix
    public static final String PUBLISH_GRAPH_URN_SUFFIX = "/publish.graph";
    //mesage to show when base URI is invalid
    private final String INVALID_BASE_URI_ALERT = "A valid base URI has not been set. It can be set in the framework configuration panel (eu.fusepool.datalifecycle.SourcingAdmin)";


    // Validity of base Uri (enables interlinking, smushing and publishing tasks)
    private boolean isValidBaseUri = false;
    
    //all active and some other tasks
    final private Set<Task> tasks = Collections.synchronizedSet(new HashSet<Task>());

    @SuppressWarnings("unchecked")
    @Activate
    protected void activate(ComponentContext context) {
        log.info("The Sourcing Admin Service is being activated");
        // Get the value of the base uri from the service property set in the Felix console
        Dictionary<String, Object> dict = context.getProperties();
        Object baseUriObj = dict.get(BASE_URI);
        baseUri = baseUriObj.toString();
        if ((!"".equals(baseUri)) && (baseUri.startsWith("http://"))) {
            if (baseUri.endsWith("/")) {
                baseUri = baseUri.substring(0, baseUri.length() - 1);
            }
            isValidBaseUri = true;
            log.info("Base URI: {}", baseUri);
        } else {
            isValidBaseUri = false;
        }
        // Get the value of the confidence threshold from the service property set in the Felix console
        Object confidenceObj = dict.get(CONFIDENCE_THRESHOLD);
        if(confidenceObj != null)
            confidenceThreshold = Double.valueOf(confidenceObj.toString());   
        
        // Creates the data lifecycle graph if it doesn't exists. This graph contains references to graphs and linksets
        try {
            createDlcGraph();
            log.info("Created Data Lifecycle Register Graph. This graph will reference all graphs during their lifecycle");
        } catch (EntityAlreadyExistsException ex) {
            log.info("Data Lifecycle Graph already exists.");
        }

    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        log.info("The Sourcing Admin Service is being deactivated");
    }

    /**
     * Bind digesters used by this component. Adds a digester to an hashmap
     *
     * @param digester
     */
    protected void bindDigesters(RdfDigester digester) {

        log.info("Binding digester " + digester.getName());
        if (!digesters.containsKey(digester.getName())) {
            digesters.put(digester.getName(), digester);
            log.info("Digester " + digester.getName() + " bound");

        } else {
            log.info("Digester " + digester.getName() + " already bound.");
        }

    }

    /**
     * Unbind digesters used by this component. Removes a digester from the hash
     * map.
     *
     * @param digester
     */
    protected void unbindDigesters(RdfDigester digester) {
        if (digesters.containsKey(digester.getName())) {
            digesters.remove(digester.getName());
            log.info("Digester " + digester.getName() + " unbound.");
        }
    }

    /**
     * Bind interlinkers used by this component
     */
    protected void bindInterlinkers(Interlinker interlinker) {
        log.info("Binding interlinker " + interlinker.getName());
        if (!interlinkers.containsKey(interlinker.getName())) {
            interlinkers.put(interlinker.getName(), interlinker);
            log.info("Interlinker " + interlinker.getName() + " bound");

        } else {
            log.info("Interlinker " + interlinker.getName() + " already bound.");
        }

    }

    /**
     * Unbind interlinkers
     */
    protected void unbindInterlinkers(Interlinker interlinker) {

        if (interlinkers.containsKey(interlinker.getName())) {
            interlinkers.remove(interlinker.getName());
            log.info("Interlinker " + interlinker.getName() + " unbound.");
        }

    }

    /**
     * Bind rdfizers used by this component
     */
    protected void bindRdfizers(Rdfizer rdfizer) {
        log.info("Binding rdfizer " + rdfizer.getName());
        if (!rdfizers.containsKey(rdfizer.getName())) {
            rdfizers.put(rdfizer.getName(), rdfizer);
            log.info("Rdfizer " + rdfizer.getName() + " bound");

        } else {
            log.info("Rdfizer " + rdfizer.getName() + " already bound.");
        }

    }

    /**
     * Unbind rdfizers
     */
    protected void unbindRdfizers(Rdfizer rdfizer) {

        if (rdfizers.containsKey(rdfizer.getName())) {
            rdfizers.remove(rdfizer.getName());
            log.info("Rdfizer " + rdfizer.getName() + " unbound.");
        }

    }

    /**
     * This method return an RdfViewable, this is an RDF serviceUri with
     * associated presentational information.
     */
    @GET
    public RdfViewable serviceEntry(@Context final UriInfo uriInfo,
            @QueryParam("url") final UriRef url,
            @HeaderParam("user-agent") String userAgent) {
        //this makes sure we are nt invoked with a trailing slash which would affect
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
        final MGraph responseGraph = new IndexedMGraph();
        Lock rl = getDlcGraph().getLock().readLock();
        rl.lock();
        try {
            responseGraph.addAll(getDlcGraph());
        } finally {
            rl.unlock();
        }

        // add available digesters 
        Iterator<String> digestersNames = digesters.keySet().iterator();
        while (digestersNames.hasNext()) {
            String digesterName = digestersNames.next();
            responseGraph.add(new TripleImpl(DATA_LIFECYCLE_GRAPH_REFERENCE, Ontology.enhanceService, new UriRef("urn:x-temp:/" + digesterName)));
            responseGraph.add(new TripleImpl(new UriRef("urn:x-temp:/" + digesterName), RDFS.label, new PlainLiteralImpl(digesterName)));
        }

        // add available rdfizers 
        Iterator<String> rdfizersNames = rdfizers.keySet().iterator();
        while (rdfizersNames.hasNext()) {
            String rdfizerName = rdfizersNames.next();
            responseGraph.add(new TripleImpl(DATA_LIFECYCLE_GRAPH_REFERENCE, Ontology.rdfizeService, new UriRef("urn:x-temp:/" + rdfizerName)));
            responseGraph.add(new TripleImpl(new UriRef("urn:x-temp:/" + rdfizerName), RDFS.label, new PlainLiteralImpl(rdfizerName)));
        }

        // add available interlinkers 
        Iterator<String> interlinkersNames = interlinkers.keySet().iterator();
        while (interlinkersNames.hasNext()) {
            String interlinkerName = interlinkersNames.next();
            NonLiteral interlinkerNode = new BNode();
            responseGraph.add(new TripleImpl(DATA_LIFECYCLE_GRAPH_REFERENCE, Ontology.interlinkService, interlinkerNode));
            responseGraph.add(new TripleImpl(interlinkerNode, RDFS.label, new PlainLiteralImpl(interlinkerName)));
        }

        //This GraphNode represents the service within our result graph
        final GraphNode node = new GraphNode(DATA_LIFECYCLE_GRAPH_REFERENCE, responseGraph);

        // Adds information about base uri configuration
        if (!isValidBaseUri) {
            responseGraph.add(new TripleImpl(DATA_LIFECYCLE_GRAPH_REFERENCE, RDFS.comment, new PlainLiteralImpl(INVALID_BASE_URI_ALERT)));
        }

        // The DLC service uri (set in component config panel) should be the same as the base uri (otherwise there might be a base uri config error)
        String platformPort = (uriInfo.getBaseUri().getPort() > 0 ) ? ":" + String.valueOf(uriInfo.getBaseUri().getPort()) : "";
        String platformBaseUri = uriInfo.getBaseUri().getScheme() + "://" + uriInfo.getBaseUri().getHost() + platformPort;
        if (!platformBaseUri.equals((baseUri))) {
            String message = "The DLC service URI " + platformBaseUri + " is different from the base URI " + baseUri + " set in the component configuration.";
            responseGraph.add(new TripleImpl(DATA_LIFECYCLE_GRAPH_REFERENCE, RDFS.comment, new PlainLiteralImpl(message)));
        }

        for (Task task : tasks) {
            if (task.isActive()) {
                node.addProperty(Ontology.activeTask, task.getUri());
                responseGraph.addAll(task.getNode().getGraph());
            }
        }
        //What we return is the GraphNode we created with a template path
        return new RdfViewable("SourcingAdmin", node, SourcingAdmin.class);
    }

    private LockableMGraph getContentGraph() {
        return tcManager.getMGraph(CONTENT_GRAPH_REF);
    }

    /**
     * Creates a new dataset with tasks and product graphs and adds its uri and a
     * label to the data life cycle graph. A graph will contain the RDF data
     * uploaded or sent by a transformation task that have to be processed (text
     * extraction, NLP processing, reconciliation, smushing). The following
     * graphs are created to store the results of the processing tasks
     * enhance.graph interlink.graph smush.graph publish.graph These graphs will
     * be empty at the beginning.
     *
     * @param uriInfo
     * @param graphName
     * @return
     */
    @POST
    @Path("create_pipe")
    @Produces("text/plain")
    public Response createPipeRequest(@Context final UriInfo uriInfo,
            @FormParam("pipe_label") final String pipeLabel) {

        AccessController.checkPermission(new AllPermission());

        // use dataset label as name after validation
        String datasetName = getValidDatasetName(pipeLabel);

        if (datasetName != null && initializePipe(datasetName)) {
            return RedirectUtil.createSeeOtherResponse("./", uriInfo);
        } else {
            return Response.status(Response.Status.BAD_REQUEST).entity("Cannot create graph " + pipeLabel).build();
        }

    }

    /**
     * Check whether a label can be used as a dataset name. To be a valid name a
     * label must be: 1) not null and at least one character long 2) without
     * white spaces 3) unique (no two dataset can have the same name)
     *
     * @return String
     */
    private String getValidDatasetName(String label) {
        String newDatasetName = null;
        //check validity 
        if (label == null || "".equals(label)) {
            return null;
        }

        // replace white space if present
        newDatasetName = label.replace(' ', '-');

        //check uniqueness of name
        Lock rl = getDlcGraph().getLock().readLock();
        rl.lock();
        try {
            Iterator<Triple> idatasets = getDlcGraph().filter(null, RDF.type, Ontology.Pipe);
            while (idatasets.hasNext()) {
                GraphNode datasetNode = new GraphNode((UriRef) idatasets.next().getSubject(), getDlcGraph());
                String datasetName = datasetNode.getLiterals(RDFS.label).next().getLexicalForm();
                if (newDatasetName.equals(datasetName)) {
                    return null;
                }
            }
        } finally {
            rl.unlock();
        }

        return newDatasetName;
    }

    /**
     * Initialize the dataset creating the graphs for each task in the pipe line
     */
    private boolean initializePipe(String datasetName) {

        boolean result = false;

        try {
            // create a pipe 
            UriRef pipeRef = new UriRef(GRAPH_URN_PREFIX + datasetName);
            getDlcGraph().add(new TripleImpl(pipeRef, RDF.type, Ontology.Pipe));
            if (datasetName != null & !"".equals(datasetName)) {
                getDlcGraph().add(new TripleImpl(pipeRef, RDFS.label, new PlainLiteralImpl(datasetName)));
            }
            getDlcGraph().add(new TripleImpl(DATA_LIFECYCLE_GRAPH_REFERENCE, Ontology.pipe, pipeRef));

            // create tasks
            //rdf task
            UriRef rdfTaskRef = new UriRef(GRAPH_URN_PREFIX + datasetName + "/rdf");
            getDlcGraph().add(new TripleImpl(pipeRef, Ontology.creates, rdfTaskRef));
            getDlcGraph().add(new TripleImpl(rdfTaskRef, RDF.type, Ontology.RdfTask));
            // digest task
            UriRef digestTaskRef = new UriRef(GRAPH_URN_PREFIX + datasetName + "/digest");
            getDlcGraph().add(new TripleImpl(pipeRef, Ontology.creates, digestTaskRef));
            getDlcGraph().add(new TripleImpl(digestTaskRef, RDF.type, Ontology.DigestTask));
            // enhance task
            UriRef enhanceTaskRef = new UriRef(GRAPH_URN_PREFIX + datasetName + "/enhance");
            getDlcGraph().add(new TripleImpl(pipeRef, Ontology.creates, enhanceTaskRef));
            getDlcGraph().add(new TripleImpl(enhanceTaskRef, RDF.type, Ontology.EnhanceTask));
            // interlink task
            UriRef interlinkTaskRef = new UriRef(GRAPH_URN_PREFIX + datasetName + "/interlink");
            getDlcGraph().add(new TripleImpl(pipeRef, Ontology.creates, interlinkTaskRef));
            getDlcGraph().add(new TripleImpl(interlinkTaskRef, RDF.type, Ontology.InterlinkTask));
            // smush task
            UriRef smushTaskRef = new UriRef(GRAPH_URN_PREFIX + datasetName + "/smush");
            getDlcGraph().add(new TripleImpl(pipeRef, Ontology.creates, smushTaskRef));
            getDlcGraph().add(new TripleImpl(smushTaskRef, RDF.type, Ontology.SmushTask));
            // publish task
            UriRef publishTaskRef = new UriRef(GRAPH_URN_PREFIX + datasetName + "/publish");
            getDlcGraph().add(new TripleImpl(pipeRef, Ontology.creates, publishTaskRef));
            getDlcGraph().add(new TripleImpl(smushTaskRef, RDF.type, Ontology.PublishTask));

            // create the source graph for the dataset (result of transformation in RDF)
            String sourceGraphName = GRAPH_URN_PREFIX + datasetName + SOURCE_GRAPH_URN_SUFFIX;
            UriRef sourceGraphRef = new UriRef(sourceGraphName);
            tcManager.createMGraph(sourceGraphRef);
            //GraphNode dlcGraphNode = new GraphNode(DATA_LIFECYCLE_GRAPH_REFERENCE, getDlcGraph());
            //dlcGraphNode.addProperty(DCTERMS.hasPart, graphRef);
            getDlcGraph().add(new TripleImpl(rdfTaskRef, Ontology.deliverable, sourceGraphRef));
            getDlcGraph().add(new TripleImpl(sourceGraphRef, RDF.type, Ontology.voidDataset));
            
            // create the graph to store text fields extract from properties in the source rdf
            String digestGraphName = GRAPH_URN_PREFIX + datasetName + DIGEST_GRAPH_URN_SUFFIX;
            UriRef digestGraphRef = new UriRef(digestGraphName);
            tcManager.createMGraph(digestGraphRef);
            getDlcGraph().add(new TripleImpl(enhanceTaskRef, Ontology.deliverable, digestGraphRef));
            getDlcGraph().add(new TripleImpl(digestGraphRef, RDFS.label, new PlainLiteralImpl("Contains a sioc:content property with text "
                    + "for indexing and references to entities found in the text by NLP enhancement engines")));

            // create the graph to store enhancements found by NLP engines in the digest
            String enhancementsGraphName = GRAPH_URN_PREFIX + datasetName + ENHANCE_GRAPH_URN_SUFFIX;
            UriRef enhancementsGraphRef = new UriRef(enhancementsGraphName);
            tcManager.createMGraph(enhancementsGraphRef);
            getDlcGraph().add(new TripleImpl(enhanceTaskRef, Ontology.deliverable, enhancementsGraphRef));
            getDlcGraph().add(new TripleImpl(enhancementsGraphRef, RDFS.label, new PlainLiteralImpl("Contains  entities found "
                    + "in digest by NLP enhancement engines")));

            // create the graph to store the result of the interlinking task
            String interlinkGraphName = GRAPH_URN_PREFIX + datasetName + INTERLINK_GRAPH_URN_SUFFIX;
            UriRef interlinkGraphRef = new UriRef(interlinkGraphName);
            tcManager.createMGraph(interlinkGraphRef);
            getDlcGraph().add(new TripleImpl(interlinkTaskRef, Ontology.deliverable, interlinkGraphRef));
            getDlcGraph().add(new TripleImpl(interlinkGraphRef, RDF.type, Ontology.voidLinkset));
            getDlcGraph().add(new TripleImpl(interlinkGraphRef, Ontology.voidSubjectsTarget, sourceGraphRef));
            getDlcGraph().add(new TripleImpl(interlinkGraphRef, Ontology.voidLinkPredicate, OWL.sameAs));
            getDlcGraph().add(new TripleImpl(interlinkGraphRef, RDFS.label, new PlainLiteralImpl("Contains equivalence links")));

            // create the graph to store the result of the smushing task
            String smushGraphName = GRAPH_URN_PREFIX + datasetName + SMUSH_GRAPH_URN_SUFFIX;
            UriRef smushGraphRef = new UriRef(smushGraphName);
            tcManager.createMGraph(smushGraphRef);
            getDlcGraph().add(new TripleImpl(smushTaskRef, Ontology.deliverable, smushGraphRef));

            // create the graph to store the result of the publishing task
            String publishGraphName = GRAPH_URN_PREFIX + datasetName + PUBLISH_GRAPH_URN_SUFFIX;
            UriRef publishGraphRef = new UriRef(publishGraphName);
            tcManager.createMGraph(publishGraphRef);
            getDlcGraph().add(new TripleImpl(publishTaskRef, Ontology.deliverable, publishGraphRef));

            // set the initial dataset status as unpublished 
            UriRef statusRef = new UriRef(pipeRef.getUnicodeString() + "/Status");
            getDlcGraph().add(new TripleImpl(pipeRef, Ontology.status, statusRef));
            getDlcGraph().add(new TripleImpl(statusRef, RDF.type, Ontology.Unpublished));
            getDlcGraph().add(new TripleImpl(statusRef, RDFS.label, new PlainLiteralImpl("Unpublished")));


            result = true;

        } catch (UnsupportedOperationException uoe) {
            log.error("Error while creating a graph");
        }

        return result;

    }
    
    /**
     * Applies one of the following operations to a graph: - add triples
     * (operation code: 1) - remove all triples (operation code: 2) - delete
     * graph (operation code: 3) - reconcile (operation code: 4) - smush
     * (operation code: 5)
     */
    @POST
    @Path("dataUpload")
    @Produces("text/plain")
    public Response dataUpload(@Context final UriInfo uriInfo,
            @FormParam("pipe") final UriRef pipeRef,
            @FormParam("operation_code") final int operationCode,
            @FormParam("data_url") final URL dataUrl,
            @FormParam("rdfizer") final String rdfizer) throws IOException {
        AccessController.checkPermission(new AllPermission());

        // validate arguments and handle all the connection exceptions
        StringWriter stringWriter = new StringWriter();
        PrintWriter messageWriter = new PrintWriter(stringWriter);
        if (pipeExists(pipeRef)) {
            DataSet dataSet = new DataSet((pipeRef));
            switch (operationCode) {
                case UPLOAD_RDF:
                    uploadRdf(dataSet, dataUrl, messageWriter);
                    break;
                case UPLOAD_XML:
                    uploadXml(dataSet, dataUrl, rdfizer, messageWriter);
                    break;
            }
        } else {
            messageWriter.println("The dataset does not exist.");
        }
        //return stringWriter.toString();
        return RedirectUtil.createSeeOtherResponse("./", uriInfo);

    }
    /**
     * Uploads RDF files. Each file name must end with .rdf or .ttl,.nt,.n3. An url that does not ends with the mentioned extensions
     * or ends with a slash is supposed to refer to a folder in a local file system or in a remote one (http server). 
     * @param dataSet
     * @param dataUrl
     * @param messageWriter
     * @throws IOException
     */
    private void uploadRdf(DataSet dataSet, URL dataUrl, PrintWriter messageWriter) throws IOException {
        String [] fileNameExtensions = {".rdf", ".ttl",".nt",".n3"};
        // retrieves the list of file to be uploaded
        ArrayList<String> fileList = FileUtil.getFileList(dataUrl,fileNameExtensions);
        Iterator<String> ifile = fileList.iterator();
        while(ifile.hasNext()){
            URL fileUrl = new URL(ifile.next());
            URLConnection connection = fileUrl.openConnection();
            String mediaType = guessContentTypeFromUri(fileUrl);
            InputStream stream = connection.getInputStream();
            if (stream != null) {
                parser.parse(dataSet.getSourceGraph(), stream, mediaType);                
            }         
        }        
    }
    /**
     * Uploads XML files. Each file name must end with .xml or .nxml. An url that does not ends with the mentioned extensions
     * or ends with a slash is supposed to refer to a folder in a local file system or in a remote one (http server). 
     * @param dataSet
     * @param dataUrl
     * @param rdfizerName
     * @param messageWriter
     */
    private void uploadXml(DataSet dataSet, URL dataUrl, String rdfizerName, PrintWriter messageWriter) throws IOException {
        Rdfizer rdfizer = rdfizers.get(rdfizerName);
        String [] fileNameExtensions = {".xml", ".nxml"};
        // retrieves the list of file to be uploaded
        ArrayList<String> fileList = FileUtil.getFileList(dataUrl,fileNameExtensions);
        Iterator<String> ifile = fileList.iterator();
        while(ifile.hasNext()){
            URL fileUrl = new URL(ifile.next());
            URLConnection connection = fileUrl.openConnection();            
            InputStream stream = connection.getInputStream();
            if (stream != null) {
                dataSet.getSourceGraph().addAll(rdfizer.transform(stream));                                
            }         
        }        
    }

    /**
     * Applies one of the following operations to a graph: - add triples
     * (operation code: 1) - remove all triples (operation code: 2) - delete
     * graph (operation code: 3) - reconcile (operation code: 4) - smush
     * (operation code: 5)
     */
    @POST
    @Path("performTask")
    @Produces("text/plain")
    public Response performTaskRequest(@Context final UriInfo uriInfo,
            @FormParam("pipe") final UriRef pipeRef,
            @FormParam("task_code") final int taskCode,
            @FormParam("rdfdigester") final String rdfdigester,
            @FormParam("interlinker") final String interlinker) throws IOException {
        AccessController.checkPermission(new AllPermission());

        // validate arguments and handle all the connection exceptions
        StringWriter stringWriter = new StringWriter();
        PrintWriter messageWriter = new PrintWriter(stringWriter);
        performTask(pipeRef, taskCode, rdfdigester, interlinker, messageWriter);
        //return stringWriter.toString();
        return RedirectUtil.createSeeOtherResponse("./", uriInfo);

    }
    
    @POST
    @Path("processBatch")
    public Response processBatch(@Context final UriInfo uriInfo,
            @FormParam("dataSet") final UriRef dataSetRef,
            @FormParam("url") final URL url,
            @FormParam("rdfizer") final String rdfizerName,
            @FormParam("digester") final String digester,
            @FormParam("interlinker") final String interlinker,
            @FormParam("maxFiles") @DefaultValue("10") final int maxFiles,
            @FormParam("skipPreviouslyAdded") final String skipPreviouslyAddedValue,
            @FormParam("recurse") final String recurseValue,
            @FormParam("smushAndPublish") final String smushAndPublishValue) throws Exception {
        final boolean skipPreviouslyAdded = "on".equals(skipPreviouslyAddedValue);
        final boolean recurse = "on".equals(recurseValue);
        final boolean smushAndPublish = "on".equals(smushAndPublishValue);
        if (dataSetRef == null) {
            throw new WebApplicationException("Param dataSet must be specified", Response.Status.BAD_REQUEST);
        }
        
        AccessController.checkPermission(new DlcPermission());
        final DataSet dataSet = new DataSet(dataSetRef);
        final Rdfizer rdfizer = rdfizerName.equals("none")? null : rdfizers.get(rdfizerName);    
        Task task = new Task(uriInfo) {

            @Override
            public void execute() {
                try {
                    final int[] count = {0};
                    LinksRetriever.processLinks(url, recurse, 
                            new LinksRetriever.LinkProcessor() {
                        public boolean process(URL dataUrl) {

                            if (skipPreviouslyAdded) {
                                Lock lock = dataSet.getLogGraph().getLock().readLock();
                                lock.lock();
                                try {
                                    if (dataSet.getLogGraph().filter(null,
                                            Ontology.retrievedURI,
                                            new UriRef((dataUrl.toString()))).hasNext()) {
                                        return true;
                                    }
                                } finally {
                                    lock.unlock();
                                }
                            }
                            if (isTerminationRequested()) {
                                return false;
                            }
                            if (++count[0] > maxFiles) {
                                return false;
                            }
                            try {
                                rdfUploadPublish(dataSet, dataUrl, rdfizer, digester, interlinker, smushAndPublish, log);
                            } catch (Exception e) {
                                log.println("Exception processing " + dataUrl);
                                e.printStackTrace(log);
                            }
                            return true;
                        }
                    });
                } catch (Exception ex) {
                    ex.printStackTrace(log);
                }
            }
        
        };
        tasks.add(task);
        task.start();
        return Response.seeOther(new URI(task.getUri().getUnicodeString())).build();
    }
    
    
    @POST
    @Path("reprocess")
    public Response reprocess(@Context final UriInfo uriInfo,
            @FormParam("dataSet") final UriRef dataSetRef,
            @FormParam("interlinker") final String interlinkerName) throws Exception {
        if (dataSetRef == null) {
            throw new WebApplicationException("Param dataSet must be specified", Response.Status.BAD_REQUEST);
        }
        
        AccessController.checkPermission(new DlcPermission());
        final DataSet dataSet = new DataSet(dataSetRef); 
        final Interlinker interlinker = interlinkerName.equals("none")? null : interlinkers.get(interlinkerName);
        Task task = new Task(uriInfo) {

            @Override
            public void execute() {
                try {        
                    if (interlinker != null) {
                        log.println("Interlinking with: "+interlinker);
                        final TripleCollection dataSetInterlinks = interlinker.interlink(dataSet.getEnhancedGraph(), dataSet.getEnhancedGraph());
                        dataSet.getInterlinksGraph().addAll(dataSetInterlinks);
                        log.println("Added " + dataSetInterlinks.size() + " data-set interlinks to " + dataSet.getInterlinksGraphRef().getUnicodeString());
                    } else {
                        log.println("No interlinker selected, proceding.");
                    }
                    // Smush
                    smush(dataSet, log);
                    // Publish
                    publishData(dataSet, log);
                } catch (Exception ex) {
                    ex.printStackTrace(log);
                }
            }
        
        };
        tasks.add(task);
        task.start();
        return Response.seeOther(new URI(task.getUri().getUnicodeString())).build();
    }
    
    @GET
    @Path("task/{id}")
    public RdfViewable describeTask(@Context final UriInfo uriInfo) {
        final String resourcePath = uriInfo.getAbsolutePath().toString();
        final UriRef taskUri = new UriRef(resourcePath);
        for (Task task : tasks) {
            if (task.getUri().equals(taskUri)) {
                return new RdfViewable("task", task.getNode(), SourcingAdmin.class);
            }
        }
        throw new WebApplicationException(Response.Status.NOT_FOUND);
    }
    
    @POST
    @Path("task/{id}")
    public Response actOnTaks(@Context final UriInfo uriInfo, @FormParam("action") String action) throws URISyntaxException {
        final String resourcePath = uriInfo.getAbsolutePath().toString();
        final UriRef taskUri = new UriRef(resourcePath);
        for (Task task : tasks) {
            if (task.getUri().equals(taskUri)) {
                if ("TERMINATE".equalsIgnoreCase(action)) {
                    task.requestTermination();
                    return Response.seeOther(new URI(task.getUri().getUnicodeString())).build();
                }
                throw new WebApplicationException(Response.Status.BAD_REQUEST);
            }
        }
        throw new WebApplicationException(Response.Status.NOT_FOUND);
    }

    /**
     * Performs a task on a dataset: digest, interlink, smush, publish.
     * @param pipeRef
     * @param taskCode
     * @param rdfdigester
     * @param interlinker
     * @param messageWriter
     * @throws IOException
     */
    private void performTask(UriRef pipeRef,
            int taskCode,
            String rdfdigester,
            String interlinker,
            PrintWriter messageWriter) throws IOException {
        AccessController.checkPermission(new AllPermission());
        if (pipeExists(pipeRef)) {
            DataSet dataSet = new DataSet((pipeRef));

            switch (taskCode) {
                case TEXT_EXTRACTION:
                    extractTextFromRdf(dataSet, rdfdigester, messageWriter);
                    break;
                case COMPUTE_ENHANCEMENTS:
                    computeEnhancements(dataSet, messageWriter);
                    break;
                case RECONCILE_GRAPH_OPERATION:
                    reconcile(dataSet, interlinker, messageWriter);
                    break;
                case SMUSH_GRAPH_OPERATION:
                    smush(dataSet, messageWriter);
                    break;     
                case PUBLISH_DATA:
                    publishData(dataSet, messageWriter);
                    break;
            }
        } else {
            messageWriter.println("The pipe does not exist.");
        }

    }

    @POST
    @Path("runsequence")
    @Produces("text/plain")
    public String runSequence(@Context final UriInfo uriInfo,
            @FormParam("pipe") final UriRef pipeRef,
            @FormParam("sequence_code") final int sequenceCode,
            @FormParam("digester") final String digester,
            @FormParam("interlinker") final String interlinker) throws IOException {

        AccessController.checkPermission(new AllPermission());

        StringWriter stringWriter = new StringWriter();
        PrintWriter messageWriter = new PrintWriter(stringWriter);
        messageWriter.println("Pipe: " + pipeRef.getUnicodeString() + 
                " Digester: " + digester + " Interlinker: " + interlinker);

        if (pipeExists(pipeRef)) {
            DataSet dataSet = new DataSet(pipeRef);
            performAllTasks(dataSet, digester, interlinker, messageWriter);

        } else {
            messageWriter.println("The dataset does not exist.");
        }

        return stringWriter.toString();

    }

    /**
     * Uploads and transforms Patent or PubMed XML data into RDF.
     *
     * @param dataUrl
     * @param rdfizer
     * @return
     */
    private MGraph transformXml(DataSet dataSet, URL dataUrl, Rdfizer rdfizer, PrintWriter messageWriter) throws IOException {
        AccessController.checkPermission(new AllPermission());

        // create a graph to store the data after the document transformation        
        MGraph documentGraph = null;

        InputStream xmldata = null;

        if (isValidUrl(dataUrl)) {

            try {
                URLConnection connection = dataUrl.openConnection();
                connection.addRequestProperty("Accept", "application/xml; q=1");
                xmldata = connection.getInputStream();
            } catch (FileNotFoundException ex) {
                messageWriter.println("The file " + dataUrl.toString() + " has not been found.");
                throw ex;
            }
        } else {
            messageWriter.println("The URL " + dataUrl.toString() + " is not a valid one.\n");
        }

        int numberOfTriples = 0;

        if (xmldata != null) {
            documentGraph = rdfizer.transform(xmldata);
            numberOfTriples = documentGraph.size();

        }

        if (documentGraph != null && numberOfTriples > 0) {
            // add the triples of the document graph to the source graph of the selected dataset
            Lock wl = dataSet.getSourceGraph().getLock().writeLock();
            wl.lock();
            try {
                dataSet.getSourceGraph().addAll(documentGraph);
            } finally {
                wl.unlock();
            }
            messageWriter.println("Added " + numberOfTriples + " triples from "+dataUrl+ " to " + dataSet.getSourceGraphRef().getUnicodeString());
        }
        return documentGraph;

    }

    /**
     * Load RDF data into an existing graph from a URL (schemes: "file://" or
     * "http://"). The arguments to be passed are: 1) graph in which the RDF
     * data must be stored 2) url of the dataset After the upload the input
     * graph is sent to a digester to extract text for indexing and adding
     * entities found by NLP components (in the default chain) as subject
     *
     * @return the added triples
     */
    private MGraph addTriples(DataSet dataSet, URL dataUrl, PrintWriter messageWriter) throws IOException {
        AccessController.checkPermission(new AllPermission());


        // add the triples of the temporary graph into the graph selected by the user
        if (isValidUrl(dataUrl)) {

            MGraph updatedGraph = addTriplesCommand(dataSet.getSourceGraphRef(), dataUrl);

            messageWriter.println("Added " + updatedGraph.size() + " triples from " + dataUrl + " to " + dataSet.getSourceGraphRef().getUnicodeString());
            return updatedGraph;

        } else {
            messageWriter.println("The URL of the data is not a valid one.");
            throw new RuntimeException("Invalid URL; " + dataUrl);
        }

    }

    /**
     *
     * Add triples to graph
     */
    private MGraph addTriplesCommand(UriRef graphRef, URL dataUrl) throws IOException {
        AccessController.checkPermission(new AllPermission());

        URLConnection connection = dataUrl.openConnection();
        connection.addRequestProperty("Accept", "application/rdf+xml; q=.9, text/turte;q=1");

        // create a temporary graph to store the data        
        SimpleMGraph tempGraph = new SimpleMGraph();
        String mediaType = connection.getHeaderField("Content-type");
        if ((mediaType == null) || mediaType.equals("application/octet-stream")) {
            mediaType = guessContentTypeFromUri(dataUrl);
        }
        InputStream data = connection.getInputStream();
        if (data != null) {
            parser.parse(tempGraph, data, mediaType);
            // add the triples of the temporary graph into the graph selected by the user
            if (graphExists(graphRef)) {
                MGraph graph = tcManager.getMGraph(graphRef);

                graph.addAll(tempGraph);

            }
        }

        return tempGraph;
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
     * Reconciles a source graph against itself and against the content graph.
     * The result of the reconciliation is an equivalence set stored in the
     * interlink graph of the pipe.
     *
     * @param sourceGraphRef the URI of the referenced graph, i.e. the graph for
     * which the reconciliation should be performed.
     * @return String
     */
    private void reconcile(DataSet dataSet, String selectedInterlinker, PrintWriter messageWriter) {

        if (dataSet.getSourceGraph().size() > 0) {

            // size of interlink graph before reconciliations
            int interlinkGraphInitSize = dataSet.getInterlinksGraph().size();

            // reconcile the source graph against itself 
            reconcileCommand(dataSet, dataSet.getSourceGraphRef(), dataSet.getSourceGraphRef(), selectedInterlinker);

            // size of interlink graph after reconciliation of source graph against itself 
            int interlinkSourceGraphSize = dataSet.getInterlinksGraph().size();

            // new interlinks within source graph
            int numSourceInterlinks = interlinkSourceGraphSize - interlinkGraphInitSize;

            if (numSourceInterlinks > 0) {

                messageWriter.println("A reconciliation task has been done on " + dataSet.getSourceGraphRef().getUnicodeString() + "\n"
                        + numSourceInterlinks + " owl:sameAs statements have been created.");
            } else {
                messageWriter.println("A reconciliation task has been done on " + dataSet.getSourceGraphRef().getUnicodeString()
                        + ". No equivalent entities have been found.");
            }

            // reconcile the source graph against the content graph 
            if (getContentGraph().size() > 0) {

                reconcileCommand(dataSet, dataSet.getSourceGraphRef(), CONTENT_GRAPH_REF, selectedInterlinker);

                // size of interlink graph after reconciliation of source graph against content graph 
                int interlinkContentGraphSize = dataSet.getInterlinksGraph().size();

                // new interlinks with content graph
                int numContentInterlinks = interlinkContentGraphSize - interlinkSourceGraphSize;

                if (numContentInterlinks > 0) {

                    messageWriter.println("A reconciliation task has been done between " + dataSet.getSourceGraphRef().getUnicodeString() + " and " + CONTENT_GRAPH_NAME + "\n"
                            + numContentInterlinks + " owl:sameAs statements have been created.");
                } else {
                    messageWriter.println("A reconciliation task has been done between " + dataSet.getSourceGraphRef().getUnicodeString() + " and " + CONTENT_GRAPH_NAME + "\n"
                            + ". No equivalent entities have been found.");
                }
            }

        } else {
            messageWriter.println("The source graph does not exist or is empty.");
        }

    }

    /**
     * Reconciles a source graph with a target graph. The result of the
     * reconciliation is an equivalence set stored in the interlink graph of the
     * pipe. The graph used as source is the source rdf graph.
     */
    private void reconcileCommand(DataSet dataSet, UriRef sourceGraphRef, UriRef targetGraphRef, String selectedInterlinker) {


        if (graphExists(sourceGraphRef)) {

            // Get the source graph from the triple store
            LockableMGraph sourceGraph = dataSet.getSourceGraph();
            // reconcile the source graph with the target graph 
            Interlinker interlinker = interlinkers.get(selectedInterlinker);
            TripleCollection owlSameAs = interlinker.interlink(sourceGraph, targetGraphRef);

            if (owlSameAs.size() > 0) {

                LockableMGraph sameAsGraph = dataSet.getInterlinksGraph();
                sameAsGraph.addAll(owlSameAs);
                // add a reference of the equivalence set to the source graph 
                getDlcGraph().add(new TripleImpl(dataSet.getInterlinksGraphRef(), Ontology.voidSubjectsTarget, sourceGraphRef));
                // add a reference of the equivalence set to the target graph                
                getDlcGraph().add(new TripleImpl(dataSet.getInterlinksGraphRef(), Ontology.voidObjectsTarget, targetGraphRef));

            }
        }

    }

    /**
     * Smush the union of the source, digest and enhancements graphs 
     * using the interlinking graph. More precisely collates URIs coming from different 
     * equivalent resources in a single one chosen among them. All the triples in the union graph are copied in the
     * smush graph that is then smushed using the interlinking graph. URIs are canonicalized to http://
     *
     * @param graphToSmushRef
     * @return
     */
    private void smush(DataSet dataSet, PrintWriter messageWriter) {
        messageWriter.println("Smushing task.");
        
        if (dataSet.getSourceGraph().size() > 0) {

            LockableMGraph smushedGraph = smushCommand(dataSet);

            messageWriter.println("Smushing of " + dataSet.getEnhancedGraphRef().getUnicodeString()                    
                    + "Smushed graph size = " + smushedGraph.size());
        } else {
            messageWriter.println("The source graph " + dataSet.getSourceGraphRef().getUnicodeString() + " is empty.");
        }

    }

    private LockableMGraph smushCommand(final DataSet dataSet) {

        final SameAsSmusher smusher = new SameAsSmusher() {

            @Override
            protected UriRef getPreferedIri
            (Set<UriRef> uriRefs
            
                ) {
            Set<UriRef> httpUri = new HashSet<UriRef>();
                for (UriRef uriRef : uriRefs) {
                    if (uriRef.getUnicodeString().startsWith("http")) {
                        httpUri.add(uriRef);
                    }
                }
                if (httpUri.size() == 1) {
                    return httpUri.iterator().next();
                }
                // There is no http URI in the set of equivalent resource. The entity was unknown. 
                // A new representation of the entity with canonical http URI will be created. 
                if (httpUri.size() == 0) {
                    return generateNewHttpUri(dataSet, uriRefs);
                }
                if (httpUri.size() > 1) {
                    return chooseBest(httpUri);
                }
                throw new Error("Negative size set.");
            }

        };

        if (dataSet.getSmushGraph().size() > 0) {
            dataSet.getSmushGraph().clear();
        }

        LockableMGraph unionGraph = new UnionMGraph(dataSet.getSourceGraph(),dataSet.getDigestGraph(), dataSet.getEnhancedGraph());     
        Lock erl = unionGraph.getLock().readLock();
        erl.lock();
        try {
            // add triples from enhance graph to smush graph
            dataSet.getSmushGraph().addAll(unionGraph);
            log.info("Copied " + unionGraph.size() + " triples from the union of source, digest and enhancements graph into the smush graph.");
            MGraph tempEquivalenceSet = new IndexedMGraph();
            tempEquivalenceSet.addAll(dataSet.getInterlinksGraph());
            log.info("Smush task started.");
            smusher.smush(dataSet.getSmushGraph(), tempEquivalenceSet, true);
            log.info("Smush task completed.");
        } finally {
            erl.unlock();
        }

        // Remove from smush graph equivalences between temporary uri (urn:x-temp) and http uri that are added by the clerezza smusher.
        // These equivalences must be removed as only equivalences between known entities (http uri) must be maintained and then published
        MGraph equivToRemove = new SimpleMGraph();
        Lock srl = dataSet.getSmushGraph().getLock().readLock();
        srl.lock();
        try {
            Iterator<Triple> isameas = dataSet.getSmushGraph().filter(null, OWL.sameAs, null);
            while (isameas.hasNext()) {
                Triple sameas = isameas.next();
                NonLiteral subject = sameas.getSubject();
                Resource object = sameas.getObject();
                for(int i = 0; i < URN_SCHEMES.length; i++) {
                    if (subject.toString().startsWith("<" + URN_SCHEMES[i]) || object.toString().startsWith("<" + URN_SCHEMES[i])) {
                        equivToRemove.add(sameas);
                    }
                }
            }
        } finally {
            srl.unlock();
        }

        dataSet.getSmushGraph().removeAll(equivToRemove);

        return dataSet.getSmushGraph();

    }

    /**
     * Extract text from dcterms:title and dcterms:abstract fields in the source
     * graph and adds a sioc:content property with that text in the enhance
     * graph. The text is used by the ECS for indexing. The keywords will be
     * related to the resource so that it could be retrieved. 
     *
     * @return
     */
    private void extractTextFromRdf(DataSet dataSet, String selectedDigester, PrintWriter messageWriter) {
        RdfDigester digester = digesters.get(selectedDigester);
        MGraph tempGraph = new IndexedMGraph();
        LockableMGraph sourceGraph = dataSet.getSourceGraph();
        Lock rl = sourceGraph.getLock().readLock();
        rl.lock();
        try {
            tempGraph.addAll(sourceGraph);
        } finally {
            rl.unlock();
        }
        
        digester.extractText(tempGraph);
        tempGraph.removeAll(sourceGraph);
        dataSet.getDigestGraph().addAll(tempGraph);
        messageWriter.println("Extracted text from " + dataSet.getDigestGraphRef().getUnicodeString() + " by " + selectedDigester + " digester");

    }
    /**
     * Sends the digested content to the default chain to compute enhancements them stores them
     * in the dataset's enhancements graph
     * @param dataSet
     * @param messageWriter
     */
    private void computeEnhancements(DataSet dataSet, PrintWriter messageWriter){
        LockableMGraph digestGraph = dataSet.getDigestGraph();
        if(digestGraph.size() > 0) {
          Lock digestLock = digestGraph.getLock().readLock();
          digestLock.lock();
          try {
            Iterator<Triple> isiocStmt = digestGraph.filter(null, SIOC.content, null);
            while(isiocStmt.hasNext()){
                Triple stmt = isiocStmt.next();             
                UriRef itemRef = (UriRef) stmt.getSubject();
                String content = ((PlainLiteralImpl) stmt.getObject()).getLexicalForm();
                if(! "".equals(content) && content != null ) {
                    try {
                        enhance(dataSet, content, itemRef);
                    } 
                    catch (IOException e) {
                        throw new RuntimeException();                        
                    } 
                    catch (EnhancementException e) {                        
                        e.printStackTrace();
                    }
                }
            }
          }
          finally {
            digestLock.unlock();
          }
        }
        
        
        
        
    }
    /**
     * Add dc:subject properties to an item pointing to entities which are assumed to be related to
     * content item. This method uses the enhancementJobManager to extract related entities using NLP 
     * engines available in the default chain. The node uri is also the uri of the content item
     * so that the enhancements will be referred that node. Each enhancement found with a confidence 
     * value above a threshold is then added as a dc:subject to the node
     */
    private void enhance(DataSet dataSet, String content, UriRef itemRef) throws IOException, EnhancementException {
        final ContentSource contentSource = new ByteArraySource(
                content.getBytes(), "text/plain");
        final ContentItem contentItem = contentItemFactory.createContentItem(itemRef, contentSource);
        enhancementJobManager.enhanceContent(contentItem);
        // this contains the enhancement results
        final MGraph contentMetadata = contentItem.getMetadata();
        LockableMGraph enhancedGraph = dataSet.getEnhancedGraph();
        Lock enhanceLock = enhancedGraph.getLock().readLock();
        enhanceLock.lock();
        try {
            addSubjects(enhancedGraph, itemRef, contentMetadata);
        }
        finally {
            enhanceLock.unlock();
        }
        
    }
    /** 
     * Add dc:subject property to items pointing to entities extracted by NLP engines in the default chain. 
     * Given a node and a TripleCollection containing fise:Enhancements about that node 
     * dc:subject properties are added to an item pointing to entities referenced by those enhancements 
     * if the enhancement confidence value is above a threshold.
     * @param node
     * @param metadata
     */
    private void addSubjects(LockableMGraph enhancedGraph, UriRef itemRef, TripleCollection metadata) {
        final GraphNode enhancementType = new GraphNode(TechnicalClasses.ENHANCER_ENHANCEMENT, metadata);
        final Set<UriRef> entities = new HashSet<UriRef>();
        // get all the enhancements
        final Iterator<GraphNode> enhancements = enhancementType.getSubjectNodes(RDF.type);
        while (enhancements.hasNext()) {
            final GraphNode enhhancement = enhancements.next();
          //look the confidence value for each enhancement
            double enhancementConfidence = LiteralFactory.getInstance().createObject(Double.class,
                    (TypedLiteral) enhhancement.getLiterals(org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_CONFIDENCE).next());
            if( enhancementConfidence >= confidenceThreshold ) {            
                // get entities referenced in the enhancement 
                final Iterator<Resource> referencedEntities = enhhancement.getObjects(org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_ENTITY_REFERENCE);
                while (referencedEntities.hasNext()) {
                    final UriRef entity = (UriRef) referencedEntities.next();                   
                    // Add dc:subject to the patent for each referenced entity
                    enhancedGraph.add(new TripleImpl(itemRef, DC.subject, entity));
                    entities.add(entity);
                }
            }


        }
        for (UriRef uriRef : entities) {
            // We don't get the entity description directly from metadata
            // as the context there would include
            addResourceDescription(uriRef, enhancedGraph);
        }
    }
    /** 
     * Add a description of the entities extracted from the text by NLP engines in the default chain
     */
    private void addResourceDescription(UriRef iri, LockableMGraph mGraph) {
        final Entity entity = siteManager.getEntity(iri.getUnicodeString());
        if (entity != null) {
            final RdfValueFactory valueFactory = new RdfValueFactory(mGraph);
            final Representation representation = entity.getRepresentation();
            if (representation != null) {
                valueFactory.toRdfRepresentation(representation);
            }
        }
    }

    /**
     * Moves data from smush.grah to content.graph. The triples (facts) in the
     * two graphs must be coherent, i.e. the same. Before publishing the current
     * smushed data must be compared with the last published data. New triples
     * in the smushed graph not in the published graph must be added while
     * triples in the published graph not in the smushed graph must be
     * removed. The algorithm is as follows 1) make all URIs in smush.graph http
     * dereferencable (uri canonicalization) 2) find triples in smush.graph not
     * in publish.graph (new triples) 3) find triples in publish.graph not in
     * smush.graph (old triples) 4) add new triples to content.graph 5) remove
     * old triples from content.graph 6) delete all triples in publish.graph 7)
     * copy triples from smush.graph to publish.graph. URIs that are of the urn: type 
     * will be canonicalized replacing the urn: prefix with the base URI.
     */
    private void publishData(DataSet dataSet, PrintWriter messageWriter) {

        // add these triples to the content.graph 
        MGraph triplesToAdd = new SimpleMGraph();
        // remove these triples from the content.graph
        MGraph triplesToRemove = new SimpleMGraph();

        // make all URIs in smush graph canonical (http dereferencable URIs)
        canonicalizeResources(dataSet, dataSet.getSmushGraph());

        // triples to add to the content.graph
        Lock ls = dataSet.getSmushGraph().getLock().readLock();
        ls.lock();
        try {

            Iterator<Triple> ismush = dataSet.getSmushGraph().iterator();
            while (ismush.hasNext()) {
                Triple smushTriple = ismush.next();
                if (!dataSet.getPublishGraph().contains(smushTriple)) {
                    triplesToAdd.add(smushTriple);
                }

            }
        } finally {
            ls.unlock();
        }

        // triples to remove from the content.graph
        Lock lp = dataSet.getPublishGraph().getLock().readLock();
        lp.lock();
        try {
            Iterator<Triple> ipublish = dataSet.getPublishGraph().iterator();
            while (ipublish.hasNext()) {
                Triple publishTriple = ipublish.next();
                if (!dataSet.getSmushGraph().contains(publishTriple)) {
                    triplesToRemove.add(publishTriple);
                }

            }
        } finally {
            lp.unlock();
        }

        if (triplesToRemove.size() > 0) {
            getContentGraph().removeAll(triplesToRemove);
            log.info("Removed " + triplesToRemove.size() + " triples from " + CONTENT_GRAPH_REF.getUnicodeString());
        } else {
            log.info("No triples to remove from " + CONTENT_GRAPH_REF.getUnicodeString());
        }
        if (triplesToAdd.size() > 0) {
            getContentGraph().addAll(triplesToAdd);
            log.info("Added " + triplesToAdd.size() + " triples to " + CONTENT_GRAPH_REF.getUnicodeString());
        } else {
            log.info("No triples to add to " + CONTENT_GRAPH_REF.getUnicodeString());
        }

        dataSet.getPublishGraph().clear();

        Lock rl = dataSet.getSmushGraph().getLock().readLock();
        rl.lock();
        try {
            dataSet.getPublishGraph().addAll(dataSet.getSmushGraph());
        } finally {
            rl.unlock();
        }
        
        // update the dataset status to published in the dlc meta graph
        updateDatasetStatus(dataSet);

        messageWriter.println("Copied " + triplesToAdd.size() + " triples from " + dataSet.getUri() + " to content-graph");

    }
    
    /**
     * Updates the dataset status to published in the dlc meta graph
     * @param datasetName
     */
    private void updateDatasetStatus(DataSet dataSet) {
        UriRef statusRef = new UriRef(dataSet.getUri().getUnicodeString() + "/Status");
        getDlcGraph().remove(new TripleImpl(statusRef, RDF.type, Ontology.Unpublished));
        getDlcGraph().remove(new TripleImpl(statusRef, RDFS.label, new PlainLiteralImpl("Unpublished")));
        getDlcGraph().add(new TripleImpl(statusRef, RDF.type, Ontology.Published));
        getDlcGraph().add(new TripleImpl(statusRef, RDFS.label, new PlainLiteralImpl("Published")));
    }
    
    /**
     * Performs the following tasks in sequence - Enhance -
     * Interlink - Smush - Publish
     *
     * @param pipeRef
     * @param digester
     * @param interlinker
     * @param mediaType
     * @return
     */
    private void performAllTasks(DataSet dataSet, String digesterName, String interlinkerName, PrintWriter messageWriter) throws IOException {
        // Digest RDF data
        extractTextFromRdf(dataSet, digesterName, messageWriter);
        //compute enhacements
        computeEnhancements(dataSet, messageWriter);
        // Interlink (against itself and content.graph)
        reconcile(dataSet, interlinkerName, messageWriter);
        // Smush
        smush(dataSet, messageWriter);
        // Publish
        publishData(dataSet, messageWriter);

    }

    /**
     * Performs the following tasks in sequence - RDF data upload - Enhance -
     * Interlink - Smush - Publish
     *
     * @param pipeRef
     * @param dataUrl
     * @param digester
     * @param interlinker
     * @param mediaType
     * @return
     */
    private void rdfUploadPublish(DataSet dataSet, URL dataUrl, Rdfizer rdfizer, String digesterName, String interlinkerName, boolean smushAndPublish, PrintWriter messageWriter) throws IOException {

        // Transform to RDF
        TripleCollection addedTriples = rdfizer == null ? 
            addTriples(dataSet, dataUrl, messageWriter)
            : transformXml(dataSet, dataUrl, rdfizer, messageWriter);

        // Digest. Add sioc:content and dc:subject predicates
        MGraph digestedTriples = new IndexedMGraph();
        digestedTriples.addAll(addedTriples);
        RdfDigester digester = digesters.get(digesterName);
        digester.extractText(digestedTriples);
        dataSet.getDigestGraph().addAll(digestedTriples);
        messageWriter.println("Added " + digestedTriples.size() + " digested triples to " + dataSet.getDigestGraphRef().getUnicodeString());
        MGraph enhancedTriples = new IndexedMGraph();
        computeEnhancements(dataSet, messageWriter);
        dataSet.getEnhancedGraph().addAll(enhancedTriples);
        messageWriter.println("Added " + enhancedTriples.size() + " enahnced triples to " + dataSet.getEnhancedGraphRef().getUnicodeString());
        // Interlink (self)
        if (!interlinkerName.equals("none")) {
            Interlinker interlinker = interlinkers.get(interlinkerName);
            final TripleCollection dataSetInterlinks = interlinker.interlink(enhancedTriples, dataSet.getEnhancedGraphRef());
            dataSet.getInterlinksGraph().addAll(dataSetInterlinks);
            messageWriter.println("Added " + dataSetInterlinks.size() + " data-set interlinks to " + dataSet.getInterlinksGraphRef().getUnicodeString());
            // Interlink (content.graph)
            final TripleCollection contentGraphInterlinks = interlinker.interlink(enhancedTriples, CONTENT_GRAPH_REF);
            dataSet.getInterlinksGraph().addAll(contentGraphInterlinks);
            messageWriter.println("Added " + contentGraphInterlinks.size() + " content-graph interlinks to " + dataSet.getInterlinksGraphRef().getUnicodeString());
        }
        if (smushAndPublish) {
            // Smush
            smush(dataSet, messageWriter);
            // Publish
            publishData(dataSet, messageWriter);
        }
        
        
        GraphNode logEntry = new GraphNode(new BNode(), dataSet.getLogGraph());
        logEntry.addProperty(RDF.type, Ontology.LogEntry);
        logEntry.addProperty(Ontology.retrievedURI, new UriRef(dataUrl.toString()));


    }

    /**
     * All the resources in the smush graph must be http dereferencable when
     * published. All the triples in the smush graph are copied into a temporary
     * graph. For each triple the subject and the object that have a non-http
     * URI are changed in http uri and an equivalence link is added in the
     * interlinking graph for each resource (subject and object) that has been
     * changed.
     */
    private void canonicalizeResources(DataSet dataSet, LockableMGraph graph) {

        MGraph graphCopy = new SimpleMGraph();
        // graph containing the same triple with the http URI for each subject and object
        MGraph canonicGraph = new SimpleMGraph();
        Lock rl = graph.getLock().readLock();
        rl.lock();
        try {
            graphCopy.addAll(graph);
        } finally {
            rl.unlock();
        }

        Iterator<Triple> ismushTriples = graphCopy.iterator();
        while (ismushTriples.hasNext()) {
            Triple triple = ismushTriples.next();
            UriRef subject = (UriRef) triple.getSubject();
            Resource object = triple.getObject();
            // generate an http URI for both subject and object and add an equivalence link into the interlinking graph
            for(int i = 0; i < URN_SCHEMES.length; i++){
              if (subject.getUnicodeString().startsWith(URN_SCHEMES[i])) {
                subject = generateNewHttpUri(dataSet, Collections.singleton(subject));
              }
              if (object.toString().startsWith("<" + URN_SCHEMES[i])) {
                object = generateNewHttpUri(dataSet, Collections.singleton((UriRef) object));
              }
            }

            // add the triple with the http uris to the canonic graph
            canonicGraph.add(new TripleImpl(subject, triple.getPredicate(), object));
        }

        Lock wl = graph.getLock().writeLock();
        wl.lock();
        try {
            graph.clear();
            graph.addAll(canonicGraph);
        } finally {
            wl.unlock();
        }

    }

    /**
     * Validate URL A valid URL must start with file:/// or http://
     */
    private boolean isValidUrl(URL url) {
        boolean isValidUrl = false;
        if (url != null) {
            if (url.toString().startsWith("http://") || url.toString().startsWith("file:/")) {
                isValidUrl = true;
            }
        }

        return isValidUrl;
    }

    /**
     * Extracts the content type from the file extension
     *
     * @param url
     * @return
     */
    private String guessContentTypeFromUri(URL url) {
        String contentType = null;
        if (url.getFile().endsWith("ttl")) {
            contentType = "text/turtle";
        } else if (url.getFile().endsWith("nt")) {
            contentType = "text/rdf+nt";
        } else if (url.getFile().endsWith("n3")) {
            contentType = "text/rdf+n3";
        } else if (url.getFile().endsWith("rdf")) {
            contentType = "application/rdf+xml";
        } else if (url.getFile().endsWith("xml")) {
            contentType = "application/xml";
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
     * Checks if a graph exists and returns a boolean value. true if graph exist
     * false if graph does not exist
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
     * Checks whether a pipe exists
     */
    private boolean pipeExists(UriRef pipeRef) {
        boolean result = false;

        if (pipeRef != null) {
            GraphNode pipeNode = new GraphNode(pipeRef, getDlcGraph());
            if (pipeNode != null) {
                result = true;
            }
        }

        return result;

    }

    /**
     * Creates the data lifecycle graph. Must be called at the bundle activation
     * if the graph doesn't exists yet.
     */
    private MGraph createDlcGraph() {
        MGraph dlcGraph = tcManager.createMGraph(DATA_LIFECYCLE_GRAPH_REFERENCE);
        TcAccessController tca = tcManager.getTcAccessController();
        tca.setRequiredReadPermissions(DATA_LIFECYCLE_GRAPH_REFERENCE,
                Collections.singleton((Permission) new TcPermission(
                                "urn:x-localinstance:/content.graph", "read")));
        return dlcGraph;
    }

    
    /**
     * Generates a new http URI that will be used as the canonical one in place
     * of a set of equivalent non-http URIs. An owl:sameAs statement is added to
     * the interlinking graph stating that the canonical http URI is equivalent
     * to one of the non-http URI in the set of equivalent URIs.
     *
     * @param uriRefs
     * @return
     */
    private UriRef generateNewHttpUri(DataSet dataSet, Set<UriRef> uriRefs) {
        UriRef bestNonHttp = chooseBest(uriRefs);
        String nonHttpString = bestNonHttp.getUnicodeString();
        String URN_SCHEME = null;
        for(int i = 0; i < URN_SCHEMES.length; i++){
            if(nonHttpString.startsWith(URN_SCHEMES[i]))
                URN_SCHEME = URN_SCHEMES[i];
        }
        
        if ( URN_SCHEME == null) {
            throw new RuntimeException("Sorry this non-hhtp URI cannot handled: " + nonHttpString);
        }
        
        String httpUriString = nonHttpString.replaceFirst(URN_SCHEME, baseUri + "/");
        //TODO check that this URI is in fact new
        UriRef httpUriRef = new UriRef(httpUriString);
        // add an owl:sameAs statement in the interlinking graph 
        dataSet.getInterlinksGraph().add(new TripleImpl(bestNonHttp, OWL.sameAs, httpUriRef));
        return httpUriRef;
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

    /**
     * For each rdf triple collection uploaded 5 graphs are created. 1) a source
     * graph to store the rdf data 2) an enhancements graph to store the text
     * extracted for indexing and the entities extracted from the text by NLP
     * engines in the default enhancement chain 3) a graph to store the result
     * of the interlinking task 4) a graph to store the smushed graph 5) a graph
     * to store the published graph i.e. the smushed graph in a coherent state
     * with data in the content graph The name convention for these graphs is
     * GRAPH_URN_PREFIX + timestamp + SUFFIX where SUFFIX can be one of
     * SOURCE_GRAPH_URN_SUFFIX, ENHANCE_GRAPH_URN_SUFFIX,
     * INTERLINK_GRAPH_URN_SUFFIX, SMUSH_GRAPH_URN_SUFFIX,
     * PUBLISH_GRAPH_URN_SUFFIX
     */
    class DataSet {

        private UriRef dataSetUri;

        DataSet(UriRef dataSetUri) {
            this.dataSetUri = dataSetUri;
        }

        /**
         *
         * @return the graph containing the enhanced data
         */
        public LockableMGraph getEnhancedGraph() {
            return tcManager.getMGraph(getEnhancedGraphRef());
        }

        public UriRef getEnhancedGraphRef() {
            return new UriRef(dataSetUri.getUnicodeString() + ENHANCE_GRAPH_URN_SUFFIX);

        }
        
        /**
         *
         * @return the graph containing the activity log of the dataset
         */
        public LockableMGraph getLogGraph() {
            try {
                return tcManager.getMGraph(getLogGraphRef());
            } catch (NoSuchEntityException e) {
                return tcManager.createMGraph(getLogGraphRef());
            }
        }

        public UriRef getLogGraphRef() {
            return new UriRef(dataSetUri.getUnicodeString() + LOG_GRAPH_URN_SUFFIX);

        }
        
        /**
        *
        * @return the graph containing the digested content to be used for enhancements and indexing
        */
        public LockableMGraph getDigestGraph() {
           try {
               return tcManager.getMGraph(getDigestGraphRef());
           } catch (NoSuchEntityException e) {
               return tcManager.createMGraph(getDigestGraphRef());
           }
        }

        public UriRef getDigestGraphRef() {
           return new UriRef(dataSetUri.getUnicodeString() + DIGEST_GRAPH_URN_SUFFIX);

        }

        public LockableMGraph getInterlinksGraph() {
            return tcManager.getMGraph(getInterlinksGraphRef());
        }

        public UriRef getInterlinksGraphRef() {
            return new UriRef(dataSetUri.getUnicodeString() + INTERLINK_GRAPH_URN_SUFFIX);

        }

        private UriRef getSourceGraphRef() {
            return new UriRef(dataSetUri.getUnicodeString() + SOURCE_GRAPH_URN_SUFFIX);
        }
        
        public LockableMGraph getSourceGraph() {
            return tcManager.getMGraph(getSourceGraphRef());
        }

        public LockableMGraph getSmushGraph() {
            return tcManager.getMGraph(new UriRef(dataSetUri.getUnicodeString() + SMUSH_GRAPH_URN_SUFFIX));
        }

        public LockableMGraph getPublishGraph() {
            return tcManager.getMGraph(new UriRef(dataSetUri.getUnicodeString() + PUBLISH_GRAPH_URN_SUFFIX));
        }

        private UriRef getUri() {
            return dataSetUri;
        }

        

    }
}
