package guru.bubl.module.neo4j_graph_manipulator.graph.graph.notification;

import com.google.inject.Inject;
import guru.bubl.module.model.User;
import guru.bubl.module.model.notification.Notification;
import guru.bubl.module.model.notification.NotificationOperator;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;

import java.net.URI;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.neo4j.driver.Values.parameters;

public class NotificationOperatorNeo4j implements NotificationOperator {

    @Inject
    private Driver driver;

    @Override
    public void add() {

    }

    @Override
    public List<Notification> listForUserAndNbSkip(User user, Integer nbSkip) {
        List<Notification> notifications = new ArrayList<>();
        try (Session session = driver.session()) {
            String query = "MATCH (n:Notification{owner:$owner}) " +
                    "RETURN n.action, n.watchUri, n.rootUri, n.watchLabel " +
                    "ORDER BY n.creationDate DESC SKIP " + nbSkip + " LIMIT 10 ";
            Result rs = session.run(
                    query,
                    parameters(
                            "owner", user.username()
                    )
            );
            while (rs.hasNext()) {
                Record record = rs.next();
                notifications.add(
                        new Notification(
                                URI.create(record.get("n.rootUri").asString()),
                                URI.create(record.get("n.watchUri").asString()),
                                new Date(),
                                record.get("n.action").asString(),
                                record.get("n.watchLabel").asString()
                        )
                );
            }
        }
        return notifications;
    }
}
