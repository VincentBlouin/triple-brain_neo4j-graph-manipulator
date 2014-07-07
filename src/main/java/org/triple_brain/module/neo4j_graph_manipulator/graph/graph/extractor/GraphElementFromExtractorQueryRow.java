package org.triple_brain.module.neo4j_graph_manipulator.graph.graph.extractor;

import org.triple_brain.module.model.graph.FriendlyResourcePojo;
import org.triple_brain.module.model.graph.GraphElement;
import org.triple_brain.module.model.graph.GraphElementPojo;
import org.triple_brain.module.neo4j_graph_manipulator.graph.graph.Neo4jUserGraph;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/*
* Copyright Mozilla Public License 1.1
*/
public class GraphElementFromExtractorQueryRow {

    private Map<String, Object> row;
    private String key;
    private String genericIdentificationKey;
    private String typeKey;
    private String sameAsKey;

    public static GraphElementFromExtractorQueryRow usingRowAndKey(Map<String, Object> row, String key) {
        return new GraphElementFromExtractorQueryRow(
                row,
                key
        );
    }

    protected GraphElementFromExtractorQueryRow(Map<String, Object> row, String key) {
        this.row = row;
        this.key = key;
        this.genericIdentificationKey = key + "_generic_identification";
        this.typeKey = key + "_type";
        this.sameAsKey = key + "_same_as";
    }

    public GraphElementPojo build() {
        GraphElementPojo graphElement = init();
        update(graphElement);
        return graphElement;
    }

    public void update(GraphElement graphElement) {
        GraphElementPojo pojo = (GraphElementPojo) graphElement;
        updateIdentifications(
                pojo
        );
    }

    private void updateIdentifications(GraphElementPojo graphElement) {
        updateIdentificationsUsingKeyAndCollection(
                genericIdentificationKey,
                graphElement.getGenericIdentifications()
        );
        updateIdentificationsUsingKeyAndCollection(
                typeKey,
                graphElement.getAdditionalTypes()
        );
        updateIdentificationsUsingKeyAndCollection(
                sameAsKey,
                graphElement.getSameAs()
        );
    }


    private void updateIdentificationsUsingKeyAndCollection(
            String key,
            Map<URI, FriendlyResourcePojo> collection
    ) {
        if (hasIdentificationInRow(key)) {
            URI uri = URI.create(
                    uriKey(key)
            );
            FriendlyResourceFromExtractorQueryRow extractor = identificationExtractorUsingKey(
                    key
            );
            collection.put(
                    uri,
                    extractor.build()
            );
        }
    }

    private GraphElementPojo init() {
        return new GraphElementPojo(
                FriendlyResourceFromExtractorQueryRow.usingRowAndNodeKey(
                        row,
                        key
                ).build(),
                new HashMap<URI, FriendlyResourcePojo>(),
                new HashMap<URI, FriendlyResourcePojo>(),
                new HashMap<URI, FriendlyResourcePojo>()
        );
    }


    private Boolean hasIdentificationInRow(String key) {
        return row.get(
                uriKey(key)
        ) != null;
    }

    private FriendlyResourceFromExtractorQueryRow identificationExtractorUsingKey(String key) {
        return FriendlyResourceFromExtractorQueryRow.usingRowAndPrefix(
                row,
                key
        );
    }

    private String uriKey(String prefix) {
        return prefix + "." + Neo4jUserGraph.URI_PROPERTY_NAME;
    }
}
