/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.schema;

import guru.bubl.module.model.graph.GraphElementPojo;
import guru.bubl.module.model.graph.schema.SchemaPojo;
import guru.bubl.module.neo4j_graph_manipulator.graph.FriendlyResourceNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.subgraph.GraphElementFromExtractorQueryRow;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.StatementResult;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class SchemaFromQueryResult {
    private StatementResult rs;
    private Map<URI, GraphElementPojo> properties = new HashMap<>();

    public SchemaFromQueryResult(StatementResult rs) {
        this.rs = rs;
    }

    public SchemaPojo build() {
        Boolean firstRow = true;
        GraphElementPojo schemaGraphElement = null;
        while (rs.hasNext()) {
            Record record = rs.next();
            if (firstRow) {
                schemaGraphElement = GraphElementFromExtractorQueryRow.usingRowKeyAndIdentificationKey(
                        record,
                        SchemaExtractorNeo4j.SCHEMA_QUERY_KEY,
                        SchemaExtractorNeo4j.SCHEMA_IDENTIFICATION_QUERY_KEY
                ).build();
                firstRow = false;
            }
            buildOrUpdatePropertyInRow(record);
            buildOrUpdatePropertyInRow(
                    record
            );
        }
        return new SchemaPojo(
                schemaGraphElement,
                properties
        );
    }

    private void buildOrUpdatePropertyInRow(Record record) {
        GraphElementFromExtractorQueryRow extractor = GraphElementFromExtractorQueryRow.usingRowKeyAndIdentificationKey(
                record,
                SchemaExtractorNeo4j.PROPERTY_QUERY_KEY,
                SchemaExtractorNeo4j.PROPERTY_IDENTIFICATION_QUERY_KEY
        );
        if (rowHasSchemaProperty(record)) {
            URI uri = getPropertyUri(record);
            properties.put(
                    uri,
                    extractor.build()
            );
        }
    }

    private Boolean rowHasSchemaProperty(Record record) {
        return record.get(
                SchemaExtractorNeo4j.PROPERTY_QUERY_KEY + "." + FriendlyResourceNeo4j.props.uri
        ).asObject() != null;
    }

    private URI getPropertyUri(Record record) {
        return URI.create(
                record.get(
                        SchemaExtractorNeo4j.PROPERTY_QUERY_KEY + "." + FriendlyResourceNeo4j.props.uri
                ).asString()
        );
    }
}
