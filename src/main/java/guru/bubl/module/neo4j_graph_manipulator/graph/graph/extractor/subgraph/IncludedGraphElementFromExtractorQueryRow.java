/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.subgraph;

import guru.bubl.module.neo4j_graph_manipulator.graph.Neo4jFriendlyResource;

import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class IncludedGraphElementFromExtractorQueryRow {

    private ResultSet row;

    private String key;

    public IncludedGraphElementFromExtractorQueryRow(
            ResultSet row,
            String key
    ) {
        this.row = row;
        this.key = key;
    }


    public URI getUri() throws SQLException{
        return URI.create(
                row.getString(
                        key + ".uri"
                )
        );
    }


    public Boolean hasResult() throws SQLException{
        return row.getString(key) != null;

    }

    public List<List<String>> getList() throws SQLException{
        return (List) row.getObject(key);
    }

    public String getLabel() throws SQLException{
        String labelKey = key+ "." + Neo4jFriendlyResource.props.label;
        return row.getString(
                labelKey
        ) != null ? row.getString(labelKey) : "";
    }
}
