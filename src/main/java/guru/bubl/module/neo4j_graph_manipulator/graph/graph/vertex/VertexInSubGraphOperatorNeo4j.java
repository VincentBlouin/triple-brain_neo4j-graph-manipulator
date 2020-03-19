/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.graph.vertex;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import guru.bubl.module.model.Image;
import guru.bubl.module.model.User;
import guru.bubl.module.model.UserUris;
import guru.bubl.module.model.graph.ShareLevel;
import guru.bubl.module.model.graph.edge.Edge;
import guru.bubl.module.model.graph.edge.EdgeOperator;
import guru.bubl.module.model.graph.edge.EdgePojo;
import guru.bubl.module.model.graph.tag.Tag;
import guru.bubl.module.model.graph.tag.TagPojo;
import guru.bubl.module.model.graph.vertex.*;
import guru.bubl.module.neo4j_graph_manipulator.graph.FriendlyResourceFactoryNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.FriendlyResourceNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.OperatorNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.GraphElementFactoryNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.GraphElementOperatorNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.edge.EdgeFactoryNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.edge.EdgeOperatorNeo4j;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.graphdb.Node;

import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static guru.bubl.module.neo4j_graph_manipulator.graph.RestApiUtilsNeo4j.map;
import static guru.bubl.module.neo4j_graph_manipulator.graph.graph.GraphElementOperatorNeo4j.incrementNbFriendsOrPublicQueryPart;
import static org.neo4j.driver.v1.Values.parameters;

public class VertexInSubGraphOperatorNeo4j implements VertexInSubGraphOperator, OperatorNeo4j {

    public enum props {
        shareLevel,
        is_public
    }

    protected GraphElementOperatorNeo4j graphElementOperator;
    protected VertexFactoryNeo4j vertexFactory;

    protected EdgeFactoryNeo4j edgeFactory;

    protected GraphElementFactoryNeo4j neo4jGraphElementFactory;
    protected Node node;
    protected Driver driver;

    protected VertexTypeOperatorFactory vertexTypeOperatorFactory;


    @Inject
    protected FriendlyResourceFactoryNeo4j friendlyResourceFactory;

    @AssistedInject
    protected VertexInSubGraphOperatorNeo4j(
            VertexFactoryNeo4j vertexFactory,
            EdgeFactoryNeo4j edgeFactory,
            GraphElementFactoryNeo4j neo4jGraphElementFactory,
            Driver driver,
            VertexTypeOperatorFactory vertexTypeOperatorFactory,
            @Assisted URI uri
    ) {
        this.vertexFactory = vertexFactory;
        this.edgeFactory = edgeFactory;
        this.neo4jGraphElementFactory = neo4jGraphElementFactory;
        this.driver = driver;
        this.graphElementOperator = neo4jGraphElementFactory.withUri(
                uri
        );
        this.vertexTypeOperatorFactory = vertexTypeOperatorFactory;
    }

    @AssistedInject
    protected VertexInSubGraphOperatorNeo4j(
            VertexFactoryNeo4j vertexFactory,
            EdgeFactoryNeo4j edgeFactory,
            GraphElementFactoryNeo4j neo4jGraphElementFactory,
            Driver driver,
            VertexTypeOperatorFactory vertexTypeOperatorFactory,
            @Assisted String ownerUserName
    ) {
        this(
                vertexFactory,
                edgeFactory,
                neo4jGraphElementFactory,
                driver,
                vertexTypeOperatorFactory,
                new UserUris(ownerUserName).generateVertexUri()
        );
        create();
    }

    @Override
    public boolean hasEdge(Edge edge) {
        FriendlyResourceNeo4j edgeFriendlyResource = friendlyResourceFactory.withUri(
                edge.uri()
        );
        try (Session session = driver.session()) {
            StatementResult rs = session.run(
                    String.format(
                            "%s, %s, (n)<-[:SOURCE_VERTEX|DESTINATION_VERTEX]-(edge) RETURN edge",
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
    public EdgeOperator getEdgeThatLinksToDestinationVertex(Vertex destinationVertex) {
        FriendlyResourceNeo4j destinationVertexOperator = friendlyResourceFactory.withUri(
                destinationVertex.uri()
        );
        try (Session session = driver.session()) {
            StatementResult rs = session.run(
                    String.format(
                            "%s, %s, (n)<-[:SOURCE_VERTEX|DESTINATION_VERTEX]-(r), " +
                                    "(r)-[:SOURCE_VERTEX|DESTINATION_VERTEX]->(d) " +
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
            StatementResult rs = session.run(
                    String.format(
                            "%s, %s, " +
                                    "(n)<-[:SOURCE_VERTEX]-(r), " +
                                    "(r)-[:DESTINATION_VERTEX]->(d) " +
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
    public EdgePojo addVertexAndRelation() {
        return addVertexAndRelationIsUnderPatternOrNot(
                new UserUris(
                        getOwnerUsername()
                ).generateVertexUri(),
                null,
                this.isPatternOrUnderPattern()
        );
    }

    @Override
    public EdgePojo addVertexAndRelationWithIds(String vertexId, String edgeId) {
        return addVertexAndRelationWithIdsUnderPatternOrNot(
                vertexId,
                edgeId,
                this.isPatternOrUnderPattern()
        );
    }

    private EdgePojo addVertexAndRelationWithIdsUnderPatternOrNot(String vertexId, String edgeId, Boolean isUnderPattern) {
        UserUris userUri = new UserUris(
                getOwnerUsername()
        );
        URI vertexUri = userUri.vertexUriFromShortId(vertexId);
        if (FriendlyResourceNeo4j.haveElementWithUri(vertexUri, driver)) {
            vertexUri = userUri.generateVertexUri();
        }
        URI edgeUri = userUri.edgeUriFromShortId(edgeId);
        if (FriendlyResourceNeo4j.haveElementWithUri(edgeUri, driver)) {
            edgeUri = userUri.generateEdgeUri();
        }
        return this.addVertexAndRelationIsUnderPatternOrNot(
                vertexUri,
                edgeUri,
                isUnderPattern
        );
    }

    private EdgePojo addVertexAndRelationIsUnderPatternOrNot(URI newVertexUri, URI newEdgeUri, Boolean isUnderPattern) {
        VertexInSubGraphOperatorNeo4j newVertexOperator = vertexFactory.withUri(
                newVertexUri
        );
        this.incrementNumberOfConnectedEdges();
        Boolean isPublic = isUnderPattern || this.isPublic();
        Map<String, Object> properties = map(
                VertexTypeOperatorNeo4j.props.nb_private_neighbors.name(), isPublic ? 0 : 1,
                VertexTypeOperatorNeo4j.props.nb_public_neighbors.name(), isPublic ? 1 : 0
        );
        if (isUnderPattern) {
            properties.put(
                    "isUnderPattern",
                    true
            );
            properties.put(
                    "shareLevel",
                    ShareLevel.PUBLIC.getIndex()
            );
        }
        VertexPojo newVertex = newVertexOperator.createVertexUsingInitialValues(
                properties
        );
        EdgeOperatorNeo4j edgeOperator = newEdgeUri == null ? edgeFactory.withSourceAndDestinationVertex(
                this,
                newVertexOperator
        ) : edgeFactory.withUriAndSourceAndDestinationVertex(
                newEdgeUri,
                this,
                newVertexOperator
        );
        EdgePojo newEdge = isUnderPattern ?
                edgeOperator.createEdgeWithAdditionalProperties(
                        map("isUnderPattern", true)
                ) :
                edgeOperator.createEdge();

        newEdge.setDestinationVertex(
                new VertexInSubGraphPojo(
                        newVertex
                )
        );
        return newEdge;
    }

    @Override
    public EdgeOperator addRelationToVertex(final VertexOperator destinationVertex) {
        if (this.isPatternOrUnderPattern() || destinationVertex.isPatternOrUnderPattern()) {
            return null;
        }
        EdgeOperator edge = edgeFactory.withSourceAndDestinationVertex(
                this,
                destinationVertex
        );
        ShareLevel sourceShareLevel = this.getShareLevel();
        ShareLevel destinationShareLevel = destinationVertex.getShareLevel();
        if (sourceShareLevel == ShareLevel.FRIENDS && destinationShareLevel == ShareLevel.FRIENDS) {
            edge.createWithShareLevel(ShareLevel.FRIENDS);
        } else if (sourceShareLevel.isPublic() && destinationShareLevel.isPublic()) {
            edge.createWithShareLevel(ShareLevel.PUBLIC_WITH_LINK);
        } else {
            edge.createWithShareLevel(ShareLevel.PRIVATE);
        }
        String query = String.format(
                "MATCH (s:Vertex {uri:$uri}), (d:Vertex {uri:$destinationUri}) " +
                        "SET " +
                        "s.%s=s.%s+1, " +
                        "d.%s=d.%s+1 " +
                        incrementNbFriendsOrPublicQueryPart(destinationShareLevel, "s", "WITH s,d SET ") +
                        incrementNbFriendsOrPublicQueryPart(sourceShareLevel, "d", "WITH s,d SET "),
                VertexTypeOperatorNeo4j.props.nb_private_neighbors,
                VertexTypeOperatorNeo4j.props.nb_private_neighbors,
                VertexTypeOperatorNeo4j.props.nb_private_neighbors,
                VertexTypeOperatorNeo4j.props.nb_private_neighbors
        );
        try (Session session = driver.session()) {
            session.run(
                    query,
                    parameters(
                            "uri",
                            this.uri().toString(),
                            "destinationUri",
                            destinationVertex.uri().toString()
                    )
            );
            return edge;
        }
    }

    @Override
    public void remove() {
        try (Session session = driver.session()) {
            session.run(
                    queryPrefix() +
                            "OPTIONAL MATCH " +
                            "(n)<-[:SOURCE_VERTEX|DESTINATION_VERTEX]-(e), " +
                            "(e)-[:SOURCE_VERTEX|DESTINATION_VERTEX]->(v) " +
                            "WITH e, n " +
                            "DETACH DELETE n, e",
                    parameters(
                            "uri",
                            this.uri().toString()
                    )
            );
        }
    }

    @Override
    public Map<URI, EdgeOperator> connectedEdges() {
        Map<URI, EdgeOperator> edges = new HashMap<>();
        try (Session session = driver.session()) {
            StatementResult rs = session.run(
                    queryPrefix() +
                            "MATCH (n)<-[:SOURCE_VERTEX|DESTINATION_VERTEX]-(edge) " +
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
    public VertexOperator forkForUserUsingCache(User user, Vertex cache) {
        VertexOperator clone = vertexFactory.withUri(
                new UserUris(
                        user.username()
                ).generateVertexUri()
        );
        graphElementOperator.forkUsingCreationPropertiesAndCache(
                clone,
                map(
                        VertexTypeOperatorNeo4j.props.nb_private_neighbors.name(),
                        cache.getNbNeighbors().getPrivate()
                ),
                cache
        );
        return clone;
    }

    protected void incrementNumberOfConnectedEdges() {
        try (Session session = driver.session()) {
            session.run(
                    String.format(
                            "%s SET n.%s= n.%s + 1",
                            queryPrefix(),
                            VertexTypeOperatorNeo4j.props.nb_private_neighbors,
                            VertexTypeOperatorNeo4j.props.nb_private_neighbors
                    ),
                    parameters(
                            "uri", uri().toString()
                    )
            );
        }
    }

    @Override
    public void removeTag(Tag tag) {
        graphElementOperator.removeTag(tag);
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
        return vertexTypeOperatorFactory.withUri(uri()).getNbNeighbors();
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
    public URI getPatternUri() {
        return graphElementOperator.getPatternUri();
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
        this.connectedEdges().values().forEach(
                (edge) -> {
                    if (edge.destinationVertex().equals(this)) {
                        edge.changeDestinationVertex(vertexOperator);
                    } else {
                        edge.changeSourceVertex(vertexOperator);
                    }
                }
        );
        this.getTags().values().forEach((tag) -> {
            vertexOperator.addTag(tag);
        });
        this.remove();
        return true;
    }

    @Override
    public void setShareLevel(ShareLevel shareLevel) {
        vertexTypeOperatorFactory.withUri(uri()).setShareLevel(shareLevel);
    }

    @Override
    public void setShareLevel(ShareLevel shareLevel, ShareLevel previousShareLevel) {
        vertexTypeOperatorFactory.withUri(uri()).setShareLevel(shareLevel, previousShareLevel);
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
                            "CALL apoc.path.subgraphAll(n, {relationshipFilter:'SOURCE_VERTEX, DESTINATION_VERTEX'}) YIELD nodes " +
                            "UNWIND nodes as s " +
                            "SET s.shareLevel=40," +
                            "s.isUnderPattern=true," +
                            "s.nb_public_neighbors = s.nb_private_neighbors," +
                            "s.nb_private_neighbors = 0," +
                            "s.nb_friend_neighbors = 0 " +
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
                            "CALL apoc.path.subgraphAll(n, {relationshipFilter:'SOURCE_VERTEX, DESTINATION_VERTEX'}) YIELD nodes " +
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

    private VertexPojo createVertexUsingInitialValues(Map<String, Object> values) {
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
                props.shareLevel.name(), ShareLevel.PRIVATE.getIndex(),
                VertexTypeOperatorNeo4j.props.nb_private_neighbors.name(), 0,
                VertexTypeOperatorNeo4j.props.nb_public_neighbors.name(), 0,
                VertexTypeOperatorNeo4j.props.nb_friend_neighbors.name(), 0
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
        if (creationProperties.containsKey(VertexTypeOperatorNeo4j.props.nb_public_neighbors.name())) {
            vertex.getNbNeighbors().setPublic(
                    (Integer) creationProperties.get(
                            VertexTypeOperatorNeo4j.props.nb_public_neighbors.name()
                    )
            );
        }
        return vertex;
    }
}
