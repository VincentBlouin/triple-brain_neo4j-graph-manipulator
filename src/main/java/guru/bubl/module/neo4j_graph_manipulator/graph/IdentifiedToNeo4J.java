/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph;

import guru.bubl.module.common_utils.NoExRun;
import guru.bubl.module.model.IdentifiedTo;
import guru.bubl.module.model.User;
import guru.bubl.module.model.graph.FriendlyResourcePojo;
import guru.bubl.module.model.graph.Identification;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.Neo4jIdentification;

import javax.inject.Inject;
import java.net.URI;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.Set;

public class IdentifiedToNeo4J implements IdentifiedTo {

    @Inject
    protected Connection connection;

    @Override
    public Set<FriendlyResourcePojo> getForIdentificationAndUser(
            Identification identification,
            User user
    ) {
        Set<FriendlyResourcePojo> related = new HashSet<>();
        String query = String.format(
                "START id=node:node_auto_index('%s:\"%s\" AND %s:%s') " +
                        "MATCH id<-[]-n " +
                "RETURN n.uri, n.label",
                Neo4jIdentification.props.external_uri,
                identification.getExternalResourceUri(),
                Neo4jFriendlyResource.props.owner,
                user.username()
        );
        return NoExRun.wrap(()->{
            ResultSet rs = connection.createStatement().executeQuery(query);
            while(rs.next()){
                related.add(
                        new FriendlyResourcePojo(
                                URI.create(
                                        rs.getString("n.uri")
                                ),
                                rs.getString("n.label")
                        )
                );
            }
            return related;
        }).get();
    }
}
