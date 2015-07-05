/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.subgraph;

import guru.bubl.module.model.graph.edge.Edge;
import guru.bubl.module.model.graph.edge.EdgePojo;
import guru.bubl.module.model.graph.vertex.VertexInSubGraphPojo;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.edge.Neo4jEdgeOperator;

import java.net.URI;
import java.util.Map;

public class EdgeFromExtractorQueryRow {

    private Map<String, Object> row;
    private String key;
    public static EdgeFromExtractorQueryRow usingRowAndKey(Map<String, Object> row, String key) {
        return new EdgeFromExtractorQueryRow(row, key);
    }

    public static EdgeFromExtractorQueryRow usingRow(Map<String, Object> row) {
        return new EdgeFromExtractorQueryRow(row);
    }

    protected EdgeFromExtractorQueryRow(Map<String, Object> row) {
        this(
                row,
                Neo4jSubGraphExtractor.GRAPH_ELEMENT_QUERY_KEY
        );
    }

    protected EdgeFromExtractorQueryRow(Map<String, Object> row, String key) {
        this.row = row;
        this.key = key;
    }

    public Edge build() {
        EdgePojo edge = init();
        return edge;
    }

    private EdgePojo init() {
        return new EdgePojo(
                GraphElementFromExtractorQueryRow.usingRowAndKey(
                        row, key
                ).build(),
                new VertexInSubGraphPojo(getSourceVertexUri()),
                new VertexInSubGraphPojo(getDestinationVertexUri())
        );
    }

    public URI getSourceVertexUri() {
        return vertexUriFromProp(
                Neo4jEdgeOperator.props.source_vertex_uri
        );
    }

    public URI getDestinationVertexUri() {
        return vertexUriFromProp(
                Neo4jEdgeOperator.props.destination_vertex_uri
        );
    }

    private URI vertexUriFromProp(Enum prop) {
        return URI.create(
                row.get(
                        key + "." + prop

                ).toString()
        );
    }
}
