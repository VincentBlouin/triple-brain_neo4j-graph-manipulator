/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.schema;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import guru.bubl.module.common_utils.NoExRun;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.FriendlyResourceQueryBuilder;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.IdentificationQueryBuilder;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.subgraph.Neo4jSubGraphExtractor;
import org.neo4j.rest.graphdb.query.QueryEngine;
import org.neo4j.rest.graphdb.util.QueryResult;
import guru.bubl.module.model.graph.schema.SchemaPojo;
import guru.bubl.module.neo4j_graph_manipulator.graph.Relationships;

import javax.swing.plaf.nimbus.State;
import java.net.URI;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Map;

import static org.neo4j.helpers.collection.MapUtil.map;

public class Neo4jSchemaExtractor {
    protected URI schemaUri;

    protected Connection connection;

    @AssistedInject
    protected Neo4jSchemaExtractor(
            Connection connection,
            @Assisted URI schemaUri
    ) {
        this.connection = connection;
        this.schemaUri = schemaUri;
    }

    public SchemaPojo load() {
        return NoExRun.wrap(() -> {
            ResultSet rs = connection.createStatement().executeQuery(
                    buildQuery()
            );
            return new SchemaFromQueryResult(
                    rs
            ).build();
        }).get();
    }

    private String buildQuery() {
        String dummyReturnValueToAvoidFinishWithComma = "1";
        return "START schema_node=node:node_auto_index('uri:" + schemaUri + "') " +
                "OPTIONAL MATCH (schema_node)-[:" + Relationships.HAS_PROPERTY + "]->(schema_property) " +
                "OPTIONAL MATCH ()-[" + IdentificationQueryBuilder.IDENTIFICATION_RELATION_QUERY_KEY + ":" + Relationships.IDENTIFIED_TO + "]->(" + IdentificationQueryBuilder.IDENTIFICATION_QUERY_KEY + ") " +
                "RETURN " +
                FriendlyResourceQueryBuilder.returnQueryPartUsingPrefix("schema_node") +
                FriendlyResourceQueryBuilder.returnQueryPartUsingPrefix("schema_property") +
                FriendlyResourceQueryBuilder.imageReturnQueryPart("schema_node") +
                FriendlyResourceQueryBuilder.imageReturnQueryPart("schema_property") +
                IdentificationQueryBuilder.identificationReturnQueryPart() +
                dummyReturnValueToAvoidFinishWithComma;
    }
}
