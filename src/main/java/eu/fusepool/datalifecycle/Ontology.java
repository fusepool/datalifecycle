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

import org.apache.clerezza.rdf.core.UriRef;


/**
 * Ideally this should be a dereferenceable ontology on the web. Given such 
 * an ontology a class of constant (similar to this) can be generated with
 * the org.apache.clerezza:maven-ontologies-plugin
 */
public class Ontology {
    
    /**
     * Resources of this type can be dereferenced and will return an admin page.
     * 
     */
    public static final UriRef SourcingAdmin = new UriRef("http://fusepool.com/ontologies/interlinking#SourcingAdmin");
    public static final UriRef graph = new UriRef("http://fusepool.com/ontologies/interlinking#graph");
    
    public static final UriRef activeTask = new UriRef("http://fusepool.com/ontologies/interlinking#activeTask");
    
    //tasks
    public static final UriRef endDate = new UriRef("http://fusepool.com/ontologies/interlinking#endDate");
    
    //logs
    public static final UriRef LogEntry = new UriRef("http://fusepool.com/ontologies/interlinking#LogEntry");
    public static final UriRef retrievedURI = new UriRef("http://fusepool.com/ontologies/interlinking#retrievedURI");
    
    /**
     * A simple workflow ontology
     */
    // Classes
    public static final UriRef Pipe = new UriRef("http://example.com/ont/workflow/Pipe");
    public static final UriRef RdfTask = new UriRef("http://example.com/ont/workflow/RdfTask");
    public static final UriRef EnhanceTask = new UriRef("http://example.com/ont/workflow/enhanceTask");
    public static final UriRef InterlinkTask = new UriRef("http://example.com/ont/workflow/interlinkTask");
    public static final UriRef SmushTask = new UriRef("http://example.com/ont/workflow/smushTask");
    public static final UriRef PublishTask = new UriRef("http://example.com/ont/workflow/publishTask");
    public static final UriRef Service = new UriRef("http://example.com/ont/workflow/Service");
    // a graph is a product of a task
    public static final UriRef Product = new UriRef("http://example.com/ont/workflow/Product");
    
    // A dataset can be in one of the following status (member of one of the following disjoint classes) 
    public static final UriRef Published = new UriRef("http://fusepool.com/ontologies/interlinking#Published");
    public static final UriRef Unpublished = new UriRef("http://fusepool.com/ontologies/interlinking#Unpublished");
    
    // Properties
    
    public static final UriRef pipe = new UriRef("http://example.com/ont/workflow/pipe");
    
    // a pipe creates one or more tasks
    public static final UriRef creates = new UriRef("http://example.com/ont/workflow/creates");
    // a task delivers a product 
    public static final UriRef deliverable = new UriRef("http://example.com/ont/workflow/deliverable");
    // a task delivers a product using a service
    public static final UriRef service = new UriRef("http://example.com/ont/workflow/service");
    // a service sub property for xml to rdf transformation
    public static final UriRef rdfizeService = new UriRef("http://example.com/ont/workflow/rdfizeService");
    // a service sub property for data enhancements
    public static final UriRef enhanceService = new UriRef("http://example.com/ont/workflow/enhanceService");
    // a service sub property for interlinking
    public static final UriRef interlinkService = new UriRef("http://example.com/ont/workflow/interlinkService");
    // status of the dataset (unpublished/published)
    public static final UriRef status = new UriRef("http://example.com/ont/workflow/status");
    
    // VOID ontology
    // Dataset
    public static final UriRef voidDataset = new UriRef("http://rdfs.org/ns/void#Dataset");
    //Linkset
    public static final UriRef voidLinkset = new UriRef("http://rdfs.org/ns/void#Linkset");
    public static final UriRef voidSubjectsTarget = new UriRef("http://rdfs.org/ns/void#subjectsTarget");
    public static final UriRef voidObjectsTarget = new UriRef("http://rdfs.org/ns/void#objectsTarget");
    public static final UriRef voidLinkPredicate = new UriRef("http://rdfs.org/ns/void#linkPredicate");

}
