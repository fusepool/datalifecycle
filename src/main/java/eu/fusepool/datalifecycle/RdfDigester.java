package eu.fusepool.datalifecycle;

import org.apache.clerezza.rdf.core.MGraph;

/**
 * Services implementing this interface transform a Graph so that it becomes
 * processable following DLC and ECS conventions. Most notably a textual 
 * representation of a resource (that will be used forn enhancement as well as 
 * for full text search) shall be the value of a sioc:content property.
 *
 */

public interface RdfDigester {
	
	public void extractText(MGraph graph);
	
	// returns the name of the service implementation
	public String getName();

}
