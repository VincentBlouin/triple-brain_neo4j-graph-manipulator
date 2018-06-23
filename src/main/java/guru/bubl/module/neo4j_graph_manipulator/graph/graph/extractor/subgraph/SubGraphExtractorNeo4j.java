/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.subgraph;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import guru.bubl.module.common_utils.NoEx;
import guru.bubl.module.model.UserUris;
import guru.bubl.module.model.graph.GraphElementType;
import guru.bubl.module.model.graph.ShareLevel;
import guru.bubl.module.model.graph.subgraph.SubGraphPojo;
import guru.bubl.module.model.graph.edge.Edge;
import guru.bubl.module.model.graph.edge.EdgePojo;
import guru.bubl.module.model.graph.vertex.VertexInSubGraph;
import guru.bubl.module.model.graph.vertex.VertexInSubGraphPojo;
import guru.bubl.module.neo4j_graph_manipulator.graph.FriendlyResourceNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.Relationships;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.UserGraphNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.edge.EdgeOperatorNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.FriendlyResourceQueryBuilder;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.IdentificationQueryBuilder;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.QueryUtils;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.vertex.VertexInSubGraphOperatorNeo4j;

import java.net.URI;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class SubGraphExtractorNeo4j {
    public final static String
            INCLUDED_VERTEX_QUERY_KEY = "iv",
            INCLUDED_EDGE_QUERY_KEY = "ie",
            GRAPH_ELEMENT_QUERY_KEY = "ge";
    private URI centerBubbleUri;
    private Integer depth;
    private Integer resultsLimit;
    protected Connection connection;
    private SubGraphPojo subGraph = SubGraphPojo.withVerticesAndEdges(
            new HashMap<>(),
            new HashMap<>()
    );

    private Set<ShareLevel> inShareLevels = new HashSet<>();

    @AssistedInject
    protected SubGraphExtractorNeo4j(
            Connection connection,
            @Assisted URI centerBubbleUri,
            @Assisted("depth") Integer depth
    ) {
        this(
                connection,
                centerBubbleUri,
                depth,
                null
        );
    }

    @AssistedInject
    protected SubGraphExtractorNeo4j(
            Connection connection,
            @Assisted URI centerBubbleUri,
            @Assisted("depth") Integer depth,
            @Assisted("resultsLimit") Integer resultsLimit
    ) {
        this.connection = connection;
        this.centerBubbleUri = centerBubbleUri;
        this.depth = depth;
        this.resultsLimit = resultsLimit;
        inShareLevels.clear();
        inShareLevels.add(ShareLevel.PRIVATE);
    }

    @AssistedInject
    protected SubGraphExtractorNeo4j(
            Connection connection,
            @Assisted URI centerBubbleUri,
            @Assisted Set<ShareLevel> inShareLevels
    ) {
        this.connection = connection;
        this.centerBubbleUri = centerBubbleUri;
        this.depth = 1;
        this.inShareLevels = inShareLevels;
    }

    @AssistedInject
    protected SubGraphExtractorNeo4j(
            Connection connection,
            @Assisted URI centerBubbleUri,
            @Assisted Set<ShareLevel> inShareLevels,
            @Assisted Integer depth
    ) {
        this.connection = connection;
        this.centerBubbleUri = centerBubbleUri;
        this.depth = depth;
        this.inShareLevels = inShareLevels;
    }

    public SubGraphPojo load() {
        NoEx.wrap(() -> {
            ResultSet rs = connection.createStatement().executeQuery(
                    queryToGetGraph()
            );
            while (rs.next()) {
                switch (getGraphElementTypeFromRow(rs)) {
                    case vertex:
                        ShareLevel shareLevel = ShareLevel.get(rs.getInt("ge.shareLevel"));
                        if (!inShareLevels.contains(shareLevel)) {
                            break;
                        }
                        addVertexUsingRow(
                                rs,
                                shareLevel
                        );
                        break;
                    case edge:
                        addEdgeUsingRow(
                                rs
                        );
                        break;
                }
            }
            return rs;
        }).get();
        Iterator<EdgePojo> it = subGraph.edges().values().iterator();
        while (it.hasNext()) {
            EdgePojo edge = it.next();
            Boolean hasSourceVertex = subGraph.vertices().containsKey(
                    edge.sourceVertex().uri()
            );
            Boolean hasDestinationVertex = subGraph.vertices().containsKey(
                    edge.destinationVertex().uri()
            );
            if (!hasSourceVertex || !hasDestinationVertex) {
                it.remove();
            }
        }
        return subGraph;
    }

    private GraphElementType getGraphElementTypeFromRow(ResultSet rs) throws SQLException {
        return GraphElementType.valueOf(rs.getString(
                "type"
        ));
    }

    private VertexInSubGraph addVertexUsingRow(ResultSet row, ShareLevel shareLevel) throws SQLException {
        VertexInSubGraph vertex = new VertexFromExtractorQueryRow(
                row,
                SubGraphExtractorNeo4j.GRAPH_ELEMENT_QUERY_KEY
        ).build(shareLevel);
        subGraph.addVertex(
                (VertexInSubGraphPojo) vertex
        );
        return vertex;
    }

    private String queryToGetGraph() {
        return
                "START start_node=node:node_auto_index('uri:" + centerBubbleUri + "') " +
                        getMatchQueryPart() +
//                        "OPTIONAL MATCH (" + GRAPH_ELEMENT_QUERY_KEY + ")-[:HAS_INCLUDED_VERTEX]->(" + INCLUDED_VERTEX_QUERY_KEY + ") " +
//                        "OPTIONAL MATCH(" + GRAPH_ELEMENT_QUERY_KEY + ")-[:HAS_INCLUDED_EDGE]->(" + INCLUDED_EDGE_QUERY_KEY + ") " +
                        "OPTIONAL MATCH (" + GRAPH_ELEMENT_QUERY_KEY + ")-[" + IdentificationQueryBuilder.IDENTIFICATION_RELATION_QUERY_KEY + ":" + Relationships.IDENTIFIED_TO + "]->(" + IdentificationQueryBuilder.IDENTIFIER_QUERY_KEY + ") " +
                        "RETURN " +
                        vertexAndEdgeCommonQueryPart(GRAPH_ELEMENT_QUERY_KEY) +
                        vertexReturnQueryPart(GRAPH_ELEMENT_QUERY_KEY) +
                        edgeReturnQueryPart(GRAPH_ELEMENT_QUERY_KEY) +
                        IdentificationQueryBuilder.identificationReturnQueryPart() +
                        SubGraphExtractorNeo4j.GRAPH_ELEMENT_QUERY_KEY + ".type as type ";
    }

    private String getMatchQueryPart() {
        if (UserUris.isUriOfAnIdentifier(centerBubbleUri)) {
            return "MATCH start_node<-[:" +
                    Relationships.IDENTIFIED_TO +
                    "]-it " +
                    "MATCH it<-[:" +
                    Relationships.SOURCE_VERTEX + "|" +
                    Relationships.DESTINATION_VERTEX + "*0.." + depth +
                    "]->" + SubGraphExtractorNeo4j.GRAPH_ELEMENT_QUERY_KEY + " ";

        } else {
            return "MATCH path=start_node<-[:" +
                    Relationships.SOURCE_VERTEX +
                    "|" + Relationships.DESTINATION_VERTEX + "*0.." + depth * 2 +
                    "]->" + SubGraphExtractorNeo4j.GRAPH_ELEMENT_QUERY_KEY + " ";
        }
    }

    private String vertexAndEdgeCommonQueryPart(String prefix) {
        return FriendlyResourceQueryBuilder.returnQueryPartUsingPrefix(prefix);
    }

    private String edgeReturnQueryPart(String prefix) {
        return edgeSpecificPropertiesQueryPartUsingPrefix(prefix);
    }

    private String vertexReturnQueryPart(String prefix) {
        return vertexSpecificPropertiesQueryPartUsingPrefix(prefix) +
//                includedVertexQueryPart(INCLUDED_VERTEX_QUERY_KEY) +
//                includedEdgeQueryPart(INCLUDED_EDGE_QUERY_KEY) +
                FriendlyResourceQueryBuilder.imageReturnQueryPart(prefix);
    }

    private static String includedVertexQueryPart(String key) {
        return "COLLECT([" +
                QueryUtils.getPropertyUsingContainerNameQueryPart(
                        key,
                        UserGraphNeo4j.URI_PROPERTY_NAME
                ) +
                QueryUtils.getLastPropertyUsingContainerNameQueryPart(
                        key,
                        FriendlyResourceNeo4j.props.label.toString()
                ) +
                "]) as " + key + ", ";
    }

    private static String includedEdgeQueryPart(String key) {
        return "COLLECT([" +
                edgeSpecificPropertiesQueryPartUsingPrefix(key) +
                QueryUtils.getPropertyUsingContainerNameQueryPart(
                        key,
                        UserGraphNeo4j.URI_PROPERTY_NAME
                ) +
                QueryUtils.getLastPropertyUsingContainerNameQueryPart(
                        key,
                        FriendlyResourceNeo4j.props.label.toString()
                ) +
                "]) as " + key + ", ";
    }

    public static String includedElementQueryPart(String key) {
        return QueryUtils.getPropertyUsingContainerNameQueryPart(
                key,
                UserGraphNeo4j.URI_PROPERTY_NAME
        ) + QueryUtils.getPropertyUsingContainerNameQueryPart(
                key,
                FriendlyResourceNeo4j.props.label.toString()
        );
    }

    public static String edgeSpecificPropertiesQueryPartUsingPrefix(String prefix) {
        return QueryUtils.getPropertyUsingContainerNameQueryPart(
                prefix,
                EdgeOperatorNeo4j.props.source_vertex_uri.toString()
        ) +
                QueryUtils.getPropertyUsingContainerNameQueryPart(
                        prefix,
                        EdgeOperatorNeo4j.props.destination_vertex_uri.toString()
                );
    }

    private String vertexSpecificPropertiesQueryPartUsingPrefix(String prefix) {
        return QueryUtils.getPropertyUsingContainerNameQueryPart(
                prefix,
                VertexInSubGraphOperatorNeo4j.props.number_of_connected_edges_property_name.toString()
        ) +
                QueryUtils.getPropertyUsingContainerNameQueryPart(
                        prefix,
                        VertexInSubGraphOperatorNeo4j.props.nb_public_neighbors.name()
                ) +
                QueryUtils.getPropertyUsingContainerNameQueryPart(
                        prefix,
                        VertexInSubGraphOperatorNeo4j.props.nb_friend_neighbors.name()
                ) +
                QueryUtils.getPropertyUsingContainerNameQueryPart(
                        prefix,
                        VertexInSubGraphOperatorNeo4j.props.suggestions.name()
                ) +
                QueryUtils.getPropertyUsingContainerNameQueryPart(
                        prefix,
                        "childrenIndexes"
                ) +
                QueryUtils.getPropertyUsingContainerNameQueryPart(
                        prefix,
                        "colors"
                ) +
                QueryUtils.getPropertyUsingContainerNameQueryPart(
                        prefix,
                        "font"
                ) +
                QueryUtils.getPropertyUsingContainerNameQueryPart(
                        prefix,
                        "shareLevel"
                );
    }

    private Edge addEdgeUsingRow(ResultSet row) throws SQLException {
        EdgePojo edge = (EdgePojo) EdgeFromExtractorQueryRow.usingRow(
                row
        ).build();
        subGraph.addEdge(edge);
        return edge;
    }
}
