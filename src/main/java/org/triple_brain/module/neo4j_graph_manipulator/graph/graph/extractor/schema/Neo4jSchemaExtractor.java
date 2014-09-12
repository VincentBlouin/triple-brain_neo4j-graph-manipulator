/*
 * Copyright Vincent Blouin under the Mozilla Public License 1.1
 */

package org.triple_brain.module.neo4j_graph_manipulator.graph.graph.extractor.schema;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.neo4j.rest.graphdb.query.QueryEngine;
import org.neo4j.rest.graphdb.util.QueryResult;
import org.triple_brain.module.model.graph.schema.SchemaPojo;
import org.triple_brain.module.neo4j_graph_manipulator.graph.Relationships;
import org.triple_brain.module.neo4j_graph_manipulator.graph.graph.extractor.FriendlyResourceFromExtractorQueryRow;
import org.triple_brain.module.neo4j_graph_manipulator.graph.graph.extractor.FriendlyResourceQueryBuilder;
import org.triple_brain.module.neo4j_graph_manipulator.graph.graph.extractor.IdentificationQueryBuilder;

import java.net.URI;
import java.util.Map;

import static org.neo4j.helpers.collection.MapUtil.map;

public class Neo4jSchemaExtractor {
    protected QueryEngine<Map<String, Object>> queryEngine;
    protected URI schemaUri;

    @AssistedInject
    protected Neo4jSchemaExtractor(
            QueryEngine queryEngine,
            @Assisted URI schemaUri
    ) {
        this.queryEngine = queryEngine;
        this.schemaUri = schemaUri;
    }

    public SchemaPojo load() {
        QueryResult<Map<String, Object>> result = queryEngine.query(
                buildQuery(),
                map()
        );
        return new SchemaFromQueryResult(
                result
        ).build();
    }

    private String buildQuery() {
        String dummyReturnValueToAvoidFinishWithComma = "1";
        return "START schema_node=node:node_auto_index(uri='" + schemaUri + "') " +
                "OPTIONAL MATCH (schema_node)-[:" + Relationships.HAS_PROPERTY+ "]->(schema_property) " +
                "OPTIONAL MATCH (schema_property)-[:" + Relationships.IDENTIFIED_TO + "]->(schema_property_generic_identification) " +
                "OPTIONAL MATCH (schema_property)-[:" + Relationships.TYPE + "]->(schema_property_type) " +
                "OPTIONAL MATCH (schema_property)-[:" + Relationships.SAME_AS + "]->(schema_property_same_as) " +
                "RETURN " +
                FriendlyResourceQueryBuilder.returnQueryPartUsingPrefix("schema_node") +
                FriendlyResourceQueryBuilder.returnQueryPartUsingPrefix("schema_property") +
                IdentificationQueryBuilder.genericIdentificationReturnQueryPart("schema_property") +
                IdentificationQueryBuilder.typeReturnQueryPart("schema_property") +
                IdentificationQueryBuilder.sameAsReturnQueryPart("schema_property") +
                dummyReturnValueToAvoidFinishWithComma;
    }
}
