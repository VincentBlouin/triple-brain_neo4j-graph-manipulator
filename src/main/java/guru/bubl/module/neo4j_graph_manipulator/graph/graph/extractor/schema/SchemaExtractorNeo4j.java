/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.schema;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import guru.bubl.module.common_utils.NoEx;
import guru.bubl.module.model.graph.schema.SchemaPojo;
import guru.bubl.module.neo4j_graph_manipulator.graph.Relationships;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.FriendlyResourceQueryBuilder;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.IdentificationQueryBuilder;

import java.net.URI;
import java.sql.Connection;
import java.sql.ResultSet;

public class SchemaExtractorNeo4j {
    protected URI schemaUri;

    protected Connection connection;

    public static final String
            SCHEMA_QUERY_KEY = "s",
            PROPERTY_QUERY_KEY = "p",
            SCHEMA_IDENTIFICATION_QUERY_KEY = "id_s",
            SCHEMA_IDENTIFICATION_RELATION_QUERY_KEY = "id_r_s",
            PROPERTY_IDENTIFICATION_RELATION_QUERY_KEY = "id_r_p",
            PROPERTY_IDENTIFICATION_QUERY_KEY = "id_p";


    @AssistedInject
    protected SchemaExtractorNeo4j(
            Connection connection,
            @Assisted URI schemaUri
    ) {
        this.connection = connection;
        this.schemaUri = schemaUri;
    }

    public SchemaPojo load() {
        return NoEx.wrap(() -> {
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
        return "START " + SCHEMA_QUERY_KEY + "=node:node_auto_index('uri:" + schemaUri + "') " +
                "OPTIONAL MATCH ("+ SCHEMA_QUERY_KEY +")-[:" + Relationships.HAS_PROPERTY + "]->("+PROPERTY_QUERY_KEY+") " +
                "OPTIONAL MATCH ("+SCHEMA_QUERY_KEY+")-[" + SCHEMA_IDENTIFICATION_RELATION_QUERY_KEY + ":" + Relationships.IDENTIFIED_TO + "]->(" + SCHEMA_IDENTIFICATION_QUERY_KEY + ") " +
                "OPTIONAL MATCH ("+PROPERTY_QUERY_KEY+")-[" + PROPERTY_IDENTIFICATION_RELATION_QUERY_KEY + ":" + Relationships.IDENTIFIED_TO + "]->(" + PROPERTY_IDENTIFICATION_QUERY_KEY + ") " +
                "RETURN " +
                FriendlyResourceQueryBuilder.returnQueryPartUsingPrefix(SCHEMA_QUERY_KEY) +
                FriendlyResourceQueryBuilder.returnQueryPartUsingPrefix(PROPERTY_QUERY_KEY) +
                FriendlyResourceQueryBuilder.imageReturnQueryPart(SCHEMA_QUERY_KEY) +
                FriendlyResourceQueryBuilder.imageReturnQueryPart(PROPERTY_QUERY_KEY) +
                IdentificationQueryBuilder.identificationReturnQueryPartUsingKeysForIdentificationRelationAndAlias(
                        SCHEMA_IDENTIFICATION_QUERY_KEY,
                        SCHEMA_IDENTIFICATION_RELATION_QUERY_KEY
                ) +
                IdentificationQueryBuilder.identificationReturnQueryPartUsingKeysForIdentificationRelationAndAlias(
                        PROPERTY_IDENTIFICATION_QUERY_KEY,
                        PROPERTY_IDENTIFICATION_RELATION_QUERY_KEY
                ) +
                dummyReturnValueToAvoidFinishWithComma;
    }
}
