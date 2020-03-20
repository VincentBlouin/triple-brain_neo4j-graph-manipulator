package guru.bubl.module.neo4j_graph_manipulator.graph.graph.vertex;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import guru.bubl.module.model.graph.vertex.NbNeighbors;
import guru.bubl.module.model.graph.ShareLevel;
import guru.bubl.module.model.graph.vertex.VertexTypeOperator;
import guru.bubl.module.neo4j_graph_manipulator.graph.FriendlyResourceFactoryNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.OperatorNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.GraphElementFactoryNeo4j;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;

import java.net.URI;

import static guru.bubl.module.neo4j_graph_manipulator.graph.graph.GraphElementOperatorNeo4j.decrementNbNeighborsQueryPart;
import static guru.bubl.module.neo4j_graph_manipulator.graph.graph.GraphElementOperatorNeo4j.incrementNbNeighborsQueryPart;
import static org.neo4j.driver.v1.Values.parameters;

public class VertexTypeOperatorNeo4j implements VertexTypeOperator, OperatorNeo4j {

    public enum props {
        nb_private_neighbors,
        nb_public_neighbors,
        nb_friend_neighbors
    }

    protected Driver driver;
    protected FriendlyResourceFactoryNeo4j friendlyResourceFactory;
    protected GraphElementFactoryNeo4j graphElementFactoryNeo4j;

    protected URI uri;


    @AssistedInject
    protected VertexTypeOperatorNeo4j(
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
                            "(n)<-[:SOURCE_VERTEX|DESTINATION_VERTEX]->(e), " +
                            "(e)<-[:SOURCE_VERTEX|DESTINATION_VERTEX]->(d) " +
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
    public NbNeighbors getNbNeighbors() {
        return new NbNeighborsNeo4j();
    }

    @Override
    public URI uri() {
        return uri;
    }

    private class NbNeighborsNeo4j implements NbNeighbors {

        @Override
        public Integer getPrivate() {
            try (Session session = driver.session()) {
                StatementResult rs = session.run(
                        String.format(
                                "%sRETURN n.nb_private_neighbors as result",
                                queryPrefix()
                        ),
                        parameters(
                                "uri",
                                uri().toString()
                        )
                );
                Record record = rs.next();
                return record.get("result").asObject() == null ? 0 : record.get("result").asInt();
            }
        }

        @Override
        public void setPrivate(Integer nbPrivate) {
            try (Session session = driver.session()) {
                session.run(
                        String.format(
                                "%s SET n.%s=$nbPrivateNeighbors",
                                queryPrefix(),
                                VertexTypeOperatorNeo4j.props.nb_private_neighbors
                        ),
                        parameters(
                                "uri",
                                uri.toString(),
                                "nbPrivateNeighbors",
                                nbPrivate
                        )
                );
            }
        }

        @Override
        public Integer getFriend() {
            try (Session session = driver.session()) {
                StatementResult rs = session.run(
                        queryPrefix() + "RETURN n.nb_friend_neighbors as result",
                        parameters(
                                "uri",
                                uri().toString()
                        )
                );
                Record record = rs.next();
                return record.get("result").asObject() == null ? 0 : record.get("result").asInt();
            }
        }

        @Override
        public void setFriend(Integer friend) {
            try (Session session = driver.session()) {
                session.run(
                        queryPrefix() + "SET n.nb_friend_neighbors=$nbFriendNeighbors",
                        parameters(
                                "uri",
                                uri.toString(),
                                "nbFriendNeighbors",
                                friend
                        )
                );
            }
        }

        @Override
        public Integer getPublic() {
            try (Session session = driver.session()) {
                StatementResult rs = session.run(
                        String.format(
                                "%sRETURN n.nb_public_neighbors as result",
                                queryPrefix()
                        ),
                        parameters(
                                "uri",
                                uri().toString()
                        )
                );
                Record record = rs.next();
                return record.get("result").asObject() == null ? 0 : record.get("result").asInt();
            }
        }

        @Override
        public void setPublic(Integer nbPublic) {
            try (Session session = driver.session()) {
                session.run(
                        queryPrefix() + "SET n.nb_public_neighbors=$nbPublicNeighbors",
                        parameters(
                                "uri",
                                uri.toString(),
                                "nbPublicNeighbors",
                                nbPublic
                        )
                );
            }
        }
    }

}
