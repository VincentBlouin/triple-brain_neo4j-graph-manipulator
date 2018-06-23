/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.center_graph_element;

import com.google.gson.reflect.TypeToken;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import guru.bubl.module.common_utils.NoEx;
import guru.bubl.module.model.User;
import guru.bubl.module.model.center_graph_element.CenterGraphElementPojo;
import guru.bubl.module.model.center_graph_element.CenteredGraphElementsOperator;
import guru.bubl.module.model.graph.FriendlyResourcePojo;
import guru.bubl.module.model.graph.GraphElementPojo;
import guru.bubl.module.model.json.JsonUtils;
import guru.bubl.module.neo4j_graph_manipulator.graph.FriendlyResourceNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.identification.IdentificationNeo4j;

import java.net.URI;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class CenterGraphElementsOperatorNeo4j implements CenteredGraphElementsOperator {

    private Connection connection;
    private User user;

    @AssistedInject
    protected CenterGraphElementsOperatorNeo4j(
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
    public Set<CenterGraphElementPojo> getPublicOnlyOfType() {
        return getPublicOnlyOrNot(
                true
        );
    }

    @Override
    public void removeCenterGraphElements(Set<CenterGraphElementPojo> centers) {
        String uris = centers.stream()
                .map(center -> "'" + center.getGraphElement().uri().toString() + "'")
                .collect(Collectors.joining(", "));
        String query = String.format(
                "START n=node:node_auto_index('" +
                        "owner:%s" +
                        "') " +
                        "WHERE n.uri IN %s " +
                        "REMOVE n.last_center_date, n.number_of_visits",
                user.username(),
                "[" + uris + "]"
        );
        NoEx.wrap(() -> connection.createStatement().execute(
                query
        )).get();
    }

    private Set<CenterGraphElementPojo> getPublicOnlyOrNot(Boolean publicOnly) {
        String query = String.format(
                "START n=node:node_auto_index('" +
                        "owner:%s AND %s:* " +
                        (publicOnly ? "AND shareLevel:40" : "") +
                        "') " +
                        "return n.%s as context, n.%s as numberOfVisits, n.%s as lastCenterDate, n.%s as label, n.%s as uri, n.%s as nbReferences",
                user.username(),
                CenterGraphElementOperatorNeo4j.props.last_center_date,
                publicOnly ? "public_context" : "private_context",
                CenterGraphElementOperatorNeo4j.props.number_of_visits,
                CenterGraphElementOperatorNeo4j.props.last_center_date,
                FriendlyResourceNeo4j.props.label,
                FriendlyResourceNeo4j.props.uri,
                IdentificationNeo4j.props.nb_references
        );
        Set<CenterGraphElementPojo> centerGraphElements = new HashSet<>();
        return NoEx.wrap(() -> {
            ResultSet rs = connection.createStatement().executeQuery(
                    query
            );
            while (rs.next()) {
                Date lastCenterDate = null == rs.getString("lastCenterDate") ?
                        null :
                        new Date(rs.getLong("lastCenterDate"));
                Integer numberOfVisits = null == rs.getString("numberOfVisits") ?
                        null :
                        new Integer(rs.getString("numberOfVisits"));
                Integer nbReferences = null == rs.getString("nbReferences") ?
                        null :
                        new Integer(rs.getString("nbReferences"));
                centerGraphElements.add(
                        new CenterGraphElementPojo(
                                numberOfVisits,
                                lastCenterDate,
                                new GraphElementPojo(new FriendlyResourcePojo(
                                        URI.create(rs.getString("uri")),
                                        rs.getString("label")
                                )),
                                getContextFromRow(rs),
                                nbReferences
                        )
                );
            }
            return centerGraphElements;
        }).get();
    }

    private Map<URI, String> getContextFromRow(ResultSet row) throws SQLException {
        String contextStr = row.getString("context");
        if(null == contextStr){
            return new HashMap<>();
        }
        return JsonUtils.getGson().fromJson(
                contextStr,
                new TypeToken<Map<URI, String>>() {
                }.getType()
        );
    }
}
