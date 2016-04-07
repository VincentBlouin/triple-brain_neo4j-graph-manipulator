/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.center_graph_element;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import guru.bubl.module.common_utils.NoExRun;
import guru.bubl.module.model.User;
import guru.bubl.module.model.center_graph_element.CenterGraphElementPojo;
import guru.bubl.module.model.center_graph_element.CenteredGraphElementsOperator;
import guru.bubl.module.model.graph.FriendlyResourcePojo;
import guru.bubl.module.model.graph.GraphElementPojo;
import guru.bubl.module.neo4j_graph_manipulator.graph.Neo4jFriendlyResource;

import java.net.URI;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.Set;

public class Neo4jCenterGraphElementsOperator implements CenteredGraphElementsOperator {

    private Connection connection;
    private User user;

    @AssistedInject
    protected Neo4jCenterGraphElementsOperator(
            Connection connection,
            @Assisted User user
    ) {
        this.connection = connection;
        this.user = user;
    }

    @Override
    public Set<CenterGraphElementPojo> getPublicAndPrivate() {
        return getPublicOnlyOrNot(
                false
        );
    }

    @Override
    public Set<CenterGraphElementPojo> getPublicOnly() {
        return getPublicOnlyOrNot(
                true
        );
    }

    private Set<CenterGraphElementPojo> getPublicOnlyOrNot(Boolean publicOnly) {
        String query = String.format(
                "START n=node:node_auto_index('" +
                        "owner:%s AND %s:* " +
                        (publicOnly ? "AND is_public:true" : "") +
                        "') " +
                        "return n.%s as numberOfVisits, n.%s as label, n.%s as uri;",
                user.username(),
                Neo4jCenterGraphElementOperator.props.number_of_visits,
                Neo4jCenterGraphElementOperator.props.number_of_visits,
                Neo4jFriendlyResource.props.label,
                Neo4jFriendlyResource.props.uri
        );
        Set<CenterGraphElementPojo> centerGraphElements = new HashSet<>();
        return NoExRun.wrap(() -> {
            ResultSet rs = connection.createStatement().executeQuery(
                    query
            );
            while (rs.next()) {
                centerGraphElements.add(
                        new CenterGraphElementPojo(
                                new Integer(rs.getString("numberOfVisits")),
                                new GraphElementPojo(new FriendlyResourcePojo(
                                        URI.create(rs.getString("uri")),
                                        rs.getString("label")
                                ))
                        )
                );
            }
            return centerGraphElements;
        }).get();
    }
}
