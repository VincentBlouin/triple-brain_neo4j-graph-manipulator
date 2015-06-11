/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.subgraph;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.edge.Neo4jEdgeOperator;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.FriendlyResourceQueryBuilder;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.IdentificationQueryBuilder;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.vertex.Neo4jVertexInSubGraphOperator;
import org.neo4j.rest.graphdb.query.QueryEngine;
import org.neo4j.rest.graphdb.util.QueryResult;
import guru.bubl.module.model.graph.SubGraphPojo;
import guru.bubl.module.model.graph.edge.Edge;
import guru.bubl.module.model.graph.edge.EdgePojo;
import guru.bubl.module.model.graph.vertex.VertexInSubGraph;
import guru.bubl.module.model.graph.vertex.VertexInSubGraphPojo;
import guru.bubl.module.neo4j_graph_manipulator.graph.Neo4jFriendlyResource;
import guru.bubl.module.neo4j_graph_manipulator.graph.Relationships;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.Neo4jUserGraph;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.QueryUtils;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.vertex.Neo4jVertexFactory;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.neo4j.helpers.collection.MapUtil.map;

public class Neo4jSubGraphExtractor {
    QueryEngine engine;
    URI centerVertexUri;
    Integer depth;
    Neo4jVertexFactory vertexFactory;
    private SubGraphPojo subGraph = SubGraphPojo.withVerticesAndEdges(
            new HashMap<URI, VertexInSubGraphPojo>(),
            new HashMap<URI, EdgePojo>()
    );

    @AssistedInject
    protected Neo4jSubGraphExtractor(
            QueryEngine engine,
            Neo4jVertexFactory vertexFactory,
            @Assisted URI centerVertexUri,
            @Assisted Integer depth
    ) {
        this.engine = engine;
        this.vertexFactory = vertexFactory;
        this.centerVertexUri = centerVertexUri;
        this.depth = depth;
    }

    public SubGraphPojo load() {
        QueryResult<Map<String, Object>> result = engine.query(
                queryToGetGraph(),
                map()
        );
        for (Map<String, Object> row : result) {
            if (isVertexFromRow(row)) {
                addOrUpdateVertexUsingRow(
                        row
                );
            } else {
                addOrUpdateEdgeUsingRow(
                        row
                );
            }
        }
        return subGraph;
    }

    private Boolean isVertexFromRow(Map<String, Object> row) {
        return row.get("type").toString().contains("vertex");
    }

    private VertexInSubGraph addOrUpdateVertexUsingRow(Map<String, Object> row) {
        URI uri = URI.create(
                row.get("in_path_node.uri").toString()
        );
        VertexInSubGraph vertex;
        if (subGraph.vertices().containsKey(uri)) {
            vertex = subGraph.vertexWithIdentifier(uri);
            new VertexFromExtractorQueryRow(row, "in_path_node").update(
                    (VertexInSubGraphPojo) vertex
            );
            return vertex;
        }
        vertex = new VertexFromExtractorQueryRow(row, "in_path_node").build();

        subGraph.addVertex(
                (VertexInSubGraphPojo) vertex
        );
        return vertex;
    }

    private String queryToGetGraph() {
        return "START start_node=node:node_auto_index('uri:" + centerVertexUri + "') " +
                "MATCH path=start_node<-[:" +
                Relationships.SOURCE_VERTEX +
                "|" + Relationships.DESTINATION_VERTEX + "*0.." + depth * 2 +
                "]->in_path_node " +
                "OPTIONAL MATCH (in_path_node)-[:HAS_INCLUDED_VERTEX]->(in_path_node_included_vertex) " +
                "OPTIONAL MATCH (in_path_node)-[:HAS_INCLUDED_EDGE]->(in_path_node_included_edge) " +
                "RETURN " +
                vertexAndEdgeCommonQueryPart("in_path_node") +
                vertexReturnQueryPart("in_path_node") +
                edgeReturnQueryPart("in_path_node") +
                "in_path_node.type as type";
    }

    private String vertexAndEdgeCommonQueryPart(String prefix){
        return FriendlyResourceQueryBuilder.returnQueryPartUsingPrefix(prefix) +
                IdentificationQueryBuilder.identificationReturnQueryPart(prefix);
    }

    private String edgeReturnQueryPart(String prefix) {
        return edgeSpecificPropertiesQueryPartUsingPrefix(prefix);
    }

    private String vertexReturnQueryPart(String prefix) {
        return vertexSpecificPropertiesQueryPartUsingPrefix(prefix) +
                includedElementQueryPart(prefix + "_included_vertex") +
                includedEdgeQueryPart(prefix) +
                FriendlyResourceQueryBuilder.imageReturnQueryPart(prefix);
    }

    private static String includedEdgeQueryPart(String prefix) {
        String key = prefix + "_included_edge";
        return edgeSpecificPropertiesQueryPartUsingPrefix(key) +
                includedElementQueryPart(
                        key
                );
    }

    public static String includedElementQueryPart(String key) {
        return QueryUtils.getPropertyUsingContainerNameQueryPart(
                key,
                Neo4jUserGraph.URI_PROPERTY_NAME
        ) + QueryUtils.getPropertyUsingContainerNameQueryPart(
                key,
                Neo4jFriendlyResource.props.label.toString()
        );
    }

    public static String edgeSpecificPropertiesQueryPartUsingPrefix(String prefix) {
        return QueryUtils.getPropertyUsingContainerNameQueryPart(
                prefix,
                Neo4jEdgeOperator.props.source_vertex_uri.toString()
        ) +
                QueryUtils.getPropertyUsingContainerNameQueryPart(
                        prefix,
                        Neo4jEdgeOperator.props.destination_vertex_uri.toString()
                );
    }

    private String vertexSpecificPropertiesQueryPartUsingPrefix(String prefix) {
        return QueryUtils.getPropertyUsingContainerNameQueryPart(
                prefix,
                Neo4jVertexInSubGraphOperator.props.number_of_connected_edges_property_name.toString()
        ) +
                QueryUtils.getPropertyUsingContainerNameQueryPart(
                        prefix,
                        Neo4jVertexInSubGraphOperator.props.is_public.name()
                ) +
                QueryUtils.getPropertyUsingContainerNameQueryPart(
                        prefix,
                        Neo4jVertexInSubGraphOperator.props.suggestions.name()
                );
    }

    private Edge addOrUpdateEdgeUsingRow(Map<String, Object> row) {
        EdgePojo edge = (EdgePojo) EdgeFromExtractorQueryRow.usingRow(
                row
        ).build();
        subGraph.addEdge(edge);
        return edge;
    }
}
