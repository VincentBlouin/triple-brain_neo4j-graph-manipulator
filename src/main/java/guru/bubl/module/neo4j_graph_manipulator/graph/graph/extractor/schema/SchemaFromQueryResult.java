/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.schema;

import guru.bubl.module.model.graph.GraphElementPojo;
import guru.bubl.module.model.graph.schema.SchemaPojo;
import guru.bubl.module.neo4j_graph_manipulator.graph.FriendlyResourceNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.subgraph.GraphElementFromExtractorQueryRow;

import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class SchemaFromQueryResult {
    private ResultSet result;
    private Map<URI, GraphElementPojo> properties = new HashMap<>();
    public SchemaFromQueryResult(ResultSet result) {
        this.result = result;
    }

    public SchemaPojo build() throws SQLException{
        result.next();
        GraphElementPojo schemaGraphElement = GraphElementFromExtractorQueryRow.usingRowKeyAndIdentificationKey(
                result,
                SchemaExtractorNeo4j.SCHEMA_QUERY_KEY,
                SchemaExtractorNeo4j.SCHEMA_IDENTIFICATION_QUERY_KEY
        ).build();
        buildOrUpdatePropertyInRow(result);
        while(result.next()){
            buildOrUpdatePropertyInRow(
                    result
            );
        }
        return new SchemaPojo(
                schemaGraphElement,
                properties
        );
    }

    private void buildOrUpdatePropertyInRow(ResultSet row) throws SQLException{
        GraphElementFromExtractorQueryRow extractor = GraphElementFromExtractorQueryRow.usingRowKeyAndIdentificationKey(
                row,
                SchemaExtractorNeo4j.PROPERTY_QUERY_KEY,
                SchemaExtractorNeo4j.PROPERTY_IDENTIFICATION_QUERY_KEY
        );
        if(rowHasSchemaProperty(row)){
            URI uri = getPropertyUri(row);
            properties.put(
                    uri,
                    extractor.build()
            );
        }
    }

    private Boolean rowHasSchemaProperty(ResultSet row) throws SQLException{
        return row.getString(
                SchemaExtractorNeo4j.PROPERTY_QUERY_KEY + "." + FriendlyResourceNeo4j.props.uri
        ) != null;
    }

    private URI getPropertyUri(ResultSet row)throws SQLException{
        return URI.create(
                row.getString(
                        SchemaExtractorNeo4j.PROPERTY_QUERY_KEY + "." + FriendlyResourceNeo4j.props.uri
                )
        );
    }
}
