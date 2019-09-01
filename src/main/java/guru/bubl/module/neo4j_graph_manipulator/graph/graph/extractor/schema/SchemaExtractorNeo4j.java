/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.schema;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import guru.bubl.module.model.graph.schema.SchemaPojo;
import guru.bubl.module.neo4j_graph_manipulator.graph.Relationships;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.FriendlyResourceQueryBuilder;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.IdentificationQueryBuilder;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;

import java.net.URI;

import static org.neo4j.driver.v1.Values.parameters;

public class SchemaExtractorNeo4j {
    protected URI schemaUri;

    protected Session session;

    public static final String
            SCHEMA_QUERY_KEY = "s",
            PROPERTY_QUERY_KEY = "p",
            SCHEMA_IDENTIFICATION_QUERY_KEY = "id_s",
            SCHEMA_IDENTIFICATION_RELATION_QUERY_KEY = "id_r_s",
            PROPERTY_IDENTIFICATION_RELATION_QUERY_KEY = "id_r_p",
            PROPERTY_IDENTIFICATION_QUERY_KEY = "id_p";


    @AssistedInject
    protected SchemaExtractorNeo4j(
            Session session,
            @Assisted URI schemaUri
    ) {
        this.session = session;
        this.schemaUri = schemaUri;
    }

    public SchemaPojo load() {
        StatementResult rs = session.run(
                buildQuery(),
                parameters(
                        "uri",
                        schemaUri.toString()
                )
        );
        return new SchemaFromQueryResult(
                rs
        ).build();
    }

    private String buildQuery() {
        String dummyReturnValueToAvoidFinishWithComma = "1";
        return "MATCH (s{uri:$uri}) " +
                "OPTIONAL MATCH (s)-[:" + Relationships.HAS_PROPERTY + "]->(p) " +
                "OPTIONAL MATCH (s)-[" + SCHEMA_IDENTIFICATION_RELATION_QUERY_KEY + ":" + Relationships.IDENTIFIED_TO + "]->(" + SCHEMA_IDENTIFICATION_QUERY_KEY + ") " +
                "OPTIONAL MATCH (p)-[" + PROPERTY_IDENTIFICATION_RELATION_QUERY_KEY + ":" + Relationships.IDENTIFIED_TO + "]->(" + PROPERTY_IDENTIFICATION_QUERY_KEY + ") " +
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
