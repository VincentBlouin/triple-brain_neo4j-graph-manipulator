/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.subgraph;

import guru.bubl.module.model.graph.edge.Edge;
import guru.bubl.module.model.graph.edge.EdgePojo;
import guru.bubl.module.model.graph.vertex.VertexInSubGraphPojo;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.edge.Neo4jEdgeOperator;

import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;

public class EdgeFromExtractorQueryRow {

    private ResultSet row;
    private String key;

    public static EdgeFromExtractorQueryRow usingRowAndKey(ResultSet row, String key) {
        return new EdgeFromExtractorQueryRow(
                row,
                key
        );
    }

    public static EdgeFromExtractorQueryRow usingRow(ResultSet row) {
        return new EdgeFromExtractorQueryRow(row);
    }

    protected EdgeFromExtractorQueryRow(ResultSet row) {
        this(
                row,
                Neo4jSubGraphExtractor.GRAPH_ELEMENT_QUERY_KEY
        );
    }

    protected EdgeFromExtractorQueryRow(ResultSet row, String key) {
        this.row = row;
        this.key = key;
    }

    public Edge build() throws SQLException {
        return init();
    }

    private EdgePojo init() throws SQLException {
        EdgePojo edge = new EdgePojo(
                GraphElementFromExtractorQueryRow.usingRowAndKey(
                        row, key
                ).build(),
                new VertexInSubGraphPojo(getSourceVertexUri()),
                new VertexInSubGraphPojo(getDestinationVertexUri())
        );
        return edge;
    }

    public URI getSourceVertexUri() throws SQLException{
        return vertexUriFromProp(
                Neo4jEdgeOperator.props.source_vertex_uri
        );
    }

    public URI getDestinationVertexUri() throws SQLException{
        return vertexUriFromProp(
                Neo4jEdgeOperator.props.destination_vertex_uri
        );
    }

    private URI vertexUriFromProp(Enum prop) throws SQLException{
        return URI.create(
                row.getString(
                        key + "." + prop

                )
        );
    }
}
