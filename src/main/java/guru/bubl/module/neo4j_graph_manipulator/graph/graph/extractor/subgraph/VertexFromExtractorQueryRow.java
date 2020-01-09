/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.subgraph;

import guru.bubl.module.model.graph.ShareLevel;
import guru.bubl.module.model.graph.vertex.VertexInSubGraph;
import guru.bubl.module.model.graph.vertex.VertexInSubGraphPojo;
import guru.bubl.module.model.json.SuggestionJson;
import guru.bubl.module.model.suggestion.SuggestionPojo;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.vertex.VertexInSubGraphOperatorNeo4j;
import org.neo4j.driver.v1.Record;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VertexFromExtractorQueryRow {

    private Record row;

    private String keyPrefix;

    public VertexFromExtractorQueryRow(
            Record row,
            String keyPrefix
    ) {
        this.row = row;
        this.keyPrefix = keyPrefix;
    }

    public VertexInSubGraph build(ShareLevel shareLevel) {
        VertexInSubGraphPojo vertexInSubGraphPojo = new VertexInSubGraphPojo(
                GraphElementFromExtractorQueryRow.usingRowAndKey(
                        row,
                        keyPrefix
                ).build(),
                getNumberOfConnectedEdges(),
                getNbPublicNeighbors(),
                getNbFriendNeighbors(),
                null,
                null,
                getSuggestions(),
                shareLevel
        );
        vertexInSubGraphPojo.getGraphElement().setChildrenIndex(
                getChildrenIndexes(keyPrefix, row)
        );
        vertexInSubGraphPojo.getGraphElement().setColors(
                getColors(keyPrefix, row)
        );
        vertexInSubGraphPojo.getGraphElement().setFont(
                getFont(keyPrefix, row)
        );
        if (this.isPattern()) {
            vertexInSubGraphPojo.getVertex().setAsPattern();
        }
        return vertexInSubGraphPojo;
    }

    private Integer getNumberOfConnectedEdges() {
        return row.get(
                keyPrefix + "." + VertexInSubGraphOperatorNeo4j.props.number_of_connected_edges_property_name
        ).asInt();

    }

    private Integer getNbPublicNeighbors() {
        return row.get(
                keyPrefix + "." + VertexInSubGraphOperatorNeo4j.props.nb_public_neighbors
        ).asInt();
    }

    private Integer getNbFriendNeighbors() {
        return row.get(
                keyPrefix + "." + VertexInSubGraphOperatorNeo4j.props.nb_friend_neighbors
        ).asInt();
    }

    private Map<URI, SuggestionPojo> getSuggestions() {
        Object suggestionValue = row.get(
                keyPrefix + "." + VertexInSubGraphOperatorNeo4j.props.suggestions
        ).asObject();
        if (suggestionValue == null) {
            return new HashMap<>();
        }
        return SuggestionJson.fromJsonArray(
                suggestionValue.toString()
        );
    }

    public static String getChildrenIndexes(String keyPrefix, Record row) {
        String key = keyPrefix + "." + "childrenIndexes";
        if (row.get(key) == null) {
            return null;
        }
        return row.get(
                key
        ).asString();
    }

    public static String getColors(String keyPrefix, Record row) {
        String key = keyPrefix + "." + "colors";
        if (row.get(key) == null) {
            return null;
        }
        return row.get(
                key
        ).asString();
    }

    public static String getFont(String keyPrefix, Record row) {
        String key = keyPrefix + "." + "font";
        if (row.get(key) == null) {
            return null;
        }
        return row.get(
                key
        ).asString();
    }

    private Boolean isPattern() {
        List<String> types = (List) row.get("type").asList();
        Boolean isPattern = false;
        for (String typeStr : types) {
            if (typeStr.equals("Pattern")) {
                isPattern = true;
            }
        }
        return isPattern;
    }
}
