/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.subgraph;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import guru.bubl.module.model.UserUris;
import guru.bubl.module.model.graph.GraphElementType;
import guru.bubl.module.model.graph.ShareLevel;
import guru.bubl.module.model.graph.edge.Edge;
import guru.bubl.module.model.graph.edge.EdgePojo;
import guru.bubl.module.model.graph.subgraph.SubGraphPojo;
import guru.bubl.module.model.graph.vertex.Vertex;
import guru.bubl.module.model.graph.vertex.VertexInSubGraph;
import guru.bubl.module.model.graph.vertex.VertexInSubGraphPojo;
import guru.bubl.module.neo4j_graph_manipulator.graph.FriendlyResourceNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.Relationships;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.UserGraphNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.edge.EdgeOperatorNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.FriendlyResourceQueryBuilder;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.TagQueryBuilder;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.QueryUtils;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.vertex.VertexTypeOperatorNeo4j;
import org.neo4j.driver.internal.InternalRelationship;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import sun.security.provider.SHA;

import java.net.URI;
import java.util.*;

import static org.neo4j.driver.v1.Values.parameters;

public class SubGraphExtractorNeo4j {

    public final static String GRAPH_ELEMENT_QUERY_KEY = "ge";
    private URI centerBubbleUri;
    private Boolean isCenterTagFlow;
    private Integer depth;
    private SubGraphPojo subGraph = SubGraphPojo.withVerticesAndEdges(
            new HashMap<>(),
            new HashMap<>()
    );

    private Integer[] inShareLevelsArray;

    private Set<ShareLevel> inShareLevels;

    protected Driver driver;

    @AssistedInject
    protected SubGraphExtractorNeo4j(
            Driver driver,
            @Assisted URI centerBubbleUri,
            @Assisted Integer... inShareLevelsArray
    ) {
        this.driver = driver;
        this.centerBubbleUri = centerBubbleUri;
        this.depth = 1;
        this.inShareLevelsArray = inShareLevelsArray;
        inShareLevels = ShareLevel.arrayOfIntegersToSet(this.inShareLevelsArray);
        this.isCenterTagFlow = UserUris.isUriOfAnIdentifier(centerBubbleUri);
    }

    @AssistedInject
    protected SubGraphExtractorNeo4j(
            Driver driver,
            @Assisted URI centerBubbleUri,
            @Assisted Integer depth,
            @Assisted Integer... inShareLevelsArray
    ) {
        this.driver = driver;
        this.centerBubbleUri = centerBubbleUri;
        this.depth = depth;
        this.inShareLevelsArray = inShareLevelsArray;
        inShareLevels = ShareLevel.arrayOfIntegersToSet(this.inShareLevelsArray);
        this.isCenterTagFlow = UserUris.isUriOfAnIdentifier(centerBubbleUri);
    }

    public SubGraphPojo load() {
        try (Session session = driver.session()) {
            StatementResult rs = session.run(
                    queryToGetGraph(),
                    parameters(
                            "centerUri", centerBubbleUri.toString(),
                            "shareLevels", inShareLevelsArray
                    )
            );
            Set<InternalRelationship> relationships = new HashSet<>();
            Map<Long, URI> idsUri = new HashMap<>();
            while (rs.hasNext()) {
                Record record = rs.next();
                List<InternalRelationship> relations = (List) record.get("rel").asList();
                relationships.addAll(relations);
                switch (getGraphElementTypeFromRow(record)) {
                    case Vertex:
                        Vertex vertex = addVertexUsingRow(
                                record
                        );
                        idsUri.put(
                                record.get("nId").asLong(),
                                vertex.uri()
                        );
                        break;
                    case Edge:
                        Edge edge = addEdgeUsingRow(
                                record
                        );
                        idsUri.put(
                                record.get("nId").asLong(),
                                edge.uri()
                        );
                        break;
                    case Meta:
                        URI uri = URI.create(record.get(
                                "ge.uri"
                        ).asString());
                        if (uri.equals(centerBubbleUri)) {
                            subGraph.setCenterMeta(
                                    TagFromExtractorQueryRow.usingRowAndKey(
                                            record,
                                            "ge"
                                    ).build()
                            );
                        }
                        break;
                    case Unknown:
                        break;
                }
            }
            for (InternalRelationship relation : relationships) {
                EdgePojo edge = subGraph.edgeWithIdentifier(
                        idsUri.get(relation.startNodeId())
                );
                URI uri = idsUri.get(relation.endNodeId());
                if (uri == null || edge == null) {
                } else if (relation.type().equals("SOURCE_VERTEX")) {
                    edge.setSourceVertex(
                            new VertexInSubGraphPojo(
                                    uri
                            )
                    );
                } else {
                    edge.setDestinationVertex(
                            new VertexInSubGraphPojo(
                                    uri
                            )
                    );
                }
            }
            Iterator<EdgePojo> it = subGraph.edges().values().iterator();
            while (it.hasNext()) {
                EdgePojo edge = it.next();
                Boolean hasSourceVertex = edge.sourceVertex() != null && subGraph.vertices().containsKey(
                        edge.sourceVertex().uri()
                );
                Boolean hasDestinationVertex = edge.destinationVertex() != null && subGraph.vertices().containsKey(
                        edge.destinationVertex().uri()
                );
                if (!hasSourceVertex || !hasDestinationVertex) {
                    it.remove();
                }
            }
            return subGraph;
        }
    }

    private GraphElementType getGraphElementTypeFromRow(Record record) {
        List<String> types = (List) record.get("type").asList();
        GraphElementType type = null;
        for (String typeStr : types) {
            GraphElementType graphElementType = GraphElementType.valueOf(typeStr);
            if (!GraphElementType.commonTypes.contains(graphElementType)) {
                type = graphElementType;
            }
        }
        if (type == null) {
            return GraphElementType.Unknown;
        }
        return type;
    }

    private VertexInSubGraph addVertexUsingRow(Record row) {
        VertexInSubGraph vertex = new VertexFromExtractorQueryRow(
                row,
                SubGraphExtractorNeo4j.GRAPH_ELEMENT_QUERY_KEY
        ).build();
        subGraph.addVertex(
                (VertexInSubGraphPojo) vertex
        );
        return vertex;
    }

    private String queryToGetGraph() {
        return
                "MATCH(start_node:Resource{uri:$centerUri}) " +
                        getMatchQueryPart() +
                        "WHERE ge.shareLevel IN {shareLevels} WITH ge,rel " +
                        "OPTIONAL MATCH (ge)-[idr:IDENTIFIED_TO]->(id) " +
                        "WHERE id.shareLevel IN {shareLevels} " +
                        "RETURN " +
                        vertexAndEdgeCommonQueryPart(GRAPH_ELEMENT_QUERY_KEY) +
                        vertexReturnQueryPart(GRAPH_ELEMENT_QUERY_KEY) +
                        edgeReturnQueryPart(GRAPH_ELEMENT_QUERY_KEY) +
                        (isCenterTagFlow ? TagQueryBuilder.centerTagQueryPart(GRAPH_ELEMENT_QUERY_KEY) : "") +
                        TagQueryBuilder.identificationReturnQueryPart(inShareLevels) +
                        "labels(ge) as type, ID(ge) as nId, rel";
    }

    private String getMatchQueryPart() {
        return "MATCH (start_node)<-[rel:" +
                (isCenterTagFlow ? (Relationships.IDENTIFIED_TO + "|") : "") +
                Relationships.SOURCE_VERTEX + "|" +
                Relationships.DESTINATION_VERTEX + "*0.." + depth * 2 +
                "]->(" + SubGraphExtractorNeo4j.GRAPH_ELEMENT_QUERY_KEY + ") ";
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
        return (inShareLevels.contains(ShareLevel.PRIVATE) ? QueryUtils.getPropertyUsingContainerNameQueryPart(
                prefix,
                VertexTypeOperatorNeo4j.props.nb_private_neighbors.toString()
        ) : "") +
                (inShareLevels.contains(ShareLevel.FRIENDS) ? QueryUtils.getPropertyUsingContainerNameQueryPart(
                        prefix,
                        VertexTypeOperatorNeo4j.props.nb_friend_neighbors.toString()
                ) : "") +
                (inShareLevels.contains(ShareLevel.PUBLIC) || inShareLevels.contains(ShareLevel.PUBLIC_WITH_LINK) ? QueryUtils.getPropertyUsingContainerNameQueryPart(
                        prefix,
                        VertexTypeOperatorNeo4j.props.nb_public_neighbors.toString()
                ) : "") +
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

    private Edge addEdgeUsingRow(Record row) {
        EdgePojo edge = (EdgePojo) EdgeFromExtractorQueryRow.usingRow(
                row
        ).build();
        subGraph.addEdge(edge);
        return edge;
    }
}
