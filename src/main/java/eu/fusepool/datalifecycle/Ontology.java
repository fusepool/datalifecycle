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
    
    // VOID ontology - Linkset
    public static final UriRef voidLinkset = new UriRef("http://rdfs.org/ns/void#Linkset");

}
