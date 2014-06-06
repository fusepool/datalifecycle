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

import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.NonLiteral;
import org.apache.clerezza.rdf.core.Resource;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.access.LockableMGraph;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.clerezza.rdf.ontologies.OWL;
import static org.apache.clerezza.rdf.ontologies.PLATFORM.baseUri;
import org.apache.clerezza.rdf.utils.UnionMGraph;
import org.apache.clerezza.rdf.utils.smushing.SameAsSmusher;
import org.apache.stanbol.commons.indexedgraph.IndexedMGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provided the functionality to perform a smush job on dataset. This does 
 * not itself extend Task but is typically used from within a task. The task 
 * also ensures all temporary (urn:x-temp:) URI are replaced with HTTP URIs.
 * 
 * @author reto
 */
class SmushingJob {
    
    /**
     * Smush the union of the digest and enhancements graphs using the
     * interlinking graph. More precisely collates URIs coming from different
     * equivalent resources in a single one chosen among them. All the triples
     * in the union graph are copied in the smush graph that is then smushed
     * using the interlinking graph. URIs are canonicalized to http://
     *
     * @param graphToSmushRef
     * @return
     */
    static void perform(DataSet dataSet, PrintWriter messageWriter, UriRef baseUri) {
        new SmushingJob(dataSet, messageWriter, baseUri).perform();
    }
    
    // Scheme of non-http URI used
    static final String URN_SCHEME = "urn:x-temp:";
    
    private final DataSet dataSet;
    
    private static final Logger log = LoggerFactory.getLogger(SmushingJob.class);
    private final PrintWriter messageWriter;
    private final String baseUriString;
    
    private SmushingJob(DataSet dataSet, PrintWriter messageWriter, UriRef baseUri) {
        this.dataSet = dataSet;
        this.messageWriter = messageWriter;
        this.baseUriString = baseUri.getUnicodeString();
    }
    
    /**
     * Smush the union of the source, digest and enhancements graphs using the
     * interlinking graph. More precisely collates URIs coming from different
     * equivalent resources in a single one chosen among them. All the triples
     * in the union graph are copied in the smush graph that is then smushed
     * using the interlinking graph. URIs are canonicalized to http://
     *
     * @param graphToSmushRef
     * @return
     */
    void perform() {
        messageWriter.println("Smushing task.");


        final SameAsSmusher smusher = new SameAsSmusher() {

            @Override
            protected UriRef getPreferedIri(Set<UriRef> uriRefs
            ) {
                Set<UriRef> httpUri = new HashSet<UriRef>();
                for (UriRef uriRef : uriRefs) {
                    if (uriRef.getUnicodeString().startsWith("http")) {
                        httpUri.add(uriRef);
                    }
                }
                if (httpUri.size() == 1) {
                    return httpUri.iterator().next();
                }
                // There is no http URI in the set of equivalent resource. The entity was unknown. 
                // A new representation of the entity with http URI will be created. 
                if (httpUri.size() == 0) {
                    return generateNewHttpUri(dataSet, uriRefs);
                }
                if (httpUri.size() > 1) {
                    return chooseBest(httpUri);
                }
                throw new Error("Negative size set.");
            }

        };

        if (dataSet.getSmushGraph().size() > 0) {
            dataSet.getSmushGraph().clear();
        }

        LockableMGraph unionGraph = new UnionMGraph(dataSet.getDigestGraph(), dataSet.getEnhanceGraph());
        Lock erl = unionGraph.getLock().readLock();
        erl.lock();
        try {
            // add triples from enhance graph to smush graph
            dataSet.getSmushGraph().addAll(unionGraph);
            log.info("Copied " + unionGraph.size() + " triples from the union of source, digest and enhancements graph into the smush graph.");
            MGraph tempEquivalenceSet = new IndexedMGraph();
            tempEquivalenceSet.addAll(dataSet.getInterlinksGraph());
            log.info("Smush task started.");
            smusher.smush(dataSet.getSmushGraph(), tempEquivalenceSet, true);
            log.info("Smush task completed.");
        } finally {
            erl.unlock();
        }

        // Remove from smush graph equivalences between temporary uri (urn:x-temp) and http uri that are added by the clerezza smusher.
        // These equivalences must be removed as only equivalences between known entities (http uri) must be maintained and then published
        MGraph equivToRemove = new SimpleMGraph();
        Lock srl = dataSet.getSmushGraph().getLock().readLock();
        srl.lock();
        try {
            Iterator<Triple> isameas = dataSet.getSmushGraph().filter(null, OWL.sameAs, null);
            while (isameas.hasNext()) {
                Triple sameas = isameas.next();
                NonLiteral subject = sameas.getSubject();
                Resource object = sameas.getObject();
                if (subject.toString().startsWith("<" + URN_SCHEME) || object.toString().startsWith("<" + URN_SCHEME)) {
                    equivToRemove.add(sameas);
                }
            }
        } finally {
            srl.unlock();
        }

        dataSet.getSmushGraph().removeAll(equivToRemove);

        messageWriter.println("Smushing of " + dataSet.getUri()
                + "Smushed graph size = " + dataSet.getSmushGraph().size());
        canonicalizeResources();

    }
    
    

    /**
     * Generates a new http URI that will be used as the canonical one in place
     * of a set of equivalent non-http URIs. An owl:sameAs statement is added to
     * the interlinking graph stating that the canonical http URI is equivalent
     * to one of the non-http URI in the set of equivalent URIs.
     *
     * @param uriRefs
     * @return
     */
    private UriRef generateNewHttpUri(DataSet dataSet, Set<UriRef> uriRefs) {
        UriRef bestNonHttp = chooseBest(uriRefs);
        String nonHttpString = bestNonHttp.getUnicodeString();
        if (!nonHttpString.startsWith(URN_SCHEME)) {
            throw new RuntimeException("Sorry we current assume all non-http "
                    + "URIs to be canonicalized to be urn:x-temp, cannot handle: " + nonHttpString);
        }
        String httpUriString = nonHttpString.replaceFirst(URN_SCHEME, baseUriString);
        //TODO check that this URI is in fact new
        UriRef httpUriRef = new UriRef(httpUriString);
        // add an owl:sameAs statement in the interlinking graph 
        dataSet.getInterlinksGraph().add(new TripleImpl(bestNonHttp, OWL.sameAs, httpUriRef));
        return httpUriRef;
    }

    private UriRef chooseBest(Set<UriRef> httpUri) {
        Iterator<UriRef> iter = httpUri.iterator();
        UriRef best = iter.next();
        while (iter.hasNext()) {
            UriRef next = iter.next();
            if (next.getUnicodeString().compareTo(best.getUnicodeString()) < 0) {
                best = next;
            }
        }
        return best;
    }

    /**
     * All the resources in the smush graph must be http dereferencable when
     * published. All the triples in the smush graph are copied into a temporary
     * graph. For each triple the subject and the object that have a non-http
     * URI are changed in http uri and an equivalence link is added in the
     * interlinking graph for each resource (subject and object) that has been
     * changed.
     */
    private void canonicalizeResources() {
        LockableMGraph graph = dataSet.getSmushGraph();
        MGraph graphCopy = new SimpleMGraph();
        // graph containing the same triple with the http URI for each subject and object
        MGraph canonicGraph = new SimpleMGraph();
        Lock rl = graph.getLock().readLock();
        rl.lock();
        try {
            graphCopy.addAll(graph);
        } finally {
            rl.unlock();
        }

        Iterator<Triple> ismushTriples = graphCopy.iterator();
        while (ismushTriples.hasNext()) {
            Triple triple = ismushTriples.next();
            UriRef subject = (UriRef) triple.getSubject();
            Resource object = triple.getObject();
            // generate an http URI for both subject and object and add an equivalence link into the interlinking graph
            if (subject.getUnicodeString().startsWith(URN_SCHEME)) {
                subject = generateNewHttpUri(dataSet, Collections.singleton(subject));
            }
            if (object.toString().startsWith("<" + URN_SCHEME)) {
                object = generateNewHttpUri(dataSet, Collections.singleton((UriRef) object));
            }

            // add the triple with the http uris to the canonic graph
            canonicGraph.add(new TripleImpl(subject, triple.getPredicate(), object));
        }

        Lock wl = graph.getLock().writeLock();
        wl.lock();
        try {
            graph.clear();
            graph.addAll(canonicGraph);
        } finally {
            wl.unlock();
        }

    }

    
}
