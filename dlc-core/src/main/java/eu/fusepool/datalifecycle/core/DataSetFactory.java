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

import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.access.LockableMGraph;
import org.apache.clerezza.rdf.core.access.NoSuchEntityException;
import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.rdf.core.impl.PlainLiteralImpl;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.clerezza.rdf.ontologies.OWL;
import org.apache.clerezza.rdf.ontologies.RDF;
import org.apache.clerezza.rdf.ontologies.RDFS;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import eu.fusepool.datalifecycle.ontologies.DLC;

@Component
@Service(DataSetFactory.class)
public class DataSetFactory {
    
    @Reference
    private TcManager tcManager;
    
    @Reference
    private DlcGraphProvider dlcGraphProvider;
    
    // base graph uri
    public static final String GRAPH_URN_PREFIX = "urn:x-localinstance:/dlc/";
    
    public DataSet getDataSet(UriRef dataSetUri) {
        return new DataSetImpl(dataSetUri);
    }
    
    public DataSet createDataSet(String datasetName) {
        if ((datasetName == null) || "".equals(datasetName)) {
            throw new IllegalArgumentException("DatasetName must be non null and non empty");
        }
        final UriRef dataSetUri = new UriRef(GRAPH_URN_PREFIX + datasetName);
        final DataSetImpl dataSet = new DataSetImpl(dataSetUri);
        dataSet.initialize(datasetName);
        return dataSet;
    }

    /**
     * For each rdf triple collection uploaded 5 graphs are created. 1) a source
     * graph to store the rdf data 2) an enhancements graph to store the text
     * extracted for indexing and the entities extracted from the text by NLP
     * engines in the default enhancement chain 3) a graph to store the result
     * of the interlinking task 4) a graph to store the smushed graph 5) a graph
     * to store the published graph i.e. the smushed graph in a coherent state
     * with data in the content graph The name convention for these graphs is
     * GRAPH_URN_PREFIX + timestamp + SUFFIX where SUFFIX can be one of
     * SOURCE_GRAPH_URN_SUFFIX, ENHANCE_GRAPH_URN_SUFFIX,
     * INTERLINK_GRAPH_URN_SUFFIX, SMUSH_GRAPH_URN_SUFFIX,
     * PUBLISH_GRAPH_URN_SUFFIX
     */
    class DataSetImpl implements DataSet {

        // graph suffix

        public static final String SOURCE_GRAPH_URN_SUFFIX = "/rdf.graph";
        // digest graph suffix
        public static final String DIGEST_GRAPH_URN_SUFFIX = "/digest.graph";
        // enhancements graph suffix
        public static final String ENHANCE_GRAPH_URN_SUFFIX = "/enhance.graph";
        // log graph suffix
        public static final String LOG_GRAPH_URN_SUFFIX = "/log.graph";
        // interlink graph suffix
        public static final String INTERLINK_GRAPH_URN_SUFFIX = "/interlink.graph";
        // smushed graph suffix
        public static final String SMUSH_GRAPH_URN_SUFFIX = "/smush.graph";
        // published graph suffix
        public static final String PUBLISH_GRAPH_URN_SUFFIX = "/publish.graph";
        private UriRef dataSetUri;

        DataSetImpl(UriRef dataSetUri) {
            this.dataSetUri = dataSetUri;
        }

        /**
         *
         * @return the graph containing the enhanced data
         */
        @Override
        public LockableMGraph getEnhanceGraph() {
            try {
                return tcManager.getMGraph(getEnhanceGraphRef());
            } catch (NoSuchEntityException e) {
                return tcManager.createMGraph(getLogGraphRef());
            }
        }

        @Override
        public UriRef getEnhanceGraphRef() {
            return new UriRef(dataSetUri.getUnicodeString() + ENHANCE_GRAPH_URN_SUFFIX);
        }

        /**
         *
         * @return the graph containing the activity log of the dataset
         */
        @Override
        public LockableMGraph getLogGraph() {
            try {
                return tcManager.getMGraph(getLogGraphRef());
            } catch (NoSuchEntityException e) {
                return tcManager.createMGraph(getLogGraphRef());
            }
        }

        @Override
        public UriRef getLogGraphRef() {
            return new UriRef(dataSetUri.getUnicodeString() + LOG_GRAPH_URN_SUFFIX);
        }

        /**
         *
         * @return the graph containing the digested content to be used for
         * enhancements and indexing
         */
        @Override
        public LockableMGraph getDigestGraph() {
            try {
                return tcManager.getMGraph(getDigestGraphRef());
            } catch (NoSuchEntityException e) {
                return tcManager.createMGraph(getDigestGraphRef());
            }
        }

        @Override
        public UriRef getDigestGraphRef() {
            return new UriRef(dataSetUri.getUnicodeString() + DIGEST_GRAPH_URN_SUFFIX);
        }

        /**
         *
         * @return the graph containing the interlinks (owl:sameAs triples)
         */
        @Override
        public LockableMGraph getInterlinksGraph() {
            return tcManager.getMGraph(getInterlinksGraphRef());
        }

        @Override
        public UriRef getInterlinksGraphRef() {
            return new UriRef(dataSetUri.getUnicodeString() + INTERLINK_GRAPH_URN_SUFFIX);
        }

        public UriRef getSourceGraphRef() {
            return new UriRef(dataSetUri.getUnicodeString() + SOURCE_GRAPH_URN_SUFFIX);
        }

        @Override
        public LockableMGraph getSourceGraph() {
            return tcManager.getMGraph(getSourceGraphRef());
        }

        @Override
        public UriRef getSmushGraphRef() {
            return new UriRef(dataSetUri.getUnicodeString() + SMUSH_GRAPH_URN_SUFFIX);
        }

        @Override
        public LockableMGraph getSmushGraph() {
            return tcManager.getMGraph(getSmushGraphRef());
        }

        @Override
        public UriRef getPublishGraphRef() {
            return new UriRef(dataSetUri.getUnicodeString() + PUBLISH_GRAPH_URN_SUFFIX);
        }

        @Override
        public LockableMGraph getPublishGraph() {
            return tcManager.getMGraph(getPublishGraphRef());
        }

        @Override
        public UriRef getUri() {
            return dataSetUri;
        }

        void initialize(final String datasetName) {
            dlcGraphProvider.getDlcGraph().add(new TripleImpl(dataSetUri, RDF.type, DLC.Pipe));
            dlcGraphProvider.getDlcGraph().add(new TripleImpl(dataSetUri, RDFS.label, new PlainLiteralImpl(datasetName)));
            dlcGraphProvider.getDlcGraph().add(new TripleImpl(DlcGraphProvider.DATA_LIFECYCLE_GRAPH_REFERENCE, DLC.pipe, dataSetUri));
            /* waht are tasks, and what are this tripples for?*/
        // create tasks
            //rdf task
            UriRef rdfTaskRef = new UriRef(dataSetUri.getUnicodeString() + "/rdf");
            dlcGraphProvider.getDlcGraph().add(new TripleImpl(dataSetUri, DLC.creates, rdfTaskRef));
            dlcGraphProvider.getDlcGraph().add(new TripleImpl(rdfTaskRef, RDF.type, DLC.RdfTask));
            // digest task
            UriRef digestTaskRef = new UriRef(dataSetUri.getUnicodeString() + "/digest");
            dlcGraphProvider.getDlcGraph().add(new TripleImpl(dataSetUri, DLC.creates, digestTaskRef));
            dlcGraphProvider.getDlcGraph().add(new TripleImpl(digestTaskRef, RDF.type, DLC.DigestTask));
            // enhance task
            UriRef enhanceTaskRef = new UriRef(dataSetUri.getUnicodeString() + "/enhance");
            dlcGraphProvider.getDlcGraph().add(new TripleImpl(dataSetUri, DLC.creates, enhanceTaskRef));
            dlcGraphProvider.getDlcGraph().add(new TripleImpl(enhanceTaskRef, RDF.type, DLC.EnhanceTask));
            // interlink task
            UriRef interlinkTaskRef = new UriRef(dataSetUri.getUnicodeString() + "/interlink");
            dlcGraphProvider.getDlcGraph().add(new TripleImpl(dataSetUri, DLC.creates, interlinkTaskRef));
            dlcGraphProvider.getDlcGraph().add(new TripleImpl(interlinkTaskRef, RDF.type, DLC.InterlinkTask));
            // smush task
            UriRef smushTaskRef = new UriRef(dataSetUri.getUnicodeString() + "/smush");
            dlcGraphProvider.getDlcGraph().add(new TripleImpl(dataSetUri, DLC.creates, smushTaskRef));
            dlcGraphProvider.getDlcGraph().add(new TripleImpl(smushTaskRef, RDF.type, DLC.SmushTask));
            // publish task
            UriRef publishTaskRef = new UriRef(dataSetUri.getUnicodeString() + "/publish");
            dlcGraphProvider.getDlcGraph().add(new TripleImpl(dataSetUri, DLC.creates, publishTaskRef));
            dlcGraphProvider.getDlcGraph().add(new TripleImpl(smushTaskRef, RDF.type, DLC.PublishTask));
            // create the source graph for the dataset (result of transformation in RDF)
            tcManager.createMGraph(getSourceGraphRef());
        //GraphNode dlcGraphNode = new GraphNode(DATA_LIFECYCLE_GRAPH_REFERENCE, getDlcGraph());
            //dlcGraphNode.addProperty(DCTERMS.hasPart, graphRef);
            dlcGraphProvider.getDlcGraph().add(new TripleImpl(rdfTaskRef, DLC.deliverable, getSourceGraphRef()));
            dlcGraphProvider.getDlcGraph().add(new TripleImpl(getSourceGraphRef(), RDF.type, DLC.Dataset));
            // create the graph to store text fields extract from properties in the source rdf
            tcManager.createMGraph(getDigestGraphRef());
            dlcGraphProvider.getDlcGraph().add(new TripleImpl(enhanceTaskRef, DLC.deliverable, getDigestGraphRef()));
            dlcGraphProvider.getDlcGraph().add(new TripleImpl(getDigestGraphRef(), RDFS.label, new PlainLiteralImpl("Contains a sioc:content property with text " + "for indexing and references to entities found in the text by NLP enhancement engines")));
            // create the graph to store enhancements found by NLP engines in the digest
            tcManager.createMGraph(getEnhanceGraphRef());
            dlcGraphProvider.getDlcGraph().add(new TripleImpl(enhanceTaskRef, DLC.deliverable, getEnhanceGraphRef()));
            dlcGraphProvider.getDlcGraph().add(new TripleImpl(getEnhanceGraphRef(), RDFS.label, new PlainLiteralImpl("Contains  entities found " + "in digest by NLP enhancement engines")));
            // create the graph to store the result of the interlinking task
            tcManager.createMGraph(getInterlinksGraphRef());
            dlcGraphProvider.getDlcGraph().add(new TripleImpl(interlinkTaskRef, DLC.deliverable, getInterlinksGraphRef()));
            dlcGraphProvider.getDlcGraph().add(new TripleImpl(getInterlinksGraphRef(), RDF.type, DLC.Linkset));
            dlcGraphProvider.getDlcGraph().add(new TripleImpl(getInterlinksGraphRef(), DLC.subjectsTarget, getSourceGraphRef()));
            dlcGraphProvider.getDlcGraph().add(new TripleImpl(getInterlinksGraphRef(), DLC.linkPredicate, OWL.sameAs));
            dlcGraphProvider.getDlcGraph().add(new TripleImpl(getInterlinksGraphRef(), RDFS.label, new PlainLiteralImpl("Contains equivalence links")));
            // create the graph to store the result of the smushing task
            tcManager.createMGraph(getSmushGraphRef());
            dlcGraphProvider.getDlcGraph().add(new TripleImpl(smushTaskRef, DLC.deliverable, getSmushGraphRef()));
            // create the graph to store the result of the publishing task
            tcManager.createMGraph(getPublishGraphRef());
            dlcGraphProvider.getDlcGraph().add(new TripleImpl(publishTaskRef, DLC.deliverable, getPublishGraphRef()));
            // set the initial dataset status as unpublished
            UriRef statusRef = new UriRef(dataSetUri.getUnicodeString() + "/Status");
            dlcGraphProvider.getDlcGraph().add(new TripleImpl(dataSetUri, DLC.status, statusRef));
            dlcGraphProvider.getDlcGraph().add(new TripleImpl(statusRef, RDF.type, DLC.Unpublished));
            dlcGraphProvider.getDlcGraph().add(new TripleImpl(statusRef, RDFS.label, new PlainLiteralImpl("Unpublished")));
        }
    }
}
