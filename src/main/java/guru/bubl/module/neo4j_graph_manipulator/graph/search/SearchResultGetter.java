/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.search;

import guru.bubl.module.common_utils.NoExRun;
import guru.bubl.module.model.graph.GraphElementType;
import guru.bubl.module.model.search.GraphElementSearchResult;
import guru.bubl.module.neo4j_graph_manipulator.graph.search.result_builder.*;

import javax.inject.Inject;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SearchResultGetter<ResultType extends GraphElementSearchResult> {

    public static final String nodePrefix = "node";

    private List<ResultType> searchResults = new ArrayList<>();

    Connection connection;

    private String query;

    public SearchResultGetter(String query, Connection connection){
        this.query = query;
        this.connection = connection;
    }

    public List<ResultType> get(){
        return NoExRun.wrap(() -> {
            ResultSet rs = connection.createStatement().executeQuery(query);
            while (rs.next()) {
                addResult(rs);
            }
            return searchResults;
        }).get();
    }

    private void addResult(ResultSet row) throws SQLException {
//            printRow(row);
        SearchResultBuilder searchResultBuilder = getFromRow(row);
        GraphElementSearchResult graphElementSearchResult = searchResultBuilder.build();
        searchResults.add(
                (ResultType) graphElementSearchResult
        );
    }

    private void printRow(Map<String, Object> row) {
        System.out.println("*************printing row*****************");
        for (String key : row.keySet()) {
            if (key.equals("related_nodes")) {
                List collection = (List) row.get(key);
                System.out.println(collection);

            } else {
                System.out.println(key + " " + row.get(key));
            }
        }
    }

    private SearchResultBuilder getFromRow(ResultSet row) throws SQLException{
        GraphElementType resultType = GraphElementType.valueOf(
                nodeTypeInRow(row)
        );
        switch (resultType) {
            case vertex:
                return new VertexSearchResultBuilder(row, nodePrefix);
            case edge:
                return new RelationSearchResultBuilder(row, nodePrefix);
            case schema:
                return new SchemaSearchResultBuilder(row, nodePrefix);
            case property:
                return new PropertySearchResultBuilder(row, nodePrefix);
        }
        throw new RuntimeException("result type " + resultType + " does not exist");
    }

    public static String nodeTypeInRow(ResultSet row) throws SQLException{
        return row.getString("type");
    }
}
