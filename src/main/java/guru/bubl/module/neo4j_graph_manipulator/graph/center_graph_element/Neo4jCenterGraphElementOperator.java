/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.center_graph_element;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import guru.bubl.module.common_utils.NoExRun;
import guru.bubl.module.model.center_graph_element.CenterGraphElementOperator;
import guru.bubl.module.model.graph.GraphElement;
import guru.bubl.module.neo4j_graph_manipulator.graph.Neo4jFriendlyResource;
import guru.bubl.module.neo4j_graph_manipulator.graph.Neo4jFriendlyResourceFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.Date;

public class Neo4jCenterGraphElementOperator implements CenterGraphElementOperator {

    public enum props {
        number_of_visits,
        last_center_date
    }

    private Connection connection;
    private Neo4jFriendlyResource neo4jFriendlyResource;

    @AssistedInject
    protected Neo4jCenterGraphElementOperator(
            Connection connection,
            Neo4jFriendlyResourceFactory friendlyResourceFactory,
            @Assisted GraphElement graphElement
    ) {
        this.connection = connection;
        this.neo4jFriendlyResource = friendlyResourceFactory.withUri(
                graphElement.uri()
        );
    }

    @Override
    public void incrementNumberOfVisits() {
        String query = String.format(
                "%s set n.%s= CASE WHEN n.%s is null THEN 1 ELSE n.%s + 1 END",
                neo4jFriendlyResource.queryPrefix(),
                props.number_of_visits,
                props.number_of_visits,
                props.number_of_visits
        );
        NoExRun.wrap(() -> connection.createStatement().execute(query)).get();
    }

    @Override
    public Integer getNumberOfVisits() {
        String query = String.format(
                "%s return n.%s as number;",
                neo4jFriendlyResource.queryPrefix(),
                props.number_of_visits
        );
        return NoExRun.wrap(() -> {
            ResultSet rs = connection.createStatement().executeQuery(query);
            rs.next();
            return new Integer(
                    rs.getString("number")
            );
        }).get();
    }

    @Override
    public void updateLastCenterDate() {
        String query = String.format(
                "%s set n.%s= %s",
                neo4jFriendlyResource.queryPrefix(),
                props.last_center_date,
                new Date().getTime()
        );
        NoExRun.wrap(() -> connection.createStatement().execute(query)).get();
    }

    @Override
    public Date getLastCenterDate() {
        String query = String.format(
                "%s return n.%s as date;",
                neo4jFriendlyResource.queryPrefix(),
                props.last_center_date
        );
        return NoExRun.wrap(() -> {
            ResultSet rs = connection.createStatement().executeQuery(query);
            rs.next();
            if(null == rs.getString("date")){
                return null;
            }
            return new Date(
                    rs.getLong("date")
            );
        }).get();
    }
}
