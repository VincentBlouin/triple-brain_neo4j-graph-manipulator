/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.graph.vertex;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import guru.bubl.module.model.Image;
import guru.bubl.module.model.UserUris;
import guru.bubl.module.model.graph.ShareLevel;
import guru.bubl.module.model.graph.relation.Relation;
import guru.bubl.module.model.graph.relation.RelationOperator;
import guru.bubl.module.model.graph.relation.RelationPojo;
import guru.bubl.module.model.graph.fork.ForkOperatorFactory;
import guru.bubl.module.model.graph.fork.NbNeighbors;
import guru.bubl.module.model.graph.tag.Tag;
import guru.bubl.module.model.graph.tag.TagPojo;
import guru.bubl.module.model.graph.vertex.*;
import guru.bubl.module.neo4j_graph_manipulator.graph.FriendlyResourceFactoryNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.FriendlyResourceNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.OperatorNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.graph_element.GraphElementFactoryNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.graph_element.GraphElementOperatorNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.relation.RelationFactoryNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.fork.ForkOperatorNeo4J;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.neo4j.driver.Result;

import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static guru.bubl.module.neo4j_graph_manipulator.graph.RestApiUtilsNeo4j.map;
import static org.neo4j.driver.Values.parameters;

public class VertexOperatorNeo4j implements VertexOperator, OperatorNeo4j {

    public enum props {
        shareLevel,
        is_public
    }

    protected GraphElementOperatorNeo4j graphElementOperator;
    protected VertexFactoryNeo4j vertexFactory;

    protected RelationFactoryNeo4j edgeFactory;

    protected GraphElementFactoryNeo4j neo4jGraphElementFactory;
    protected Driver driver;

    protected ForkOperatorFactory forkOperatorFactory;


    @Inject
    protected FriendlyResourceFactoryNeo4j friendlyResourceFactory;

    @AssistedInject
    protected VertexOperatorNeo4j(
            VertexFactoryNeo4j vertexFactory,
            RelationFactoryNeo4j edgeFactory,
            GraphElementFactoryNeo4j neo4jGraphElementFactory,
            Driver driver,
            ForkOperatorFactory forkOperatorFactory,
            @Assisted URI uri
    ) {
        this.vertexFactory = vertexFactory;
        this.edgeFactory = edgeFactory;
        this.neo4jGraphElementFactory = neo4jGraphElementFactory;
        this.driver = driver;
        this.graphElementOperator = neo4jGraphElementFactory.withUri(
                uri
        );
        this.forkOperatorFactory = forkOperatorFactory;
    }

    @AssistedInject
    protected VertexOperatorNeo4j(
            VertexFactoryNeo4j vertexFactory,
            RelationFactoryNeo4j edgeFactory,
            GraphElementFactoryNeo4j neo4jGraphElementFactory,
            Driver driver,
            ForkOperatorFactory forkOperatorFactory,
            @Assisted String ownerUserName
    ) {
        this(
                vertexFactory,
                edgeFactory,
                neo4jGraphElementFactory,
                driver,
                forkOperatorFactory,
                new UserUris(ownerUserName).generateVertexUri()
        );
        create();
    }

    @Override
    public boolean hasEdge(Relation relation) {
        FriendlyResourceNeo4j edgeFriendlyResource = friendlyResourceFactory.withUri(
                relation.uri()
        );
        try (Session session = driver.session()) {
            Result rs = session.run(
                    String.format(
                            "%s, %s, (n)<-[:SOURCE|DESTINATION]-(edge) RETURN edge",
                            queryPrefix(),
                            edgeFriendlyResource.addToSelectUsingVariableName("edge", "edgeUri")
                    ),
                    parameters(
                            "uri", uri().toString(),
                            "edgeUri", edgeFriendlyResource.uri().toString()
                    )
            );
            return rs.hasNext();
        }
    }

    @Override
    public RelationOperator getEdgeToDestinationVertex(Vertex destinationVertex) {
        FriendlyResourceNeo4j destinationVertexOperator = friendlyResourceFactory.withUri(
                destinationVertex.uri()
        );
        try (Session session = driver.session()) {
            Result rs = session.run(
                    String.format(
                            "%s, %s, (n)<-[:SOURCE|DESTINATION]-(r), " +
                                    "(r)-[:SOURCE|DESTINATION]->(d) " +
                                    "RETURN r.uri as uri",
                            queryPrefix(),
                            destinationVertexOperator.addToSelectUsingVariableName("d", "destinationUri")
                    ),
                    parameters(
                            "uri", uri().toString(),
                            "destinationUri", destinationVertexOperator.uri().toString()
                    )
            );
            if (!rs.hasNext()) {
                throw new RuntimeException(
                        "Edge between vertex with " + uri() +
                                " and vertex with uri " + destinationVertex.uri() +
                                " was not found"
                );
            }
            Record record = rs.next();
            return edgeFactory.withUri(URI.create(
                    record.get(
                            "uri"
                    ).asString()
            ));
        }
    }

    @Override
    public Boolean hasDestinationVertex(Vertex destinationVertex) {
        FriendlyResourceNeo4j destinationVertexOperator = friendlyResourceFactory.withUri(
                destinationVertex.uri()
        );
        try (Session session = driver.session()) {
            Result rs = session.run(
                    String.format(
                            "%s, %s, " +
                                    "(n)<-[:SOURCE]-(r), " +
                                    "(r)-[:DESTINATION]->(d) " +
                                    "RETURN n.uri",
                            queryPrefix(),
                            destinationVertexOperator.addToSelectUsingVariableName("d", "destinationUri")
                    ),
                    parameters(
                            "uri", uri().toString(),
                            "destinationUri", destinationVertexOperator.uri().toString()
                    )
            );
            return rs.hasNext();
        }
    }

    @Override
    public RelationPojo addVertexAndRelation() {
        return forkOperatorFactory.withUri(uri()).addVertexAndRelation();
    }

    @Override
    public RelationPojo addVertexAndRelationWithIds(String vertexId, String edgeId) {
        return forkOperatorFactory.withUri(uri()).addVertexAndRelationWithIds(vertexId, edgeId);
    }

    @Override
    public RelationOperator addRelationToFork(URI destinationUri, ShareLevel sourceShareLevel, ShareLevel destinationShareLevel) {
        return forkOperatorFactory.withUri(uri()).addRelationToFork(
                destinationUri,
                sourceShareLevel,
                destinationShareLevel
        );
    }

    @Override
    public void remove() {
        forkOperatorFactory.withUri(uri()).remove();
    }

    @Override
    public Map<URI, RelationOperator> connectedEdges() {
        Map<URI, RelationOperator> edges = new HashMap<>();
        try (Session session = driver.session()) {
            Result rs = session.run(
                    queryPrefix() +
                            "MATCH (n)<-[:SOURCE|DESTINATION]-(edge) " +
                            "RETURN edge.uri as uri",
                    parameters(
                            "uri",
                            this.uri().toString()
                    )
            );
            while (rs.hasNext()) {
                Record record = rs.next();
                URI edgeUri = URI.create(
                        record.get(
                                "uri"
                        ).asString()
                );
                edges.put(
                        edgeUri,
                        edgeFactory.withUri(
                                edgeUri
                        )
                );
            }
            return edges;
        }
    }

    @Override
    public void removeTag(Tag tag, ShareLevel sourceShareLevel) {
        graphElementOperator.removeTag(tag, sourceShareLevel);
    }

    @Override
    public Map<URI, TagPojo> addTag(Tag friendlyResource, ShareLevel sourceShareLevel) {
        return graphElementOperator.addTag(friendlyResource, sourceShareLevel);
    }

    @Override
    public Map<URI, TagPojo> getTags() {
        return graphElementOperator.getTags();
    }

    @Override
    public Boolean isPublic() {
        return graphElementOperator.isPublic();
    }

    @Override
    public NbNeighbors getNbNeighbors() {
        return forkOperatorFactory.withUri(uri()).getNbNeighbors();
    }


    @Override
    public Date creationDate() {
        return graphElementOperator.creationDate();
    }

    @Override
    public Date lastModificationDate() {
        return graphElementOperator.lastModificationDate();
    }

    @Override
    public String getOwnerUsername() {
        return graphElementOperator.getOwnerUsername();
    }

    @Override
    public URI uri() {
        return graphElementOperator.uri();
    }

    @Override
    public String label() {
        return graphElementOperator.label();
    }

    @Override
    public Set<Image> images() {
        return graphElementOperator.images();
    }

    @Override
    public Boolean gotImages() {
        return graphElementOperator.gotImages();
    }

    @Override
    public String comment() {
        return graphElementOperator.comment();
    }

    @Override
    public void comment(String comment) {
        graphElementOperator.comment(
                comment
        );
    }

    @Override
    public Boolean gotComments() {
        return graphElementOperator.gotComments();
    }

    @Override
    public void addImages(Set<Image> images) {
        graphElementOperator.addImages(
                images
        );
    }

    @Override
    public void label(String label) {
        graphElementOperator.label(label);
    }

    @Override
    public boolean hasLabel() {
        return graphElementOperator.hasLabel();
    }

    @Override
    public String getColors() {
        return graphElementOperator.getColors();
    }

    @Override
    public String getFont() {
        return graphElementOperator.getFont();
    }

    @Override
    public void setColors(String colors) {
        graphElementOperator.setColors(colors);
    }

    @Override
    public void setFont(String font) {
        graphElementOperator.setFont(font);
    }

    @Override
    public void setChildrenIndex(String childrenIndex) {
        graphElementOperator.setChildrenIndex(
                childrenIndex
        );
    }

    @Override
    public Boolean isUnderPattern() {
        return graphElementOperator.isUnderPattern();
    }

    @Override
    public Boolean isPatternOrUnderPattern() {
        return graphElementOperator.isPatternOrUnderPattern();
    }

    @Override
    public String getChildrenIndex() {
        return graphElementOperator.getChildrenIndex();
    }

    @Override
    public URI getCopiedFromUri() {
        return graphElementOperator.getCopiedFromUri();
    }

    @Override
    public boolean equals(Object vertexToCompareAsObject) {
        return graphElementOperator.equals(vertexToCompareAsObject);
    }

    @Override
    public int hashCode() {
        return graphElementOperator.hashCode();
    }

    @Override
    public void create() {
        createUsingInitialValues(map());
    }

    @Override
    public void createUsingInitialValues(Map<String, Object> values) {
        createVertexUsingInitialValues(
                values
        );
    }

    @Override
    public Boolean mergeTo(VertexOperator vertexOperator) {
        if (this.isPatternOrUnderPattern() || vertexOperator.isPatternOrUnderPattern()) {
            return false;
        }
        try (Session session = driver.session()) {
            session.run(
                    queryPrefix() + ", (mergeTo:Resource{uri:$mergeToUri}) " +
                            "SET mergeTo.nb_private_neighbors = mergeTo.nb_private_neighbors + n.nb_private_neighbors," +
                            "mergeTo.nb_friend_neighbors = mergeTo.nb_friend_neighbors + n.nb_friend_neighbors," +
                            "mergeTo.nb_public_neighbors = mergeTo.nb_public_neighbors + n.nb_public_neighbors " +
                            "WITH n,mergeTo " +
                            "OPTIONAL MATCH (n)<-[r:SOURCE|DESTINATION]-(e) " +
                            "OPTIONAL MATCH (e)-[:SOURCE|DESTINATION]-(nv)-[:SOURCE|DESTINATION]-(nve)-[:SOURCE|DESTINATION]-(mergeTo) " +
                            "DETACH DELETE nve " +
                            "WITH n, mergeTo " +
                            "OPTIONAL MATCH (n)<-[r:SOURCE]-(e) " +
                            "MERGE (mergeTo)<-[:SOURCE]-(e) " +
                            "DELETE r " +
                            "WITH mergeTo, n " +
                            "OPTIONAL MATCH (n)<-[r:DESTINATION]-(e) " +
                            "MERGE (mergeTo)<-[:DESTINATION]-(e) " +
                            "DELETE r " +
                            "WITH mergeTo, n " +
                            "OPTIONAL MATCH (n)-[r:IDENTIFIED_TO]->(t) " +
                            "MERGE (mergeTo)-[:IDENTIFIED_TO]->(t) " +
                            "DELETE r " +
                            "WITH n " +
                            "DETACH DELETE n ",
                    parameters(
                            "uri", this.uri().toString(),
                            "mergeToUri", vertexOperator.uri().toString()
                    )
            );
        }
        return true;
    }

    @Override
    public void setShareLevel(ShareLevel shareLevel) {
        forkOperatorFactory.withUri(uri()).setShareLevel(shareLevel);
    }

    @Override
    public void setShareLevel(ShareLevel shareLevel, ShareLevel previousShareLevel) {
        forkOperatorFactory.withUri(uri()).setShareLevel(shareLevel, previousShareLevel);
    }

    @Override
    public String getPrivateContext() {
        return graphElementOperator.getPrivateContext();
    }

    @Override
    public void addUpdateNotifications(String action) {
        graphElementOperator.addUpdateNotifications(action);
    }

    @Override
    public Boolean makePattern() {
        if (isPatternOrUnderPattern()) {
            return false;
        }
        try (Session session = driver.session()) {
            session.run(
                    queryPrefix() + "SET n:Pattern,n.nbPatternUsage=0 " +
                            "WITH n " +
                            "CALL apoc.path.subgraphAll(n, {relationshipFilter:'SOURCE, DESTINATION'}) YIELD nodes " +
                            "UNWIND nodes as s " +
                            "SET s.shareLevel=40," +
                            "s.isUnderPattern=true," +
                            "s.nb_public_neighbors=(s.nb_private_neighbors + s.nb_friend_neighbors + s.nb_public_neighbors)," +
                            "s.nb_private_neighbors=0," +
                            "s.nb_friend_neighbors=0 " +
                            "WITH s,n " +
                            "REMOVE n.isUnderPattern " +
                            "WITH s " +
                            "MATCH (s)-[:IDENTIFIED_TO]->(tag) " +
                            "SET tag.shareLevel=40"
                    ,
                    parameters(
                            "uri", uri().toString()
                    )
            );
        }
        return true;
    }

    @Override
    public void undoPattern() {
        try (Session session = driver.session()) {
            session.run(
                    queryPrefix() + "remove n:Pattern " +
                            "WITH n " +
                            "CALL apoc.path.subgraphAll(n, {relationshipFilter:'SOURCE, DESTINATION'}) YIELD nodes " +
                            "UNWIND nodes as s " +
                            "REMOVE s.isUnderPattern",
                    parameters(
                            "uri", uri().toString()
                    )
            );
        }
    }

    @Override
    public Integer getNbPatternUsage() {
        try (Session session = driver.session()) {
            return session.run(
                    queryPrefix() + "RETURN n.nbPatternUsage",
                    parameters(
                            "uri", uri().toString()
                    )
            ).single().get("n.nbPatternUsage").asInt();
        }
    }

    @Override
    public Boolean isPattern() {
        try (Session session = driver.session()) {
            return session.run(
                    queryPrefix() + "RETURN 'Pattern' IN LABELS(n) as isPattern",
                    parameters(
                            "uri", uri().toString()
                    )
            ).single().get("isPattern").asBoolean();
        }
    }

    @Override
    public ShareLevel getShareLevel() {
        return graphElementOperator.getShareLevel();
    }

    public VertexPojo createVertexUsingInitialValues(Map<String, Object> values) {
        Map<String, Object> props = addCreationProperties(
                values
        );
        VertexPojo vertexPojo = pojoFromCreationProperties(
                props
        );
        try (Session session = driver.session()) {
            session.run(
                    "CREATE(n:Resource:GraphElement:Vertex $vertex)",
                    parameters(
                            "vertex",
                            props
                    )
            );
            return vertexPojo;
        }
    }

    @Override
    public Map<String, Object> addCreationProperties(Map<String, Object> map) {
        Map<String, Object> newMap = map(
                props.shareLevel.name(), ShareLevel.PRIVATE.getIndex()
        );
        newMap.putAll(
                map
        );
        newMap = graphElementOperator.addCreationProperties(
                newMap
        );
        return newMap;
    }

    public VertexPojo pojoFromCreationProperties(Map<String, Object> creationProperties) {
        VertexPojo vertex = new VertexPojo(
                graphElementOperator.pojoFromCreationProperties(
                        creationProperties
                )
        );
        if (creationProperties.containsKey(ForkOperatorNeo4J.props.nb_public_neighbors.name())) {
            vertex.getNbNeighbors().setPublic(
                    (Integer) creationProperties.get(
                            ForkOperatorNeo4J.props.nb_public_neighbors.name()
                    )
            );
        }
        return vertex;
    }
}
