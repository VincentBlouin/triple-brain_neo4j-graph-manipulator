package org.triple_brain.module.neo4j_graph_manipulator.graph.graph.extractor;

import org.triple_brain.module.model.graph.edge.EdgePojo;
import org.triple_brain.module.model.graph.vertex.VertexInSubGraph;
import org.triple_brain.module.model.graph.vertex.VertexInSubGraphPojo;
import org.triple_brain.module.model.suggestion.SuggestionPojo;
import org.triple_brain.module.neo4j_graph_manipulator.graph.graph.Neo4jUserGraph;
import org.triple_brain.module.neo4j_graph_manipulator.graph.graph.vertex.Neo4jVertexInSubGraphOperator;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/*
* Copyright Mozilla Public License 1.1
*/
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
                new HashMap<URI, SuggestionPojo>(),
                getIsPublic()
        );
    }

    public void update(VertexInSubGraphPojo vertex) {
        GraphElementFromExtractorQueryRow.usingRowAndKey(
                row,
                keyPrefix
        ).update(
                vertex.getGraphElement()
        );
        addOrUpdateSuggestionIfApplicable(
                vertex
        );
        updateIncludedVertices(vertex);
        updateIncludedEdges(vertex);
    }

    private void updateIncludedVertices(VertexInSubGraphPojo vertex){
        IncludedGraphElementFromExtractorQueryRow includedVertexExtractor = new IncludedGraphElementFromExtractorQueryRow(
                row,
                keyPrefix + "_included_vertex"
        );
        if(includedVertexExtractor.isInRow()){
            URI uri = includedVertexExtractor.getUri();
            if(!vertex.getIncludedVertices().containsKey(uri)){
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

    private void updateIncludedEdges(VertexInSubGraphPojo vertex){
        String key = keyPrefix + "_included_edge";
        IncludedGraphElementFromExtractorQueryRow includedEdgeExtractor = new IncludedGraphElementFromExtractorQueryRow(
                row,
                key
        );
        if(includedEdgeExtractor.isInRow()){
            URI uri = includedEdgeExtractor.getUri();
            if(!vertex.getIncludedEdges().containsKey(uri)){
                EdgePojo edge = new EdgePojo(
                        uri,
                        includedEdgeExtractor.getLabel()
                );
                IncludedGraphElementFromExtractorQueryRow sourceVertexExtractor = new IncludedGraphElementFromExtractorQueryRow(
                        row,
                        key + "_source_vertex"
                );
                edge.setSourceVertex(
                        new VertexInSubGraphPojo(
                                sourceVertexExtractor.getUri(),
                                sourceVertexExtractor.getLabel()
                        )
                );
                IncludedGraphElementFromExtractorQueryRow destinationVertexExtractor = new IncludedGraphElementFromExtractorQueryRow(
                        row,
                        key + "_destination_vertex"
                );
                edge.setDestinationVertex(
                        new VertexInSubGraphPojo(
                                destinationVertexExtractor.getUri(),
                                destinationVertexExtractor.getLabel()
                        )
                );
                vertex.getIncludedEdges().put(
                        uri,
                        edge
                );
            }
        }
    }

    public void addOrUpdateSuggestionIfApplicable(VertexInSubGraphPojo vertex) {
        if (!hasSuggestionInRow()) {
            return;
        }
        SuggestionExtractorQueryRow suggestionExtractorQueryRow = new SuggestionExtractorQueryRow(
                row,
                keyPrefix
        );
        URI suggestionUri = suggestionUriInRow();
        if (vertex.suggestions().containsKey(suggestionUri)) {
            suggestionExtractorQueryRow.update(
                    vertex.suggestions().get(
                            suggestionUri
                    )
            );
        } else {
            vertex.addSuggestion(
                    suggestionExtractorQueryRow.build()
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

    private Boolean hasSuggestionInRow() {
        return row.get(
                keyPrefix + "_suggestion.uri"
        ) != null;
    }


    private URI suggestionUriInRow() {
        return URI.create(
                row.get(
                        keyPrefix + "_suggestion." + Neo4jUserGraph.URI_PROPERTY_NAME
                ).toString());
    }
}
