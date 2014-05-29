/*
 * Copyright 2014 reto.
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
import org.apache.clerezza.rdf.core.access.LockableMGraph;

/**
 *
 * @author reto
 */
public interface DataSet {

    /**
     *
     * @return the graph containing the digested content to be used for
     * enhancements and indexing
     */
    LockableMGraph getDigestGraph();

    UriRef getDigestGraphRef();

    /**
     *
     * @return the graph containing the enhanced data
     */
    LockableMGraph getEnhanceGraph();

    UriRef getEnhanceGraphRef();

    /**
     *
     * @return the graph containing the interlinks (owl:sameAs triples)
     */
    LockableMGraph getInterlinksGraph();

    UriRef getInterlinksGraphRef();

    /**
     *
     * @return the graph containing the activity log of the dataset
     */
    LockableMGraph getLogGraph();

    UriRef getLogGraphRef();

    LockableMGraph getPublishGraph();

    UriRef getPublishGraphRef();

    UriRef getSmushGraphRef();
    
    LockableMGraph getSmushGraph();

    
    UriRef getSourceGraphRef();

    LockableMGraph getSourceGraph();
    
    UriRef getUri();
    
}
