package eu.fusepool.datalifecycle;

import java.io.InputStream;

import org.apache.clerezza.rdf.core.MGraph;

public interface Rdfizer {
	
	public MGraph transform(InputStream stream);
	
	public String getName();

}
