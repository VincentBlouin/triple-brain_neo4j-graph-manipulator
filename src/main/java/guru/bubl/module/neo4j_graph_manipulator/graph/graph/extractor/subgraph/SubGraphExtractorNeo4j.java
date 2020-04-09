/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.subgraph;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import guru.bubl.module.model.UserUris;
import guru.bubl.module.model.graph.GraphElement;
import guru.bubl.module.model.graph.GraphElementType;
import guru.bubl.module.model.graph.ShareLevel;
import guru.bubl.module.model.graph.edge.Edge;
import guru.bubl.module.model.graph.edge.EdgePojo;
import guru.bubl.module.model.graph.group_relation.GroupRelation;
import guru.bubl.module.model.graph.group_relation.GroupRelationPojo;
import guru.bubl.module.model.graph.subgraph.SubGraphPojo;
import guru.bubl.module.model.graph.vertex.Vertex;
import guru.bubl.module.model.graph.vertex.VertexPojo;
import guru.bubl.module.neo4j_graph_manipulator.graph.Relationships;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.FriendlyResourceQueryBuilder;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.TagQueryBuilder;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.QueryUtils;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.fork.ForkOperatorNeo4J;
import org.neo4j.driver.internal.InternalRelationship;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;

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
        this.isCenterTagFlow = UserUris.isUriOfATag(centerBubbleUri);
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
        this.isCenterTagFlow = UserUris.isUriOfATag(centerBubbleUri);
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
                    case GroupRelation:
                        GroupRelation groupRelation = addGroupRelationUsingRow(
                                record
                        );
                        idsUri.put(
                                record.get("nId").asLong(),
                                groupRelation.uri()
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
                } else if (relation.type().equals("SOURCE")) {
                    edge.setSource(
                            uri
                    );
                } else {
                    edge.setDestinationVertex(
                            new VertexPojo(
                                    uri
                            )
                    );
                }
            }
            Iterator<EdgePojo> it = subGraph.edges().values().iterator();
            while (it.hasNext()) {
                EdgePojo edge = it.next();
                GraphElement source = edge.getSource();
                Boolean hasSource = source != null && subGraph.containsGraphElement(
                        edge
                );
                GraphElement destination = edge.destinationFork();
                Boolean hasDestinationVertex = destination != null && subGraph.vertices().containsKey(
                        destination.uri()
                );
                if (!hasSource || !hasDestinationVertex) {
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

    private Vertex addVertexUsingRow(Record row) {
        Vertex vertex = new VertexFromExtractorQueryRow(
                row,
                SubGraphExtractorNeo4j.GRAPH_ELEMENT_QUERY_KEY
        ).build();
        subGraph.addVertex(
                (VertexPojo) vertex
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
                        (isCenterTagFlow ? TagQueryBuilder.centerTagQueryPart(GRAPH_ELEMENT_QUERY_KEY) : "") +
                        TagQueryBuilder.identificationReturnQueryPart(inShareLevels) +
                        "labels(ge) as type, ID(ge) as nId, rel";
    }

    private String getMatchQueryPart() {
        return "MATCH (start_node)<-[rel:" +
                (isCenterTagFlow ? (Relationships.IDENTIFIED_TO + "|") : "") +
                Relationships.SOURCE + "|" +
                Relationships.DESTINATION + "*0.." + depth * 2 +
                "]->(" + SubGraphExtractorNeo4j.GRAPH_ELEMENT_QUERY_KEY + ") ";
    }

    private String vertexAndEdgeCommonQueryPart(String prefix) {
        return FriendlyResourceQueryBuilder.returnQueryPartUsingPrefix(prefix);
    }

    private String vertexReturnQueryPart(String prefix) {
        return vertexSpecificPropertiesQueryPartUsingPrefix(prefix) +
                FriendlyResourceQueryBuilder.imageReturnQueryPart(prefix);
    }

    private String vertexSpecificPropertiesQueryPartUsingPrefix(String prefix) {
        return (inShareLevels.contains(ShareLevel.PRIVATE) ? QueryUtils.getPropertyUsingContainerNameQueryPart(
                prefix,
                ForkOperatorNeo4J.props.nb_private_neighbors.toString()
        ) : "") +
                (inShareLevels.contains(ShareLevel.FRIENDS) ? QueryUtils.getPropertyUsingContainerNameQueryPart(
                        prefix,
                        ForkOperatorNeo4J.props.nb_friend_neighbors.toString()
                ) : "") +
                (inShareLevels.contains(ShareLevel.PUBLIC) || inShareLevels.contains(ShareLevel.PUBLIC_WITH_LINK) ? QueryUtils.getPropertyUsingContainerNameQueryPart(
                        prefix,
                        ForkOperatorNeo4J.props.nb_public_neighbors.toString()
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

    private GroupRelation addGroupRelationUsingRow(Record row) {
        GroupRelationPojo groupRelation = GroupRelationFromExtractorQueryRow.withRowAndKeyPrefix(
                row,
                SubGraphExtractorNeo4j.GRAPH_ELEMENT_QUERY_KEY
        ).build();
        subGraph.addGroupRelation(groupRelation);
        return groupRelation;
    }
}
