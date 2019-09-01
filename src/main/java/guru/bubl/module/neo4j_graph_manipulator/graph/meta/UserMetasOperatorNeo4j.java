/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.meta;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import guru.bubl.module.model.User;
import guru.bubl.module.model.graph.FriendlyResourcePojo;
import guru.bubl.module.model.graph.identification.IdentifierPojo;
import guru.bubl.module.model.meta.UserMetasOperator;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;

import static org.neo4j.driver.v1.Values.parameters;

public class UserMetasOperatorNeo4j implements UserMetasOperator {

    private Session session;
    private User user;

    @AssistedInject
    protected UserMetasOperatorNeo4j(
            Session session,
            @Assisted User user
    ) {
        this.session = session;
        this.user = user;
    }

    @Override
    public Set<IdentifierPojo> get() {
        Set<IdentifierPojo> userMetas = new HashSet<>();
        StatementResult rs = session.run(
                "MATCH (n:Meta{owner:$owner}) RETURN n.label as label, n.nb_references as nbReferences, n.uri as uri;",
                parameters(
                        "owner", user.username()
                )
        );
        while (rs.hasNext()) {
            Record record = rs.next();
            userMetas.add(
                    new IdentifierPojo(
                            record.get("nbReferences").asInt(),
                            new FriendlyResourcePojo(
                                    URI.create(record.get("uri").asString()),
                                    record.get("label").asString()
                            )
                    )
            );
        }
        return userMetas;
    }
}
