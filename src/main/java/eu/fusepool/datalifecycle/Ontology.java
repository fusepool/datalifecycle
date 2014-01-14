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
    
    /**
     * A simple workflow ontology
     */
    // classes
    public static final UriRef Pipe = new UriRef("http://example.com/ont/workflow/Pipe");
    public static final UriRef RdfTask = new UriRef("http://example.com/ont/workflow/RdfTask");
    public static final UriRef EnhanceTask = new UriRef("http://example.com/ont/workflow/enhanceTask");
    public static final UriRef InterlinkTask = new UriRef("http://example.com/ont/workflow/interlinkTask");
    public static final UriRef SmushTask = new UriRef("http://example.com/ont/workflow/smushTask");
    public static final UriRef PublishTask = new UriRef("http://example.com/ont/workflow/publishTask");
    // a graph is a product of a task
    public static final UriRef Product = new UriRef("http://example.com/ont/workflow/Product");
    // properties
    public static final UriRef pipe = new UriRef("http://example.com/ont/workflow/pipe");
    // a pipe creates one or more tasks
    public static final UriRef creates = new UriRef("http://example.com/ont/workflow/creates");
    // a task delivers a product 
    public static final UriRef deliverable = new UriRef("http://example.com/ont/workflow/deliverable");
    
    // VOID ontology
    // Dataset
    public static final UriRef voidDataset = new UriRef("http://rdfs.org/ns/void#Dataset");
    //Linkset
    public static final UriRef voidLinkset = new UriRef("http://rdfs.org/ns/void#Linkset");
    public static final UriRef voidSubjectsTarget = new UriRef("http://rdfs.org/ns/void#subjectsTarget");
    public static final UriRef voidObjectsTarget = new UriRef("http://rdfs.org/ns/void#objectsTarget");
    public static final UriRef voidLinkPredicate = new UriRef("http://rdfs.org/ns/void#linkPredicate");

}
