/*
 * Copyright Vincent Blouin under the Mozilla Public License 1.1
 */

package org.triple_brain.module.neo4j_graph_manipulator.graph.graph.extractor.subgraph;

import org.triple_brain.module.model.graph.edge.Edge;
import org.triple_brain.module.model.graph.edge.EdgePojo;
import org.triple_brain.module.model.graph.vertex.VertexInSubGraphPojo;
import org.triple_brain.module.neo4j_graph_manipulator.graph.graph.edge.Neo4jEdgeOperator;

import java.net.URI;
import java.util.Map;

public class EdgeFromExtractorQueryRow {

    private Map<String, Object> row;

    public static EdgeFromExtractorQueryRow usingRow(Map<String, Object> row) {
        return new EdgeFromExtractorQueryRow(row);
    }

    protected EdgeFromExtractorQueryRow(Map<String, Object> row) {
        this.row = row;
    }

    public Edge build() {
        EdgePojo edge = init();
        update(edge);
        return edge;
    }

    public Edge update(Edge edge) {
        GraphElementFromExtractorQueryRow.usingRowAndKey(row, "in_path_node").update(
                ((EdgePojo) edge).getGraphElement()
        );
        return edge;
    }

    private EdgePojo init() {
        return new EdgePojo(
                GraphElementFromExtractorQueryRow.usingRowAndKey(
                        row, "in_path_node"
                ).build(),
                new VertexInSubGraphPojo(getSourceVertexUri()),
                new VertexInSubGraphPojo(getDestinationVertexUri())
        );
    }

    private URI getSourceVertexUri() {
        return vertexUriFromProp(
                Neo4jEdgeOperator.props.source_vertex_uri
        );
    }

    private URI getDestinationVertexUri() {
        return vertexUriFromProp(
                Neo4jEdgeOperator.props.destination_vertex_uri
        );
    }

    private URI vertexUriFromProp(Enum prop) {
        return URI.create(
                row.get(
                        "in_path_node." +
                                prop

                ).toString()
        );
    }
}
