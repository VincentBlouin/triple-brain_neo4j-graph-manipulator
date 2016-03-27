/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.graph.schema;

import guru.bubl.module.common_utils.NoExRun;
import guru.bubl.module.model.graph.GraphElementType;
import guru.bubl.module.model.graph.schema.Schema;
import guru.bubl.module.model.graph.schema.SchemaList;
import guru.bubl.module.model.graph.schema.SchemaPojo;
import guru.bubl.module.neo4j_graph_manipulator.graph.Neo4jFriendlyResource;

import javax.inject.Inject;
import java.net.URI;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.Set;

public class Neo4jSchemaList implements SchemaList{

    @Inject
    Connection connection;

    @Override
    public Set<SchemaPojo> get() {
        String query = String.format(
                "START n=node:node_auto_index('%s:%s') " +
                "RETURN n." + Neo4jFriendlyResource.props.label + " as label, " +
                "n." + Neo4jFriendlyResource.props.uri + " as uri",
                Neo4jFriendlyResource.props.type,
                GraphElementType.schema
        );
        Set<SchemaPojo> schemas = new HashSet<>();
        return NoExRun.wrap(() -> {
            ResultSet rs = connection.prepareStatement(query).executeQuery();
            while(rs.next()){
                SchemaPojo schema = new SchemaPojo(
                        URI.create(
                                rs.getString("uri")
                        )
                );
                schema.setLabel(
                        rs.getString("label")
                );
                schemas.add(
                        schema
                );
            }
            return schemas;
        }).get();
    }
}
