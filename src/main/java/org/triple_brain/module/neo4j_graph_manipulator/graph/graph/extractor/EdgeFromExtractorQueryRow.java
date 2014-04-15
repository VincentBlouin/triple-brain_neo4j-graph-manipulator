package org.triple_brain.module.neo4j_graph_manipulator.graph.graph.extractor;

/*
* Copyright Mozilla Public License 1.1
*/

import org.triple_brain.module.model.graph.FriendlyResourcePojo;
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

    public Edge build(Map<URI, VertexInSubGraphPojo> vertices) {
        EdgePojo edge = init(vertices);
        update(edge);
        return edge;
    }

    public Edge update(Edge edge) {
        GraphElementFromExtractorQueryRow.usingRowAndKey(row, "in_path_node").update(
                ((EdgePojo) edge).getGraphElement()
        );
        updateSourceVertexIfApplicable(edge);
        updateDestinationVertexIfApplicable(edge);
        return edge;
    }

    private EdgePojo init(Map<URI, VertexInSubGraphPojo> vertices) {
        URI sourceVertexUri = getSourceVertexUri();
        URI destinationVertexUri = getDestinationVertexUri();
        VertexInSubGraphPojo sourceVertex = vertices.containsKey(sourceVertexUri) ?
                vertices.get(sourceVertexUri) :
                new VertexInSubGraphPojo(sourceVertexUri);
        VertexInSubGraphPojo destinationVertex = vertices.containsKey(destinationVertexUri) ?
                vertices.get(destinationVertexUri) :
                new VertexInSubGraphPojo(destinationVertexUri);
        return new EdgePojo(
                GraphElementFromExtractorQueryRow.usingRowAndKey(
                        row, "in_path_node"
                ).build(),
                sourceVertex,
                destinationVertex
        );
    }
    private URI getSourceVertexUri(){
        return vertexUriFromProp(
                Neo4jEdgeOperator.props.source_vertex_uri
        );
    }

    private URI getDestinationVertexUri(){
        return vertexUriFromProp(
                Neo4jEdgeOperator.props.destination_vertex_uri
        );
    }

    private URI vertexUriFromProp(Enum prop){
        return URI.create(
                row.get(
                        "in_path_node." +
                                prop

                ).toString()
        );
    }

    private void updateSourceVertexIfApplicable(Edge edge) {
        new VertexFromExtractorQueryRow(
                row,
                "source_vertex"
        ).update(
                (VertexInSubGraphPojo) edge.sourceVertex()
        );
    }

    private void updateDestinationVertexIfApplicable(Edge edge) {
        new VertexFromExtractorQueryRow(
                row,
                "destination_vertex"
        ).update(
                (VertexInSubGraphPojo) edge.destinationVertex()
        );
    }

}
