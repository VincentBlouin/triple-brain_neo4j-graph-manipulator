/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.subgraph;

import guru.bubl.module.model.graph.edge.EdgePojo;
import guru.bubl.module.model.graph.vertex.VertexInSubGraph;
import guru.bubl.module.model.graph.vertex.VertexInSubGraphPojo;
import guru.bubl.module.model.json.SuggestionJson;
import guru.bubl.module.model.suggestion.SuggestionPojo;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.vertex.Neo4jVertexInSubGraphOperator;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class VertexFromExtractorQueryRow {

    private Map<String, Object> row;

    private String keyPrefix;

    public VertexFromExtractorQueryRow(
            Map<String, Object> row,
            String keyPrefix
    ) {
        this.row = row;
        this.keyPrefix = keyPrefix;
    }

    public VertexInSubGraph build() {
        VertexInSubGraphPojo vertex = init();
        update(vertex);
        return vertex;
    }

    private VertexInSubGraphPojo init() {
        return new VertexInSubGraphPojo(
                GraphElementFromExtractorQueryRow.usingRowAndKey(
                        row,
                        keyPrefix
                ).build(),
                getNumberOfConnectedEdges(),
                new HashMap<URI, VertexInSubGraphPojo>(),
                new HashMap<URI, EdgePojo>(),
                getSuggestions(),
                getIsPublic()
        );
    }

    public void update(VertexInSubGraphPojo vertex) {
        updateIncludedVertices(vertex);
        updateIncludedEdges(vertex);
    }

    private void updateIncludedVertices(VertexInSubGraphPojo vertex) {
        IncludedGraphElementFromExtractorQueryRow includedVertexExtractor = new IncludedGraphElementFromExtractorQueryRow(
                row,
                keyPrefix + "_included_vertex"
        );
        if (includedVertexExtractor.isInRow()) {
            URI uri = includedVertexExtractor.getUri();
            if (!vertex.getIncludedVertices().containsKey(uri)) {
                vertex.getIncludedVertices().put(
                        uri,
                        new VertexInSubGraphPojo(
                                uri,
                                includedVertexExtractor.getLabel()
                        )
                );
            }
        }
    }

    private void updateIncludedEdges(VertexInSubGraphPojo vertex) {
        String key = keyPrefix + "_included_edge";
        IncludedGraphElementFromExtractorQueryRow includedEdgeExtractor = new IncludedGraphElementFromExtractorQueryRow(
                row,
                key
        );
        if (includedEdgeExtractor.isInRow()) {
            URI uri = includedEdgeExtractor.getUri();
            if (vertex.getIncludedEdges().containsKey(uri)) {
                return;
            }
            EdgePojo edge = new EdgePojo(
                    uri,
                    includedEdgeExtractor.getLabel()
            );
            EdgeFromExtractorQueryRow edgeExtractor = EdgeFromExtractorQueryRow.usingRowAndKey(
                    row,
                    key
            );
            edge.setSourceVertex(
                    new VertexInSubGraphPojo(
                            edgeExtractor.getSourceVertexUri(),
                            ""
                    )
            );

            edge.setDestinationVertex(
                    new VertexInSubGraphPojo(
                            edgeExtractor.getDestinationVertexUri(),
                            ""
                    )
            );
            vertex.getIncludedEdges().put(
                    uri,
                    edge
            );
        }
    }

    private Integer getNumberOfConnectedEdges() {
        return Integer.valueOf(
                row.get(
                        keyPrefix + "." + Neo4jVertexInSubGraphOperator.props.number_of_connected_edges_property_name
                ).toString()
        );
    }

    private Boolean getIsPublic() {
        return Boolean.valueOf(
                row.get(
                        keyPrefix + "." + Neo4jVertexInSubGraphOperator.props.is_public
                ).toString()
        );
    }

    private Map<URI, SuggestionPojo> getSuggestions() {
        Object suggestionValue = row.get(
                keyPrefix + "." + Neo4jVertexInSubGraphOperator.props.suggestions
        );
        if (suggestionValue == null) {
            return new HashMap<>();
        }
        return SuggestionJson.fromJsonArray(
                suggestionValue.toString()
        );
    }
}
