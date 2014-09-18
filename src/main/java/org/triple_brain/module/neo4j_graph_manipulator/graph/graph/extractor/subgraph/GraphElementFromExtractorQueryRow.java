/*
 * Copyright Vincent Blouin under the Mozilla Public License 1.1
 */

package org.triple_brain.module.neo4j_graph_manipulator.graph.graph.extractor.subgraph;

import org.triple_brain.module.model.graph.GraphElement;
import org.triple_brain.module.model.graph.GraphElementPojo;
import org.triple_brain.module.model.graph.IdentificationPojo;
import org.triple_brain.module.neo4j_graph_manipulator.graph.Neo4jFriendlyResource;
import org.triple_brain.module.neo4j_graph_manipulator.graph.Relationships;
import org.triple_brain.module.neo4j_graph_manipulator.graph.graph.extractor.FriendlyResourceFromExtractorQueryRow;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class GraphElementFromExtractorQueryRow {

    private Map<String, Object> row;
    private String key;
    private String identificationKey;
    private String identificationTypeKey;

    public static GraphElementFromExtractorQueryRow usingRowAndKey(Map<String, Object> row, String key) {
        return new GraphElementFromExtractorQueryRow(
                row,
                key
        );
    }

    protected GraphElementFromExtractorQueryRow(Map<String, Object> row, String key) {
        this.row = row;
        this.key = key;
        this.identificationKey = key + "_identification";
        this.identificationTypeKey = key + "_identification_type";
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
        if (hasIdentificationInRow()) {
            URI uri = URI.create(
                    row.get(uriKey(identificationKey)).toString()
            );
            IdentificationFromExtractorQueryRow extractor = IdentificationFromExtractorQueryRow.usingRowAndKey(
                    row,
                    identificationKey
            );
            getCorrectIdentificationCollection(graphElement).put(
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


    private Boolean hasIdentificationInRow() {
        return row.get(
                uriKey(identificationKey)
        ) != null;
    }

    private String uriKey(String prefix) {
        return prefix + "." + Neo4jFriendlyResource.props.uri;
    }

    private Map<URI, IdentificationPojo> getCorrectIdentificationCollection(GraphElementPojo graphElement){
        Relationships identificationType = Relationships.valueOf(getIdentificationType());
        if(Relationships.TYPE == identificationType) {
            return graphElement.getAdditionalTypes();
        }else if(Relationships.SAME_AS == identificationType){
            return graphElement.getSameAs();
        }
        else {
            return graphElement.getGenericIdentifications();
        }
    }

    private String getIdentificationType(){
        return row.get(
                identificationTypeKey
        ).toString();
    }
}
