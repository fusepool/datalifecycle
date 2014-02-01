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

import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.UriRef;

public interface Interlinker {

    /**
     * Interlinks some data within itself and against the resources in graph 
     * provided by its IRI. 
     * 
     * @param dataToInterlink the data to interlink
     * @param interlinkAgainst the IRI of the graph to interlink against
     * @param the identifier of the set of rules to be applied among those available in the config file 
     * @return a collection of owl:sameAs statements
     */
    public TripleCollection interlink(TripleCollection dataToInterlink, 
            UriRef interlinkAgainst, String linkSpecId);
    
}
