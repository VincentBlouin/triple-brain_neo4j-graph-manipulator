/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.subgraph;

import guru.bubl.module.model.graph.edge.Edge;
import guru.bubl.module.model.graph.edge.EdgePojo;
import guru.bubl.module.model.graph.vertex.VertexInSubGraphPojo;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.edge.EdgeOperatorNeo4j;
import org.neo4j.driver.v1.Record;

import java.net.URI;

public class EdgeFromExtractorQueryRow {

    private Record row;
    private String key;

    public static EdgeFromExtractorQueryRow usingRowAndKey(Record row, String key) {
        return new EdgeFromExtractorQueryRow(
                row,
                key
        );
    }

    public static EdgeFromExtractorQueryRow usingRow(Record row) {
        return new EdgeFromExtractorQueryRow(row);
    }

    protected EdgeFromExtractorQueryRow(Record row) {
        this(
                row,
                SubGraphExtractorNeo4j.GRAPH_ELEMENT_QUERY_KEY
        );
    }

    protected EdgeFromExtractorQueryRow(Record row, String key) {
        this.row = row;
        this.key = key;
    }

    public Edge build() {
        return init();
    }

    private EdgePojo init() {
        EdgePojo edge = new EdgePojo(
                GraphElementFromExtractorQueryRow.usingRowAndKey(
                        row, key
                ).build(),
                new VertexInSubGraphPojo(getSourceVertexUri()),
                new VertexInSubGraphPojo(getDestinationVertexUri())
        );
        return edge;
    }

    public URI getSourceVertexUri() {
        return vertexUriFromProp(
                EdgeOperatorNeo4j.props.source_vertex_uri
        );
    }

    public URI getDestinationVertexUri() {
        return vertexUriFromProp(
                EdgeOperatorNeo4j.props.destination_vertex_uri
        );
    }

    private URI vertexUriFromProp(Enum prop) {
        return URI.create(
                row.get(
                        key + "." + prop

                ).asString()
        );
    }
}
