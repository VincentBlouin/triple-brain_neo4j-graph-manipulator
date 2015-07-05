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
import java.util.List;
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
        return new VertexInSubGraphPojo(
                GraphElementFromExtractorQueryRow.usingRowAndKey(
                        row,
                        keyPrefix
                ).build(),
                getNumberOfConnectedEdges(),
                buildIncludedVertices(),
                buildIncludedEdges(),
                getSuggestions(),
                getIsPublic()
        );
    }

    private Map<URI, VertexInSubGraphPojo> buildIncludedVertices() {
        IncludedGraphElementFromExtractorQueryRow includedVertexExtractor = new IncludedGraphElementFromExtractorQueryRow(
                row,
                Neo4jSubGraphExtractor.INCLUDED_VERTEX_QUERY_KEY
        );
        Map<URI, VertexInSubGraphPojo> includedVertices = new HashMap<>();
        if(!includedVertexExtractor.hasResult()){
            return includedVertices;
        }

        for(List<String> properties:includedVertexExtractor.getList()){
            if(properties.get(0) == null){
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

    private Map<URI, EdgePojo> buildIncludedEdges() {
        IncludedGraphElementFromExtractorQueryRow includedEdgeExtractor = new IncludedGraphElementFromExtractorQueryRow(
                row,
                Neo4jSubGraphExtractor.INCLUDED_EDGE_QUERY_KEY
        );
        Map<URI, EdgePojo> includedEdges = new HashMap<>();
        if(!includedEdgeExtractor.hasResult()){
            return includedEdges;
        }
        for(List<String> properties:includedEdgeExtractor.getList()){
            if(properties.get(0) == null){
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
