/*
* Copyright 2013 Fusepool Project.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package eu.fusepool.datalifecycle;

import java.security.AccessController;
import java.security.AllPermission;
import java.util.Iterator;
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
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.access.LockableMGraph;
import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.rdf.core.impl.PlainLiteralImpl;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.clerezza.rdf.core.serializedform.Serializer;
import org.apache.clerezza.rdf.ontologies.RDF;
import org.apache.clerezza.rdf.ontologies.RDFS;
import org.apache.clerezza.rdf.utils.GraphNode;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.indexedgraph.IndexedMGraph;
import org.apache.stanbol.commons.web.viewable.RdfViewable;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@Property(name = "javax.ws.rs", boolValue = true)
@Service(Object.class)
@Path("pipesadmin")
public class PipesAdmin {
	
	/**
     * This service allows accessing and creating persistent triple collections
     */
    @Reference
    private TcManager tcManager;
    
    @Reference
    private Serializer serializer;
    
    @Reference
    private DataSetFactory dataSetFactory;
    
    @Reference
    private DlcGraphProvider dlcGraphProvider;
    
    /**
     * Using slf4j for normal logging
     */
    private static final Logger log = LoggerFactory.getLogger(PipesAdmin.class);
    
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
        final MGraph responseGraph = new IndexedMGraph();
        Lock rl = getDlcGraph().getLock().readLock();
        rl.lock();
        try {
            responseGraph.addAll(getDlcGraph());
            //Add the size info of the graphs of all the datasets
            addGraphsSize(responseGraph);
        } finally {
            rl.unlock();
        }
        
        //This GraphNode represents the service within our result graph
        final GraphNode node = new GraphNode(DlcGraphProvider.DATA_LIFECYCLE_GRAPH_REFERENCE, responseGraph);
        
        
        
        //What we return is the GraphNode to the template with the same path and name 
        return new RdfViewable("PipesAdmin", node, PipesAdmin.class);
    }
    /**
     * Add the size of the graphs within each dataset/pipe to the rdf data for visualization
     */
    private void addGraphsSize(MGraph responseGraph){
        
        Iterator<Triple> datasets = getDlcGraph().filter(DlcGraphProvider.DATA_LIFECYCLE_GRAPH_REFERENCE, Ontology.pipe, null);        
        while(datasets.hasNext()){
            final UriRef datasetRef = (UriRef) datasets.next().getObject();
            final DataSet dataSet = dataSetFactory.getDataSet(datasetRef);
            // add source graph size
            int sourceGraphSize = dataSet.getSourceGraph().size();
            responseGraph.add(new TripleImpl(dataSet.getSourceGraphRef(), Ontology.size, new PlainLiteralImpl(Integer.toString(sourceGraphSize))));
            // add digest graph size
            int digestGraphSize = dataSet.getDigestGraph().size();
            responseGraph.add(new TripleImpl(dataSet.getDigestGraphRef(), Ontology.size, new PlainLiteralImpl(Integer.toString(digestGraphSize))));
            // add enhance graph size
            int enhanceGraphSize = dataSet.getEnhanceGraph().size();
            responseGraph.add(new TripleImpl(dataSet.getEnhanceGraphRef(), Ontology.size, new PlainLiteralImpl(Integer.toString(enhanceGraphSize))));
            // add interlink graph size
            int interlinkGraphSize = dataSet.getInterlinksGraph().size();
            responseGraph.add(new TripleImpl(dataSet.getInterlinksGraphRef(), Ontology.size, new PlainLiteralImpl(Integer.toString(interlinkGraphSize))));
            // add smush graph size
            int smushGraphSize = dataSet.getSmushGraph().size();
            responseGraph.add(new TripleImpl(dataSet.getSmushGraphRef(), Ontology.size, new PlainLiteralImpl(Integer.toString(smushGraphSize))));
            // add publish graph size
            int publishGraphSize = dataSet.getPublishGraph().size();
            responseGraph.add(new TripleImpl(dataSet.getPublishGraphRef(), Ontology.size, new PlainLiteralImpl(Integer.toString(publishGraphSize))));
            
        }
        
        
    }
    /**
     * Removes all the triples from the selected graph.
     * @param uriInfo
     * @param graphName
     * @return
     * @throws Exception
     */
    @POST
    @Path("clear_graph")
    @Produces("text/plain")
    public Response clearGraphRequest(@Context final UriInfo uriInfo,  
    		@FormParam("graph") final String graphName) throws Exception {
        AccessController.checkPermission(new AllPermission());
        String message = "";
        
        UriRef graphRef = new UriRef(graphName);
        
        tcManager.getMGraph(graphRef).clear();
        
        message += " Graph: " + graphName + " empty";
        
        return RedirectUtil.createSeeOtherResponse("./", uriInfo);
        
        //return message;
    }
    
    /**
     * Removes the published triples from the content graph. More precisely the same triples stored in the publish.graph of a dataset
     * will be removed from the content graph. Then all the triples in publish.graph are deleted so that data could be published again
     * starting from smush.graph
     */
    @POST
    @Path("unpublish_dataset")
    @Produces("text/plain")
    public Response unpublishDataset(@Context final UriInfo uriInfo,  
                       @FormParam("pipe") final UriRef pipeRef) {
        final DataSet dataSet = dataSetFactory.getDataSet(pipeRef);
        String message = "";
        LockableMGraph publishGraph = dataSet.getPublishGraph();
        int numberOfTriples = publishGraph.size(); 
        
        if(numberOfTriples > 0) {
            
            MGraph publishedTriples = new IndexedMGraph();
            Lock pwl = publishGraph.getLock().readLock();
            pwl.lock();
            try {
                publishedTriples.addAll(publishGraph);
              
            }
            finally {
                pwl.unlock();
            }
        
            // remove published triples from content graph
            LockableMGraph contentGraph = tcManager.getMGraph(new UriRef(SourcingAdmin.CONTENT_GRAPH_NAME));
            contentGraph.removeAll(publishedTriples);
           
            // removes all the triples in publish.graph
            publishGraph.clear();
              
            message += "All " + numberOfTriples + " triples have been removed from the content graph.";
        }
        else {
            message += "There are no triples in " + pipeRef;
        }
        
        // update the dataset status (unpublished)
        updateDatasetStatus(pipeRef);
        
        return RedirectUtil.createSeeOtherResponse("./", uriInfo);
        //return message;
    }
    
    /**
     * Deletes all the graphs created with the pipe: rdf.graph, enhance.graph, interlink.graph, smush.graph, publish.graph.
     * Removes from the DLC meta graph all the pipe metadata.
     * @param uriInfo
     * @param dataSetUri
     * @return
     * @throws Exception
     */
    @POST
    @Path("delete_pipe")
    @Produces("text/plain")
    public Response deletePipe(@Context final UriInfo uriInfo,  
    		@FormParam("pipe") final UriRef dataSetUri) throws Exception {
        AccessController.checkPermission(new AllPermission());
        String message = "";
        
        final DataSet dataSet = dataSetFactory.getDataSet(dataSetUri);
        // remove graphs
        tcManager.deleteTripleCollection(dataSet.getSourceGraphRef());
        tcManager.deleteTripleCollection(dataSet.getDigestGraphRef());
        tcManager.deleteTripleCollection(dataSet.getEnhanceGraphRef());
        tcManager.deleteTripleCollection(dataSet.getInterlinksGraphRef());
        tcManager.deleteTripleCollection(dataSet.getSmushGraphRef());
        tcManager.deleteTripleCollection(dataSet.getLogGraphRef());
        
        LockableMGraph publishGraph = dataSet.getPublishGraph();
        MGraph publishedTriples = new IndexedMGraph();
        Lock pl = publishGraph.getLock().readLock();
        pl.lock();
        try {
            publishedTriples.addAll(publishGraph);
          
        }
        finally {
            pl.unlock();
        }
        
        // Unpublish data. Removes published data from content graph.  
        if(publishedTriples.size() > 0){
            LockableMGraph contentGraph = tcManager.getMGraph(new UriRef(SourcingAdmin.CONTENT_GRAPH_NAME));
            contentGraph.removeAll(publishedTriples);
            
        }
        
        tcManager.deleteTripleCollection(dataSet.getPublishGraphRef());
        
        // remove pipe metadata
        removePipeMetaData(dataSetUri);
        
        message += "The dataset: " + dataSetUri + " has been deleted";
        return RedirectUtil.createSeeOtherResponse("./", uriInfo);
        //return message;
    }
    
    /**
     * Updates the status of a dataset to unpublished
     * @param pipeName
     */
    private void updateDatasetStatus(final UriRef datasetUri) {
        final LockableMGraph dlcGraph = dlcGraphProvider.getDlcGraph();
        final UriRef statusRef = new UriRef(datasetUri.getUnicodeString() + "/Status");
        dlcGraph.remove(new TripleImpl(statusRef, RDF.type, Ontology.Published));
        dlcGraph.remove(new TripleImpl(statusRef, RDFS.label, new PlainLiteralImpl("Published")));
        dlcGraph.add(new TripleImpl(statusRef, RDF.type, Ontology.Unpublished));
        dlcGraph.add(new TripleImpl(statusRef, RDFS.label, new PlainLiteralImpl("Unpublished")));
    }
    
    /**
     * Removes all the triples related to the pipe in the DLC graph: graphs and tasks metadata
     * and pipe metadata.
     * @param pipeRef
     */
    private void removePipeMetaData(UriRef dataSetUri) {    	
    	
        final DataSet dataSet = dataSetFactory.getDataSet(dataSetUri);
        
    	// triple to remove
    	SimpleMGraph pipeGraph = new SimpleMGraph(); 
    	
    	Lock rl = getDlcGraph().getLock().readLock();
        rl.lock();
        
        try {
    	
	    	// select source graph and rdf task metadata
	    	UriRef rdfTaskRef = new UriRef(dataSetUri.getUnicodeString() + "/rdf");
	    	UriRef sourceGraphRef = dataSet.getSmushGraphRef();
	    	Iterator<Triple> isourceGraph = getDlcGraph().filter(sourceGraphRef, null, null);
	    	while(isourceGraph.hasNext()) {
	    		pipeGraph.add(isourceGraph.next());
	    	}
	    	Iterator<Triple> irdfTask = getDlcGraph().filter(rdfTaskRef, null, null);
	    	while(irdfTask.hasNext()) {
	    		pipeGraph.add(irdfTask.next());
	    	}
	    	
	    	// select digest graph and task metadata 
            UriRef digestTaskRef = new UriRef(dataSetUri.getUnicodeString() + "/digest");
            UriRef digestGraphRef = dataSet.getDigestGraphRef();
            Iterator<Triple> idigestGraph = getDlcGraph().filter(digestGraphRef, null, null);
            while(idigestGraph.hasNext()) {
                pipeGraph.add(idigestGraph.next());
            }
            Iterator<Triple> idigestTask = getDlcGraph().filter(digestTaskRef, null, null);
            while(idigestTask.hasNext()) {
                pipeGraph.add(idigestTask.next());
            }
	    	
	    	// select enhance graph and task metadata 
	    	UriRef enhanceTaskRef = new UriRef(dataSetUri.getUnicodeString() + "/enhance");
	    	UriRef enhanceGraphRef = dataSet.getEnhanceGraphRef();
	    	Iterator<Triple> ienhanceGraph = getDlcGraph().filter(enhanceGraphRef, null, null);
	    	while(ienhanceGraph.hasNext()) {
	    		pipeGraph.add(ienhanceGraph.next());
	    	}
	    	Iterator<Triple> ienhanceTask = getDlcGraph().filter(enhanceTaskRef, null, null);
	    	while(ienhanceTask.hasNext()) {
	    		pipeGraph.add(ienhanceTask.next());
	    	}
	    	    	
	    	// select interlink graph and task metadata
	    	UriRef interlinkTaskRef = new UriRef(dataSetUri.getUnicodeString() + "/interlink");
	    	UriRef interlinkGraphRef = dataSet.getInterlinksGraphRef();
	    	Iterator<Triple> iinterlinkGraph = getDlcGraph().filter(interlinkGraphRef, null, null);
	    	while(iinterlinkGraph.hasNext()) {
	    		pipeGraph.add(iinterlinkGraph.next());
	    	}
	    	Iterator<Triple> iinterlinkTask = getDlcGraph().filter(interlinkTaskRef, null, null);
	    	while(iinterlinkTask.hasNext()) {
	    		pipeGraph.add(iinterlinkTask.next());
	    	}
	    
	    	
	    	// select smush graph and task metadata
	    	UriRef smushTaskRef = new UriRef(dataSetUri.getUnicodeString() + "/smush");
	    	UriRef smushGraphRef = dataSet.getSmushGraphRef();
	    	Iterator<Triple> ismushGraph = getDlcGraph().filter(smushGraphRef, null, null);
	    	while(ismushGraph.hasNext()) {
	    		pipeGraph.add(ismushGraph.next());
	    	}
	    	Iterator<Triple> ismushTask = getDlcGraph().filter(smushTaskRef, null, null);
	    	while(ismushTask.hasNext()) {
	    		pipeGraph.add(ismushTask.next());
	    	}
	    	
	    	// select publish graph and task metadata
            UriRef publishTaskRef = new UriRef(dataSetUri.getUnicodeString()  + "/publish");
            UriRef publishGraphRef = dataSet.getPublishGraphRef();
            Iterator<Triple> ipublishGraph = getDlcGraph().filter(publishGraphRef, null, null);
            while(ipublishGraph.hasNext()) {
                pipeGraph.add(ipublishGraph.next());
            }
            Iterator<Triple> ipublishTask = getDlcGraph().filter(publishTaskRef, null, null);
            while(ipublishTask.hasNext()) {
                pipeGraph.add(ipublishTask.next());
            }
            
            // select dataset status
            UriRef datasetStatusRef = new UriRef(dataSetUri.getUnicodeString()  + "/Status");
            Iterator<Triple> idatasetStatus = getDlcGraph().filter(datasetStatusRef, null, null);
            while(idatasetStatus.hasNext()) {
                pipeGraph.add(idatasetStatus.next());
            }
	    	
	    	// select pipe metadata
	    	Iterator<Triple> ipipe = getDlcGraph().filter(dataSetUri, null, null);
	    	while(ipipe.hasNext()) {
	    		pipeGraph.add(ipipe.next());
	    	}
    	
        }
        finally {
        	rl.unlock();
        }                
        
        getDlcGraph().removeAll(pipeGraph);
        
        getDlcGraph().remove(new TripleImpl(dataSetUri, RDF.type, Ontology.Pipe));
        getDlcGraph().remove(new TripleImpl(DlcGraphProvider.DATA_LIFECYCLE_GRAPH_REFERENCE, Ontology.pipe, dataSetUri));
        
    }
    
    /**
     * Retrieves the selected graph.
     * @param uriInfo
     * @param graphName
     * @return
     * @throws Exception
     */
    @GET
    @Path("get_graph")
    @Produces("text/plain")
    public Response getGraphRequest(@Context final UriInfo uriInfo,
    		@QueryParam("graph") final String graphName) throws Exception {
    	
    	String graphUrl = uriInfo.getBaseUri() + "graph?name=" + graphName;
    	
    	
    	return RedirectUtil.createSeeOtherResponse(graphUrl, uriInfo);
    	
    	
    }
    
    
    /**
     * Returns the data life cycle graph containing all the monitored graphs. It
     * creates it if doesn't exit yet.
     *
     * @return
     */
    private LockableMGraph getDlcGraph() {
        return tcManager.getMGraph(DlcGraphProvider.DATA_LIFECYCLE_GRAPH_REFERENCE);
    }
    
    
    @Activate
    protected void activate(ComponentContext context) {

        log.info("The Graphs Admin service is being activated");

    }
    
    @Deactivate
    protected void deactivate(ComponentContext context) {
        log.info("The Graphs Admin service is being deactivated");
    }
}
