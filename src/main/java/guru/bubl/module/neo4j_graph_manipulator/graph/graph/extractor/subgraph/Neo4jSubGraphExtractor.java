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
    public final static String
            INCLUDED_VERTEX_QUERY_KEY = "iv",
            INCLUDED_EDGE_QUERY_KEY = "ie",
            GRAPH_ELEMENT_QUERY_KEY = "ge";

    QueryEngine engine;
    URI centerVertexUri;
    Integer depth;
    Neo4jVertexFactory vertexFactory;
    private SubGraphPojo subGraph = SubGraphPojo.withVerticesAndEdges(
            new HashMap<>(),
            new HashMap<>()
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
                addVertexUsingRow(
                        row
                );
            } else {
                addEdgeUsingRow(
                        row
                );
            }
        }
        return subGraph;
    }

    private Boolean isVertexFromRow(Map<String, Object> row) {
        return row.get("type").toString().contains("vertex");
    }

    private VertexInSubGraph addVertexUsingRow(Map<String, Object> row) {
        VertexInSubGraph vertex = new VertexFromExtractorQueryRow(
                row,
                Neo4jSubGraphExtractor.GRAPH_ELEMENT_QUERY_KEY
        ).build();
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
                "]->" + Neo4jSubGraphExtractor.GRAPH_ELEMENT_QUERY_KEY + " " +
                "OPTIONAL MATCH (" + GRAPH_ELEMENT_QUERY_KEY + ")-[:HAS_INCLUDED_VERTEX]->(" + INCLUDED_VERTEX_QUERY_KEY + ") " +
                "OPTIONAL MATCH(" + GRAPH_ELEMENT_QUERY_KEY + ")-[:HAS_INCLUDED_EDGE]->(" + INCLUDED_EDGE_QUERY_KEY + ") " +
                "OPTIONAL MATCH ("+GRAPH_ELEMENT_QUERY_KEY+")-[" + IdentificationQueryBuilder.IDENTIFICATION_RELATION_QUERY_KEY + ":" + Relationships.IDENTIFIED_TO + "]->(" + IdentificationQueryBuilder.IDENTIFICATION_QUERY_KEY + ") " +
                "RETURN " +
                vertexAndEdgeCommonQueryPart(GRAPH_ELEMENT_QUERY_KEY) +
                vertexReturnQueryPart(GRAPH_ELEMENT_QUERY_KEY) +
                edgeReturnQueryPart(GRAPH_ELEMENT_QUERY_KEY) +
                IdentificationQueryBuilder.identificationReturnQueryPart() +
                Neo4jSubGraphExtractor.GRAPH_ELEMENT_QUERY_KEY + ".type as type";
    }

    private String vertexAndEdgeCommonQueryPart(String prefix) {
        return FriendlyResourceQueryBuilder.returnQueryPartUsingPrefix(prefix);
    }

    private String edgeReturnQueryPart(String prefix) {
        return edgeSpecificPropertiesQueryPartUsingPrefix(prefix);
    }

    private String vertexReturnQueryPart(String prefix) {
        return vertexSpecificPropertiesQueryPartUsingPrefix(prefix) +
                includedVertexQueryPart(INCLUDED_VERTEX_QUERY_KEY) +
                includedEdgeQueryPart(INCLUDED_EDGE_QUERY_KEY) +
                FriendlyResourceQueryBuilder.imageReturnQueryPart(prefix);
    }

    private static String includedVertexQueryPart(String key) {
        return "COLLECT([" +
                QueryUtils.getPropertyUsingContainerNameQueryPart(
                        key,
                        Neo4jUserGraph.URI_PROPERTY_NAME
                ) +
                QueryUtils.getLastPropertyUsingContainerNameQueryPart(
                        key,
                        Neo4jFriendlyResource.props.label.toString()
                ) +
                "]) as " + key + ", ";
    }

    private static String includedEdgeQueryPart(String key) {
        return "COLLECT([" +
                edgeSpecificPropertiesQueryPartUsingPrefix(key) +
                QueryUtils.getPropertyUsingContainerNameQueryPart(
                        key,
                        Neo4jUserGraph.URI_PROPERTY_NAME
                ) +
                QueryUtils.getLastPropertyUsingContainerNameQueryPart(
                        key,
                        Neo4jFriendlyResource.props.label.toString()
                ) +
                "]) as " + key + ", ";
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

    private Edge addEdgeUsingRow(Map<String, Object> row) {
        EdgePojo edge = (EdgePojo) EdgeFromExtractorQueryRow.usingRow(
                row
        ).build();
        subGraph.addEdge(edge);
        return edge;
    }
}
