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
        final GraphNode node = new GraphNode(SourcingAdmin.DATA_LIFECYCLE_GRAPH_REFERENCE, responseGraph);
        
        
        
        //What we return is the GraphNode to the template with the same path and name 
        return new RdfViewable("PipesAdmin", node, PipesAdmin.class);
    }
    /**
     * Add the size of the graphs within each dataset/pipe to the rdf data for visualization
     */
    private void addGraphsSize(MGraph responseGraph){
        Iterator<Triple> datasets = getDlcGraph().filter(SourcingAdmin.DATA_LIFECYCLE_GRAPH_REFERENCE, Ontology.pipe, null);
        while(datasets.hasNext()){
            UriRef datasetRef = (UriRef) datasets.next().getObject();
            // add source graph size
            UriRef sourceGraphRef = new UriRef( datasetRef.getUnicodeString() + SourcingAdmin.SOURCE_GRAPH_URN_SUFFIX );
            int sourceGraphSize = tcManager.getMGraph( sourceGraphRef ).size();
            responseGraph.add(new TripleImpl(sourceGraphRef, Ontology.size, new PlainLiteralImpl(Integer.toString(sourceGraphSize))));
            // add enhance graph size
            UriRef enhanceGraphRef = new UriRef( datasetRef.getUnicodeString() + SourcingAdmin.ENHANCE_GRAPH_URN_SUFFIX );
            int enhanceGraphSize = tcManager.getMGraph( enhanceGraphRef ).size();
            responseGraph.add(new TripleImpl(enhanceGraphRef, Ontology.size, new PlainLiteralImpl(Integer.toString(enhanceGraphSize))));
            // add interlink graph size
            UriRef interlinkGraphRef = new UriRef( datasetRef.getUnicodeString() + SourcingAdmin.INTERLINK_GRAPH_URN_SUFFIX );
            int interlinkGraphSize = tcManager.getMGraph( interlinkGraphRef ).size();
            responseGraph.add(new TripleImpl(interlinkGraphRef, Ontology.size, new PlainLiteralImpl(Integer.toString(interlinkGraphSize))));
            // add smush graph size
            UriRef smushGraphRef = new UriRef( datasetRef.getUnicodeString() + SourcingAdmin.SMUSH_GRAPH_URN_SUFFIX );
            int smushGraphSize = tcManager.getMGraph( smushGraphRef ).size();
            responseGraph.add(new TripleImpl(smushGraphRef, Ontology.size, new PlainLiteralImpl(Integer.toString(smushGraphSize))));
            // add publish graph size
            UriRef publishGraphRef = new UriRef( datasetRef.getUnicodeString() + SourcingAdmin.PUBLISH_GRAPH_URN_SUFFIX );
            int publishGraphSize = tcManager.getMGraph( publishGraphRef ).size();
            responseGraph.add(new TripleImpl(publishGraphRef, Ontology.size, new PlainLiteralImpl(Integer.toString(publishGraphSize))));
            
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
                       @FormParam("pipe") final String pipeName) {
        String message = "";
        LockableMGraph publishGraph = tcManager.getMGraph(new UriRef(pipeName + SourcingAdmin.PUBLISH_GRAPH_URN_SUFFIX));
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
            message += "There are no triples in " + pipeName;
        }
        
        // update the dataset status (unpublished)
        updateDatasetStatus(pipeName);
        
        return RedirectUtil.createSeeOtherResponse("./", uriInfo);
        //return message;
    }
    
    /**
     * Deletes all the graphs created with the pipe: rdf.graph, enhance.graph, interlink.graph, smush.graph, publish.graph.
     * Removes from the DLC meta graph all the pipe metadata.
     * @param uriInfo
     * @param pipeName
     * @return
     * @throws Exception
     */
    @POST
    @Path("delete_pipe")
    @Produces("text/plain")
    public Response deletePipe(@Context final UriInfo uriInfo,  
    		@FormParam("pipe") final String pipeName) throws Exception {
        AccessController.checkPermission(new AllPermission());
        String message = "";
        
        // remove graphs
        tcManager.deleteTripleCollection(new UriRef(pipeName + SourcingAdmin.SOURCE_GRAPH_URN_SUFFIX));
        tcManager.deleteTripleCollection(new UriRef(pipeName + SourcingAdmin.ENHANCE_GRAPH_URN_SUFFIX));
        tcManager.deleteTripleCollection(new UriRef(pipeName + SourcingAdmin.INTERLINK_GRAPH_URN_SUFFIX));
        tcManager.deleteTripleCollection(new UriRef(pipeName + SourcingAdmin.SMUSH_GRAPH_URN_SUFFIX));
        tcManager.deleteTripleCollection(new UriRef(pipeName + SourcingAdmin.LOG_GRAPH_URN_SUFFIX));
        
        LockableMGraph publishGraph = tcManager.getMGraph(new UriRef(pipeName + SourcingAdmin.PUBLISH_GRAPH_URN_SUFFIX));
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
        
        tcManager.deleteTripleCollection(new UriRef(pipeName + SourcingAdmin.PUBLISH_GRAPH_URN_SUFFIX));
        
        // remove pipe metadata
        removePipeMetaData(pipeName);
        
        message += "The dataset: " + pipeName + " has been deleted";
        return RedirectUtil.createSeeOtherResponse("./", uriInfo);
        //return message;
    }
    
    /**
     * Updates the status of a dataset to unpublished
     * @param pipeName
     */
    private void updateDatasetStatus(String datasetName) {
        LockableMGraph dlcGraph = tcManager.getMGraph(SourcingAdmin.DATA_LIFECYCLE_GRAPH_REFERENCE);
        UriRef datasetRef = new UriRef(datasetName);
        UriRef statusRef = new UriRef(datasetRef.getUnicodeString() + "/Status");
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
    private void removePipeMetaData(String pipeName) {    	
    	
    	// triple to remove
    	SimpleMGraph pipeGraph = new SimpleMGraph(); 
    	
    	Lock rl = getDlcGraph().getLock().readLock();
        rl.lock();
        
        try {
    	
	    	// select source graph and rdf task metadata
	    	UriRef rdfTaskRef = new UriRef(pipeName + "/rdf");
	    	UriRef sourceGraphRef = new UriRef(pipeName + SourcingAdmin.SOURCE_GRAPH_URN_SUFFIX);
	    	Iterator<Triple> isourceGraph = getDlcGraph().filter(sourceGraphRef, null, null);
	    	while(isourceGraph.hasNext()) {
	    		pipeGraph.add(isourceGraph.next());
	    	}
	    	Iterator<Triple> irdfTask = getDlcGraph().filter(rdfTaskRef, null, null);
	    	while(irdfTask.hasNext()) {
	    		pipeGraph.add(irdfTask.next());
	    	}
	    	
	    	// select enhance graph and task metadata 
	    	UriRef enhanceTaskRef = new UriRef(pipeName + "/enhance");
	    	UriRef enhanceGraphRef = new UriRef(pipeName + SourcingAdmin.ENHANCE_GRAPH_URN_SUFFIX);
	    	Iterator<Triple> ienhanceGraph = getDlcGraph().filter(enhanceGraphRef, null, null);
	    	while(ienhanceGraph.hasNext()) {
	    		pipeGraph.add(ienhanceGraph.next());
	    	}
	    	Iterator<Triple> ienhanceTask = getDlcGraph().filter(enhanceTaskRef, null, null);
	    	while(ienhanceTask.hasNext()) {
	    		pipeGraph.add(ienhanceTask.next());
	    	}
	    	    	
	    	// select interlink graph and task metadata
	    	UriRef interlinkTaskRef = new UriRef(pipeName + "/interlink");
	    	UriRef interlinkGraphRef = new UriRef(pipeName + SourcingAdmin.INTERLINK_GRAPH_URN_SUFFIX);
	    	Iterator<Triple> iinterlinkGraph = getDlcGraph().filter(interlinkGraphRef, null, null);
	    	while(iinterlinkGraph.hasNext()) {
	    		pipeGraph.add(iinterlinkGraph.next());
	    	}
	    	Iterator<Triple> iinterlinkTask = getDlcGraph().filter(interlinkTaskRef, null, null);
	    	while(iinterlinkTask.hasNext()) {
	    		pipeGraph.add(iinterlinkTask.next());
	    	}
	    
	    	
	    	// select smush graph and task metadata
	    	UriRef smushTaskRef = new UriRef(pipeName + "/smush");
	    	UriRef smushGraphRef = new UriRef(pipeName + SourcingAdmin.SMUSH_GRAPH_URN_SUFFIX);
	    	Iterator<Triple> ismushGraph = getDlcGraph().filter(smushGraphRef, null, null);
	    	while(ismushGraph.hasNext()) {
	    		pipeGraph.add(ismushGraph.next());
	    	}
	    	Iterator<Triple> ismushTask = getDlcGraph().filter(smushTaskRef, null, null);
	    	while(ismushTask.hasNext()) {
	    		pipeGraph.add(ismushTask.next());
	    	}
	    	
	    	// select publish graph and task metadata
            UriRef publishTaskRef = new UriRef(pipeName + "/publish");
            UriRef publishGraphRef = new UriRef(pipeName + SourcingAdmin.PUBLISH_GRAPH_URN_SUFFIX);
            Iterator<Triple> ipublishGraph = getDlcGraph().filter(publishGraphRef, null, null);
            while(ipublishGraph.hasNext()) {
                pipeGraph.add(ipublishGraph.next());
            }
            Iterator<Triple> ipublishTask = getDlcGraph().filter(publishTaskRef, null, null);
            while(ipublishTask.hasNext()) {
                pipeGraph.add(ipublishTask.next());
            }
            
            // select dataset status
            UriRef datasetStatusRef = new UriRef(pipeName + "/Status");
            Iterator<Triple> idatasetStatus = getDlcGraph().filter(datasetStatusRef, null, null);
            while(idatasetStatus.hasNext()) {
                pipeGraph.add(idatasetStatus.next());
            }
	    	
	    	// select pipe metadata
	    	UriRef pipeRef = new UriRef(pipeName);
	    	Iterator<Triple> ipipe = getDlcGraph().filter(pipeRef, null, null);
	    	while(ipipe.hasNext()) {
	    		pipeGraph.add(ipipe.next());
	    	}
    	
        }
        finally {
        	rl.unlock();
        }                
        
        getDlcGraph().removeAll(pipeGraph);
        
        UriRef pipeRef = new UriRef(pipeName);
        getDlcGraph().remove(new TripleImpl(pipeRef, RDF.type, Ontology.Pipe));
        getDlcGraph().remove(new TripleImpl(SourcingAdmin.DATA_LIFECYCLE_GRAPH_REFERENCE, Ontology.pipe, pipeRef));
        
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
        return tcManager.getMGraph(SourcingAdmin.DATA_LIFECYCLE_GRAPH_REFERENCE);
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
