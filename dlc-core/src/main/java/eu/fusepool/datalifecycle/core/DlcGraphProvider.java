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
package eu.fusepool.datalifecycle.core;

import java.security.Permission;
import java.util.Collections;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.access.LockableMGraph;
import org.apache.clerezza.rdf.core.access.NoSuchEntityException;
import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.rdf.core.access.security.TcAccessController;
import org.apache.clerezza.rdf.core.access.security.TcPermission;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;

@Component
@Service(DlcGraphProvider.class)
public class DlcGraphProvider {
    
    @Reference
    private TcManager tcManager;
    
    /**
     * Name of the data life cycle graph. It is used as a register of other
     * graphs to manage their life cycle
     */
    //TODO why is this name used outside this class and as resource in other places?
    public static final UriRef DATA_LIFECYCLE_GRAPH_REFERENCE = new UriRef("urn:x-localinstance:/dlc/meta.graph");
    
    /**
     * Returns the data life cycle graph containing all the monitored graphs. It
     * creates it if doesn't exit yet.
     *
     * @return
     */
    public LockableMGraph getDlcGraph() {
        try {
            return tcManager.getMGraph(DATA_LIFECYCLE_GRAPH_REFERENCE);
        } catch(NoSuchEntityException ex) {
            return createDlcGraph();
        }
    }
    
    /**
     * Creates the data lifecycle graph. Must be called at the bundle activation
     * if the graph doesn't exists yet.
     */
    private LockableMGraph createDlcGraph() {
        LockableMGraph dlcGraph = tcManager.createMGraph(DATA_LIFECYCLE_GRAPH_REFERENCE);
        TcAccessController tca = tcManager.getTcAccessController();
        tca.setRequiredReadPermissions(DATA_LIFECYCLE_GRAPH_REFERENCE,
                Collections.singleton((Permission) new TcPermission(
                                "urn:x-localinstance:/content.graph", "read")));
        return dlcGraph;
    }
    
}
