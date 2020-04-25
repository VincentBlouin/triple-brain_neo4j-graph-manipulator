/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.subgraph;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import guru.bubl.module.model.UserUris;
import guru.bubl.module.model.graph.ShareLevel;
import guru.bubl.module.model.graph.edge.EdgePojo;
import guru.bubl.module.model.graph.graph_element.GraphElementType;
import guru.bubl.module.model.graph.group_relation.GroupRelation;
import guru.bubl.module.model.graph.group_relation.GroupRelationPojo;
import guru.bubl.module.model.graph.relation.Relation;
import guru.bubl.module.model.graph.relation.RelationPojo;
import guru.bubl.module.model.graph.subgraph.SubGraphPojo;
import guru.bubl.module.model.graph.vertex.Vertex;
import guru.bubl.module.model.graph.vertex.VertexPojo;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.FriendlyResourceQueryBuilder;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.QueryUtils;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.TagQueryBuilder;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.fork.ForkOperatorNeo4J;
import org.neo4j.driver.v1.*;
import org.neo4j.driver.v1.types.Relationship;

import java.net.URI;
import java.util.*;

import static org.neo4j.driver.v1.Values.parameters;

public class SubGraphExtractorNeo4j {

    public final static String GRAPH_ELEMENT_QUERY_KEY = "ge";
    private URI centerBubbleUri;
    private Boolean isCenterTagFlow;
    private Integer depth;
    private SubGraphPojo subGraph = SubGraphPojo.withCenterUriVerticesAndEdges(
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
        this.depth = depth != 0 && depth % 2 == 0 ? depth + 1 : depth;
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
            Set<Relationship> relationships = new HashSet<>();
            Map<Long, URI> idsUri = new HashMap<>();
            while (rs.hasNext()) {
                Record record = rs.next();
                Value relationship1 = record.get("rel1");
                if (!relationship1.isNull()) {
                    relationships.add(relationship1.asRelationship());
                }
                Value relationship2 = record.get("rel2");
                if (!relationship2.isNull()) {
                    relationships.add(relationship2.asRelationship());
                }
                Value relationshipList = record.get("relList");
                if (!relationshipList.isNull()) {
                    relationships.addAll((List) relationshipList.asList());
                }
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
                        Relation relation = addEdgeUsingRow(
                                record
                        );
                        idsUri.put(
                                record.get("nId").asLong(),
                                relation.uri()
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
            for (Relationship relation : relationships) {
                URI edgeUri = idsUri.get(relation.startNodeId());
                EdgePojo edge = subGraph.edgeWithIdentifier(
                        edgeUri
                );
                if (edge == null) {
                    edge = subGraph.getGroupRelations().get(edgeUri);
                }
                URI endForkUri = idsUri.get(relation.endNodeId());
                if (edge != null && endForkUri != null) {
                    if (relation.type().equals("SOURCE")) {
                        edge.setSourceUri(
                                endForkUri
                        );
                    } else {
                        edge.setDestinationUri(
                                endForkUri
                        );
                    }
                }
            }
        }
        return subGraph;
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
        String relVariables = this.isCenterTagFlow ? "rel1, rel2" : "relList, rel1";
        return
                String.format(
                        "MATCH(n:Resource{uri:$centerUri}) %s " +
                                "WITH %s, ge MATCH(ge) WHERE ge.shareLevel IN {shareLevels} " +
                                "OPTIONAL MATCH (ge)-[idr:IDENTIFIED_TO]->(id) WHERE id.shareLevel IN {shareLevels} " +
                                "RETURN ge.childrenIndexes, " +
                                vertexAndEdgeCommonQueryPart(GRAPH_ELEMENT_QUERY_KEY) +
                                vertexReturnQueryPart(GRAPH_ELEMENT_QUERY_KEY) +
                                (isCenterTagFlow ? TagQueryBuilder.centerTagQueryPart(GRAPH_ELEMENT_QUERY_KEY) : "") +
                                TagQueryBuilder.tagReturnQueryPart(inShareLevels) +
                                "labels(ge) as type, ID(ge) as nId, %s",
                        (this.isCenterTagFlow ?
                                "OPTIONAL MATCH (n)<-[IDENTIFIED_TO*0..1]-(t) OPTIONAL MATCH (t:GroupRelation)-[rel1:SOURCE]->(gt) OPTIONAL MATCH (t:Edge)-[rel2:SOURCE|DESTINATION]->(ef) WITH rel1, rel2, COLLECT(t) + collect(gt) + collect(ef) as geList UNWIND geList as ge" :
                                "OPTIONAL MATCH (n)<-[relList:SOURCE|DESTINATION*0.." + depth + "]->(e) OPTIONAL MATCH (e:Edge)-[rel1:SOURCE|DESTINATION]->(ef) WITH relList, rel1, COLLECT(e) + collect(ef) as geList UNWIND geList as ge"),
                        relVariables,
                        relVariables
                );

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

    private Relation addEdgeUsingRow(Record row) {
        RelationPojo edge = (RelationPojo) RelationFromExtractorQueryRow.usingRow(
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
