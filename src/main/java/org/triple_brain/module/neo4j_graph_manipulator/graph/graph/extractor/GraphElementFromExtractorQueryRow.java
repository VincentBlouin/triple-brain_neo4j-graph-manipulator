package org.triple_brain.module.neo4j_graph_manipulator.graph.graph.extractor;

import org.triple_brain.module.model.graph.GraphElement;
import org.triple_brain.module.model.graph.GraphElementPojo;
import org.triple_brain.module.model.graph.IdentificationPojo;
import org.triple_brain.module.neo4j_graph_manipulator.graph.Neo4jFriendlyResource;

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
            Map<URI, IdentificationPojo> collection
    ) {
        if (hasIdentificationInRow(key)) {
            URI uri = URI.create(
                    row.get(uriKey(key)).toString()
            );
            IdentificationFromExtractorQueryRow extractor = IdentificationFromExtractorQueryRow.usingRowAndKey(
                    row,
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
                new HashMap<URI, IdentificationPojo>(),
                new HashMap<URI, IdentificationPojo>(),
                new HashMap<URI, IdentificationPojo>()
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
        return prefix + "." + Neo4jFriendlyResource.props.uri;
    }
}
