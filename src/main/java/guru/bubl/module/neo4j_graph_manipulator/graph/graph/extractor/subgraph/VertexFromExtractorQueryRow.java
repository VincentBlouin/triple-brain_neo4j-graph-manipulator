/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.subgraph;

import guru.bubl.module.model.graph.edge.EdgePojo;
import guru.bubl.module.model.graph.vertex.VertexInSubGraph;
import guru.bubl.module.model.graph.vertex.VertexInSubGraphPojo;
import guru.bubl.module.model.json.SuggestionJson;
import guru.bubl.module.model.suggestion.SuggestionPojo;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.Neo4jGraphElementOperator;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.vertex.Neo4jVertexInSubGraphOperator;

import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VertexFromExtractorQueryRow {

    private ResultSet row;

    private String keyPrefix;

    public VertexFromExtractorQueryRow(
            ResultSet row,
            String keyPrefix
    ) {
        this.row = row;
        this.keyPrefix = keyPrefix;
    }

    public VertexInSubGraph build() throws SQLException {
        VertexInSubGraphPojo vertexInSubGraphPojo = new VertexInSubGraphPojo(
                GraphElementFromExtractorQueryRow.usingRowAndKey(
                        row,
                        keyPrefix
                ).build(),
                getNumberOfConnectedEdges(),
                getNbPublicNeighbors(),
                null,
                null,
                getSuggestions(),
                getIsPublic()
        );
        vertexInSubGraphPojo.getGraphElement().setChildrenIndex(
                getChildrenIndexes()
        );
        vertexInSubGraphPojo.getGraphElement().setColors(
                getColors()
        );
        return vertexInSubGraphPojo;
    }

    private Map<URI, VertexInSubGraphPojo> buildIncludedVertices() throws SQLException {
        IncludedGraphElementFromExtractorQueryRow includedVertexExtractor = new IncludedGraphElementFromExtractorQueryRow(
                row,
                Neo4jSubGraphExtractor.INCLUDED_VERTEX_QUERY_KEY
        );
        Map<URI, VertexInSubGraphPojo> includedVertices = new HashMap<>();
        if (!includedVertexExtractor.hasResult()) {
            return includedVertices;
        }

        for (List<String> properties : includedVertexExtractor.getList()) {
            if (properties.get(0) == null) {
                return includedVertices;
            }
            URI uri = URI.create(properties.get(0));
            includedVertices.put(
                    uri,
                    new VertexInSubGraphPojo(
                            uri,
                            properties.get(1)
                    )
            );
        }
        return includedVertices;
    }

    private Map<URI, EdgePojo> buildIncludedEdges() throws SQLException {
        IncludedGraphElementFromExtractorQueryRow includedEdgeExtractor = new IncludedGraphElementFromExtractorQueryRow(
                row,
                Neo4jSubGraphExtractor.INCLUDED_EDGE_QUERY_KEY
        );
        Map<URI, EdgePojo> includedEdges = new HashMap<>();
        if (!includedEdgeExtractor.hasResult()) {
            return includedEdges;
        }
        for (List<String> properties : includedEdgeExtractor.getList()) {
            if (properties.get(0) == null) {
                return includedEdges;
            }
            URI sourceVertexUri = URI.create(properties.get(0));
            URI destinationVertexUri = URI.create(properties.get(1));
            URI edgeUri = URI.create(properties.get(2));
            String label = properties.get(3);
            EdgePojo edge = new EdgePojo(
                    edgeUri,
                    label
            );
            edge.setSourceVertex(
                    new VertexInSubGraphPojo(
                            sourceVertexUri,
                            ""
                    )
            );
            edge.setDestinationVertex(
                    new VertexInSubGraphPojo(
                            destinationVertexUri,
                            ""
                    )
            );
            includedEdges.put(
                    edgeUri,
                    edge
            );
        }
        return includedEdges;
    }

    private Integer getNumberOfConnectedEdges() throws SQLException {
        return Integer.valueOf(
                row.getString(
                        keyPrefix + "." + Neo4jVertexInSubGraphOperator.props.number_of_connected_edges_property_name
                )
        );
    }

    private Integer getNbPublicNeighbors() throws SQLException {
        return Integer.valueOf(
                row.getString(
                        keyPrefix + "." + Neo4jVertexInSubGraphOperator.props.nb_public_neighbors
                )
        );
    }

    private Boolean getIsPublic() throws SQLException {
        return Boolean.valueOf(
                row.getString(
                        keyPrefix + "." + Neo4jVertexInSubGraphOperator.props.is_public
                )
        );
    }

    private Map<URI, SuggestionPojo> getSuggestions() throws SQLException {
        Object suggestionValue = row.getObject(
                keyPrefix + "." + Neo4jVertexInSubGraphOperator.props.suggestions
        );
        if (suggestionValue == null) {
            return new HashMap<>();
        }
        return SuggestionJson.fromJsonArray(
                suggestionValue.toString()
        );
    }

    private Long getSortDate() throws SQLException {
        String key = keyPrefix + "." + Neo4jGraphElementOperator.props.sort_date;
        if (row.getString(key) == null) {
            return null;
        }
        return row.getLong(
                key
        );
    }

    private Long getMoveDate() throws SQLException {
        String key = keyPrefix + "." + Neo4jGraphElementOperator.props.move_date;
        if (row.getString(key) == null) {
            return null;
        }
        return row.getLong(
                key
        );
    }

    private String getChildrenIndexes() throws SQLException {
        String key = keyPrefix + "." + "childrenIndexes";
        if (row.getString(key) == null) {
            return null;
        }
        return row.getString(
                key
        );
    }

    private String getColors() throws SQLException {
        String key = keyPrefix + "." + "colors";
        if (row.getString(key) == null) {
            return null;
        }
        return row.getString(
                key
        );
    }
}
