/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.image;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import guru.bubl.module.common_utils.NoExRun;
import org.neo4j.rest.graphdb.RestAPI;
import org.neo4j.rest.graphdb.query.QueryEngine;
import org.neo4j.rest.graphdb.util.QueryResult;
import guru.bubl.module.model.Image;
import guru.bubl.module.model.json.ImageJson;
import guru.bubl.module.neo4j_graph_manipulator.graph.Neo4jFriendlyResource;

import javax.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static guru.bubl.module.neo4j_graph_manipulator.graph.Neo4jRestApiUtils.map;

public class Neo4jImages {

    public enum props {
        images
    }

    protected RestAPI restApi;
    protected QueryEngine<Map<String, Object>> queryEngine;
    protected Neo4jFriendlyResource friendlyResource;

    @Inject
    protected Connection connection;

    @AssistedInject
    public Neo4jImages(
            RestAPI restApi,
            QueryEngine queryEngine,
            @Assisted Neo4jFriendlyResource friendlyResource
    ) {
        this.restApi = restApi;
        this.queryEngine = queryEngine;
        this.friendlyResource = friendlyResource;
    }

    public void addAll(Set<Image> images) {
        Set<Image> current = get();
        current.addAll(images);
        String query = String.format(
                "%sSET n.%s= {1} ",
                friendlyResource.queryPrefix(),
                props.images
        );
        NoExRun.wrap(() -> {
            PreparedStatement statement = connection.prepareStatement(
                    query
            );
            statement.setString(
                    1,
                    ImageJson.toJsonArray(
                            current
                    )
            );
            return statement.execute();
        }).get();
    }

    public Set<Image> get() {
        String query = String.format(
                "%sreturn n.%s as images",
                friendlyResource.queryPrefix(),
                props.images
        );
        return NoExRun.wrap(() -> {
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery(
                    query
            );
            rs.next();
            if (rs.getString("images") == null) {
                return new HashSet<Image>();
            }
            return ImageJson.fromJson(
                    rs.getString("images")
            );
        }).get();
    }

}
