package guru.bubl.module.neo4j_graph_manipulator.graph.graph.fork;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import guru.bubl.module.model.UserUris;
import guru.bubl.module.model.graph.GraphElementOperator;
import guru.bubl.module.model.graph.relation.RelationOperator;
import guru.bubl.module.model.graph.relation.RelationPojo;
import guru.bubl.module.model.graph.fork.NbNeighbors;
import guru.bubl.module.model.graph.ShareLevel;
import guru.bubl.module.model.graph.fork.NbNeighborsOperatorFactory;
import guru.bubl.module.model.graph.vertex.VertexOperator;
import guru.bubl.module.model.graph.vertex.VertexPojo;
import guru.bubl.module.model.graph.fork.ForkOperator;
import guru.bubl.module.neo4j_graph_manipulator.graph.FriendlyResourceFactoryNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.FriendlyResourceNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.OperatorNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.GraphElementFactoryNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.relation.RelationFactoryNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.relation.RelationOperatorNeo4J;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.vertex.VertexFactoryNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.vertex.VertexOperatorNeo4j;
import org.neo4j.driver.v1.*;

import java.net.URI;
import java.util.Map;

import static guru.bubl.module.neo4j_graph_manipulator.graph.RestApiUtilsNeo4j.map;
import static guru.bubl.module.neo4j_graph_manipulator.graph.graph.GraphElementOperatorNeo4j.decrementNbNeighborsQueryPart;
import static guru.bubl.module.neo4j_graph_manipulator.graph.graph.GraphElementOperatorNeo4j.incrementNbNeighborsQueryPart;
import static org.neo4j.driver.v1.Values.parameters;

public class ForkOperatorNeo4J implements ForkOperator, OperatorNeo4j {

    public enum props {
        nb_private_neighbors,
        nb_public_neighbors,
        nb_friend_neighbors
    }

    protected Driver driver;
    protected FriendlyResourceFactoryNeo4j friendlyResourceFactory;
    protected GraphElementFactoryNeo4j graphElementFactoryNeo4j;

    protected URI uri;

    @Inject
    protected NbNeighborsOperatorFactory nbNeighborsOperatorFactory;

    @Inject
    protected VertexFactoryNeo4j vertexFactory;

    @Inject
    protected RelationFactoryNeo4j edgeFactory;


    @AssistedInject
    protected ForkOperatorNeo4J(
            Driver driver,
            GraphElementFactoryNeo4j graphElementFactoryNeo4j,
            FriendlyResourceFactoryNeo4j friendlyResourceFactory,
            @Assisted URI uri
    ) {
        this.uri = uri;
        this.driver = driver;
        this.graphElementFactoryNeo4j = graphElementFactoryNeo4j;
        this.friendlyResourceFactory = friendlyResourceFactory;
    }

    @Override
    @Deprecated
    public void setShareLevel(ShareLevel shareLevel) {
        this.setShareLevel(
                shareLevel,
                graphElementFactoryNeo4j.withUri(uri).getShareLevel()
        );
    }

    @Override
    public void setShareLevel(ShareLevel shareLevel, ShareLevel previousShareLevel) {
        String decrementQueryPart = decrementNbNeighborsQueryPart(previousShareLevel, "d", "SET ");
        String incrementQueryPart = incrementNbNeighborsQueryPart(shareLevel, "d", "SET ");
        try (Session session = driver.session()) {
            session.run(
                    queryPrefix()
                            + "SET n.shareLevel=$shareLevel " +
                            "WITH n OPTIONAL MATCH " +
                            "(n)-[:IDENTIFIED_TO]->(d)" +
                            decrementQueryPart + " " +
                            incrementQueryPart + " " +
                            "WITH n MATCH" +
                            "(n)<-[:SOURCE|DESTINATION]->(e), " +
                            "(e)<-[:SOURCE|DESTINATION]->(d) " +
                            decrementQueryPart + " " +
                            incrementQueryPart + " " +
                            "WITH d,n,e " +
                            "SET e.shareLevel = CASE WHEN (n.shareLevel <= d.shareLevel) THEN n.shareLevel ELSE d.shareLevel END",
                    parameters(
                            "uri", uri().toString(),
                            "shareLevel", shareLevel.getIndex()
                    )
            );
        }
    }

    @Override
    public RelationPojo addVertexAndRelation() {
        return this.addVertexAndRelationIsUnderPatternOrNot(
                new UserUris(
                        graphElementFactoryNeo4j.withUri(uri).getOwnerUsername()
                ).generateVertexUri(),
                null,
                graphElementFactoryNeo4j.withUri(uri).isPatternOrUnderPattern()
        );
    }

    @Override
    public RelationPojo addVertexAndRelationWithIds(String vertexId, String edgeId) {
        return this.addVertexAndRelationWithIdsUnderPatternOrNot(
                vertexId,
                edgeId,
                graphElementFactoryNeo4j.withUri(uri).isPatternOrUnderPattern()
        );
    }

    @Override
    public RelationOperator addRelationToFork(final ForkOperator destinationFork) {
        GraphElementOperator source = graphElementFactoryNeo4j.withUri(uri);
        GraphElementOperator destination = graphElementFactoryNeo4j.withUri(destinationFork.uri());
        if (source.isPatternOrUnderPattern() || destination.isPatternOrUnderPattern()) {
            return null;
        }
        RelationOperator edge = edgeFactory.withSourceAndDestinationUri(
                uri(),
                destination.uri()
        );
        ShareLevel sourceShareLevel = source.getShareLevel();
        ShareLevel destinationShareLevel = destination.getShareLevel();
        if (sourceShareLevel == ShareLevel.FRIENDS && destinationShareLevel == ShareLevel.FRIENDS) {
            edge.createWithShareLevel(ShareLevel.FRIENDS);
        } else if (sourceShareLevel.isPublic() && destinationShareLevel.isPublic()) {
            edge.createWithShareLevel(ShareLevel.PUBLIC_WITH_LINK);
        } else {
            edge.createWithShareLevel(ShareLevel.PRIVATE);
        }
        try (Session session = driver.session()) {
            session.run(
                    "MATCH (s:Vertex {uri:$uri}), (d:Vertex {uri:$destinationUri}) " +
                            incrementNbNeighborsQueryPart(destinationShareLevel, "s", "WITH s,d SET ") +
                            incrementNbNeighborsQueryPart(sourceShareLevel, "d", "WITH s,d SET "),
                    parameters(
                            "uri",
                            this.uri().toString(),
                            "destinationUri",
                            destination.uri().toString()
                    )
            );
            return edge;
        }
    }

    @Override
    public NbNeighbors getNbNeighbors() {
        return nbNeighborsOperatorFactory.withForkUri(uri);
    }

    @Override
    public URI uri() {
        return uri;
    }

    private RelationPojo addVertexAndRelationWithIdsUnderPatternOrNot(String vertexId, String edgeId, Boolean isUnderPattern) {
        UserUris userUri = new UserUris(
                UserUris.ownerUserNameFromUri(uri())
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

    private RelationPojo addVertexAndRelationIsUnderPatternOrNot(URI newVertexUri, URI newEdgeUri, Boolean isUnderPattern) {
        VertexOperatorNeo4j newVertexOperator = vertexFactory.withUri(
                newVertexUri
        );
        this.incrementNumberOfConnectedEdges();
        Boolean isPublic = isUnderPattern || graphElementFactoryNeo4j.withUri(uri).isPublic();
        Map<String, Object> properties = map(
                ForkOperatorNeo4J.props.nb_private_neighbors.name(), isPublic ? 0 : 1,
                ForkOperatorNeo4J.props.nb_public_neighbors.name(), isPublic ? 1 : 0
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
        RelationOperatorNeo4J edgeOperator = newEdgeUri == null ? edgeFactory.withSourceAndDestinationUri(
                uri(),
                newVertexOperator.uri()
        ) : edgeFactory.withUriAndSourceAndDestinationVertex(
                newEdgeUri,
                uri(),
                newVertexOperator.uri()
        );
        RelationPojo newEdge = isUnderPattern ?
                edgeOperator.createEdgeWithAdditionalProperties(
                        map("isUnderPattern", true)
                ) :
                edgeOperator.createEdge();

        newEdge.setDestinationVertex(
                newVertex
        );
        return newEdge;
    }

    protected void incrementNumberOfConnectedEdges() {
        try (Session session = driver.session()) {
            session.run(
                    String.format(
                            "%s SET n.%s= n.%s + 1",
                            queryPrefix(),
                            ForkOperatorNeo4J.props.nb_private_neighbors,
                            ForkOperatorNeo4J.props.nb_private_neighbors
                    ),
                    parameters(
                            "uri", uri().toString()
                    )
            );
        }
    }
}
