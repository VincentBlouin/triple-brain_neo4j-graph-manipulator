/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.subgraph;

import com.sun.org.apache.xpath.internal.operations.Bool;
import guru.bubl.module.neo4j_graph_manipulator.graph.Neo4jFriendlyResource;

import java.net.URI;
import java.util.List;
import java.util.Map;

public class IncludedGraphElementFromExtractorQueryRow {

    private Map<String, Object> row;

    private String key;

    public IncludedGraphElementFromExtractorQueryRow(
            Map<String, Object> row,
            String key
    ) {
        this.row = row;
        this.key = key;
    }


    public URI getUri() {
        return URI.create(
                row.get(
                        key + ".uri"
                ).toString()
        );
    }


    public Boolean hasResult(){
        return row.get(key) != null;
    }

    public List<List<String>> getList(){
        return (List) row.get(key);
    }

    public String getLabel() {
        String labelKey = key+ "." + Neo4jFriendlyResource.props.label;
        return row.get(
                labelKey
        ) != null ? row.get(labelKey).toString() : "";
    }
}
