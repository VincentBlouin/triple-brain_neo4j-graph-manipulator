/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.meta;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import guru.bubl.module.model.User;
import guru.bubl.module.model.graph.FriendlyResourcePojo;
import guru.bubl.module.model.graph.GraphElementPojo;
import guru.bubl.module.model.graph.tag.TagPojo;
import guru.bubl.module.model.tag.UserTagsOperator;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;

import static org.neo4j.driver.v1.Values.parameters;

public class UserTagsOperatorNeo4J implements UserTagsOperator {

    private Driver driver;
    private User user;

    @AssistedInject
    protected UserTagsOperatorNeo4J(
            Driver driver,
            @Assisted User user
    ) {
        this.driver = driver;
        this.user = user;
    }

    @Override
    public Set<TagPojo> get() {
        Set<TagPojo> userMetas = new HashSet<>();
        try (Session session = driver.session()) {
            StatementResult rs = session.run(
                    "MATCH (n:Meta{owner:$owner}) RETURN n.label as label, n.nb_references as nbReferences, n.uri as uri;",
                    parameters(
                            "owner", user.username()
                    )
            );
            while (rs.hasNext()) {
                Record record = rs.next();
                userMetas.add(
                        new TagPojo(
                                record.get("nbReferences").asInt(),
                                new GraphElementPojo(
                                        new FriendlyResourcePojo(
                                                URI.create(record.get("uri").asString()),
                                                record.get("label").asString()
                                        )
                                )
                        )
                );
            }
            return userMetas;
        }
    }
}
