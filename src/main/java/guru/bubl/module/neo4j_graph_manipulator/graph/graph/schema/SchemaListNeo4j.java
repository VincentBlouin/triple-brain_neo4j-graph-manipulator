/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.graph.schema;

import guru.bubl.module.model.UserUris;
import guru.bubl.module.model.graph.FriendlyResourcePojo;
import guru.bubl.module.model.graph.GraphElementPojo;
import guru.bubl.module.model.graph.schema.SchemaList;
import guru.bubl.module.model.graph.schema.SchemaPojo;
import guru.bubl.module.neo4j_graph_manipulator.graph.FriendlyResourceNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.Relationships;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;

import javax.inject.Inject;
import java.net.URI;
import java.util.*;

import static guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.schema.SchemaExtractorNeo4j.PROPERTY_QUERY_KEY;

public class SchemaListNeo4j implements SchemaList {

    @Inject
    Driver driver;

    @Override
    public Set<SchemaPojo> get() {
        String query = "MATCH (n:Schema) " +
                "OPTIONAL MATCH (n)-[:" + Relationships.HAS_PROPERTY + "]->(" + PROPERTY_QUERY_KEY + ") " +
                "RETURN n." + FriendlyResourceNeo4j.props.label + " as label, " +
                "n." + FriendlyResourceNeo4j.props.uri + " as uri, " +
                "COLLECT([" + PROPERTY_QUERY_KEY + "." + FriendlyResourceNeo4j.props.label + "])[0..10] as properties";

        Set<SchemaPojo> schemas = new HashSet<>();
        try (Session session = driver.session()) {
            StatementResult sr = session.run(
                    query
            );
            while (sr.hasNext()) {
                Record record = sr.next();
                List<List<String>> propertiesResult = (List) record.get("properties").asObject();
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
                                        record.get("uri").asString()
                                )
                        ),
                        properties
                );
                schema.setLabel(
                        record.get("label").asString()
                );
                schemas.add(
                        schema
                );
            }
            return schemas;
        }
    }
}
