/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.graph.schema;

import guru.bubl.module.common_utils.NoEx;
import guru.bubl.module.model.UserUris;
import guru.bubl.module.model.graph.FriendlyResourcePojo;
import guru.bubl.module.model.graph.GraphElementPojo;
import guru.bubl.module.model.graph.GraphElementType;
import guru.bubl.module.model.graph.schema.SchemaList;
import guru.bubl.module.model.graph.schema.SchemaPojo;
import guru.bubl.module.neo4j_graph_manipulator.graph.Neo4jFriendlyResource;
import guru.bubl.module.neo4j_graph_manipulator.graph.Relationships;

import javax.inject.Inject;
import java.net.URI;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.*;

import static guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.schema.Neo4jSchemaExtractor.PROPERTY_QUERY_KEY;

public class Neo4jSchemaList implements SchemaList {

    @Inject
    Connection connection;

    @Override
    public Set<SchemaPojo> get() {
        String query = String.format(
                "START n=node:node_auto_index('%s:%s') " +
                        "OPTIONAL MATCH (n)-[:" + Relationships.HAS_PROPERTY + "]->(" + PROPERTY_QUERY_KEY + ") " +
                        "RETURN n." + Neo4jFriendlyResource.props.label + " as label, " +
                        "n." + Neo4jFriendlyResource.props.uri + " as uri, " +
                        "COLLECT([" + PROPERTY_QUERY_KEY + "." + Neo4jFriendlyResource.props.label + "])[0..10] as properties",
                Neo4jFriendlyResource.props.type,
                GraphElementType.schema
        );
        Set<SchemaPojo> schemas = new HashSet<>();
        return NoEx.wrap(() -> {
            ResultSet rs = connection.prepareStatement(query).executeQuery();
            while (rs.next()) {
                List<List<String>> propertiesResult = (List) rs.getObject("properties");
                Map<URI, GraphElementPojo> properties = new HashMap<>();
                for (List<String> propertyResult : propertiesResult) {
                    String propertyLabel = propertyResult.get(0);
                    URI dummyUri = UserUris.dummyUniqueUri();
                    properties.put(
                            dummyUri,
                            new GraphElementPojo(
                                    new FriendlyResourcePojo(dummyUri, propertyLabel)
                            )
                    );
                }
                SchemaPojo schema = new SchemaPojo(
                        new GraphElementPojo(
                                URI.create(
                                        rs.getString("uri")
                                )
                        ),
                        properties
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
