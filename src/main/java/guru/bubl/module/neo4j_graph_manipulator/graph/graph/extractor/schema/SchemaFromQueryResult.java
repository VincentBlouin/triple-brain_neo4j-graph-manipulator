/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.schema;

import org.neo4j.rest.graphdb.util.QueryResult;
import guru.bubl.module.model.graph.GraphElementPojo;
import guru.bubl.module.model.graph.schema.SchemaPojo;
import guru.bubl.module.neo4j_graph_manipulator.graph.Neo4jFriendlyResource;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.subgraph.GraphElementFromExtractorQueryRow;

import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class SchemaFromQueryResult {
    private QueryResult<Map<String, Object>> result;
    private Map<URI, GraphElementPojo> properties = new HashMap<>();
    public SchemaFromQueryResult(QueryResult<Map<String, Object>> result) {
        this.result = result;
    }

    public SchemaPojo build() {
        Iterator<Map<String,Object>> iterator = result.iterator();
        Map<String,Object> row = iterator.next();
        GraphElementPojo schemaGraphElement = GraphElementFromExtractorQueryRow.usingRowAndKey(
                row ,
                "schema_node"
        ).build();
        buildOrUpdatePropertyInRow(row);
        while(iterator.hasNext()){
            buildOrUpdatePropertyInRow(
                    iterator.next()
            );
        }
        return new SchemaPojo(
                schemaGraphElement,
                properties
        );
    }

    private void buildOrUpdatePropertyInRow(Map<String, Object> row){
        GraphElementFromExtractorQueryRow extractor = GraphElementFromExtractorQueryRow.usingRowAndKey(
                row,
                "schema_property"
        );
        if(rowHasSchemaProperty(row)){
            URI uri = getPropertyUri(row);
            properties.put(
                    uri,
                    extractor.build()
            );
        }
    }

    private Boolean rowHasSchemaProperty(Map<String, Object> row) {
        return row.get("schema_property." + Neo4jFriendlyResource.props.uri) != null;
    }
    private URI getPropertyUri(Map<String, Object> row){
        return URI.create(
                row.get("schema_property." + Neo4jFriendlyResource.props.uri).toString()
        );
    }
}
