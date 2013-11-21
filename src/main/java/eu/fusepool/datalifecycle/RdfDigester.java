package eu.fusepool.datalifecycle;

import org.apache.clerezza.rdf.core.MGraph;

/**
 * This interface defines the method signature that must be implemented by components to provide the following
 * services:
 * - extraction of text from RDF properties values to be included in a sioc:content property for indexing purposes
 * - addition of dc:subject properties to link entities found by NLP components (stanbol enhancements).
 * 
 * @author luigi
 *
 */

public interface RdfDigester {
	
	public void extractText(MGraph graph);

}
