package guru.bubl.module.neo4j_graph_manipulator.graph.graph.fork;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import guru.bubl.module.model.graph.fork.NbNeighbors;
import guru.bubl.module.neo4j_graph_manipulator.graph.OperatorNeo4j;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;

import java.net.URI;

import static org.neo4j.driver.v1.Values.parameters;

public class NbNeighborsOperatorNeo4j implements NbNeighbors, OperatorNeo4j {

    private URI uri;

    @Inject
    Driver driver;

    @AssistedInject
    protected NbNeighborsOperatorNeo4j(
            @Assisted URI uri
    ) {
        this.uri = uri;
    }


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
                            ForkOperatorNeo4J.props.nb_private_neighbors
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

    @Override
    public URI uri() {
        return uri;
    }
}


