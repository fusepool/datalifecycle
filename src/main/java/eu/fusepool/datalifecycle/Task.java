/*
 * Copyright 2014 Reto.
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

import eu.fusepool.datalifecycle.utils.LinksRetriever;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.ws.rs.core.UriInfo;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.ontologies.DCTERMS;
import org.apache.clerezza.rdf.ontologies.RDFS;
import org.apache.clerezza.rdf.utils.GraphNode;
import org.apache.stanbol.commons.indexedgraph.IndexedMGraph;

/**
 * A DataLifeCycle task
 */
abstract class Task extends Thread {
    private UriRef uriRef;
    private Date startDate;
    private Date endDate;
    private Date dateSubmitted;
    private final StringWriter messageStringWriter = new StringWriter();
    protected final PrintWriter log = new PrintWriter(messageStringWriter);
    private boolean terminationRequested;
    
    Task (UriInfo uriInfo) {
        try {
            final String resourcePath = uriInfo.getAbsolutePath().toString();
            URL baseUrl = new URL(resourcePath);
            URL url = new URL(baseUrl,"../task/"+ UUID.randomUUID());
            System.out.println("url: "+url);
            uriRef = new UriRef(url.toString());
            dateSubmitted = new Date();
        } catch (MalformedURLException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public synchronized void start() {
        startDate = new Date();
        super.start(); 
    }
    
    @Override
            public void run() {
                try {
                    execute();
                } catch (Exception ex) {
                    ex.printStackTrace(log);
                }
                endDate = new Date();
            }     
    
    UriRef getUri() {
        return uriRef;
    }

    GraphNode getNode() {
        MGraph base = new IndexedMGraph();
        GraphNode result = new GraphNode(uriRef, base);
        result.addPropertyValue(DCTERMS.dateSubmitted, dateSubmitted);
        if (startDate != null) {
            result.addPropertyValue(DCTERMS.dateAccepted, startDate);
        }
        if (endDate != null) {
            result.addPropertyValue(Ontology.endDate, endDate);
        }
        result.addPropertyValue(RDFS.comment, messageStringWriter.toString());
        return result;
    }

    protected abstract void execute();
    
    public boolean isTerminationRequested() {
        return terminationRequested;
    }

    public void requestTermination() {
        this.terminationRequested = true;
    }
    
}
