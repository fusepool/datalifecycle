package eu.fusepool.datalifecycle;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.AccessController;
import java.security.AllPermission;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.clerezza.rdf.core.serializedform.Parser;
import org.apache.clerezza.rdf.core.serializedform.Serializer;
import org.apache.clerezza.rdf.core.serializedform.SupportedFormat;
import org.apache.commons.io.IOUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;




@Component
@Property(name = "javax.ws.rs", boolValue = true)
@Service(Object.class)
@Path("dlcupload")
public class DlcUploader {
	
	private static final Logger log = LoggerFactory.getLogger(DlcUploader.class);

	
	@Reference
    private Parser parser;
	/**
     * This service allows accessing and creating persistent triple collections
     */
    @Reference
    private TcManager tcManager;
    
    @GET
    @Produces("text/plain")
    public String serviceEntry(@Context final UriInfo uriInfo,
    		@HeaderParam("user-agent") String userAgent) throws Exception {
        AccessController.checkPermission(new AllPermission());
        //String uriInfoStr = uriInfo.getRequestUri().toString();
        
        return userAgent;
    }
	
	/**
     * Load RDF data sent by HTTP POST. Use the Dataset custom header
     * to address the dataset in which to store the rdf data.
     * Use this service with the following curl command:
     *  curl -X POST -u admin: -H "Content-Type: application/rdf+xml" 
     *  	-H "Dataset: mydataset" -T <rdf_file> http://localhost:8080/dlcupload/rdf 
     */
    @POST
    @Path("rdf")
    @Produces("text/plain")
    public String uploadRdf(@Context final UriInfo uriInfo,  
    		@HeaderParam("Content-Type") String mediaType,
    		@HeaderParam("Dataset") String dataset,
            final InputStream stream) throws Exception {
    	
        AccessController.checkPermission(new AllPermission());
        final MGraph graph = new SimpleMGraph();
        
        String message = "";
       
        if(mediaType.equals(SupportedFormat.RDF_XML)) {
        	parser.parse(graph, stream, SupportedFormat.RDF_XML);
        }
        else {
        	message = "Add header Content-Type: application/rdf+xml ";
        }
        
        return message + "Added " + graph.size() + " triples  to dataset " + dataset + "\n";
    }
    
	@Activate
    protected void activate(ComponentContext context) {
		
		
        log.info("Uploader service is being activated");

    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        log.info("Uploader service is being deactivated");
    }
}

