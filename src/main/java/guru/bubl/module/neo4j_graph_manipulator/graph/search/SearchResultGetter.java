/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.search;

import guru.bubl.module.common_utils.NoEx;
import guru.bubl.module.model.graph.GraphElementType;
import guru.bubl.module.model.search.GraphElementSearchResult;
import guru.bubl.module.neo4j_graph_manipulator.graph.search.result_builder.*;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.StatementResult;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class SearchResultGetter<ResultType extends GraphElementSearchResult> {

    public static final String nodePrefix = "n";

    private List<ResultType> searchResults = new ArrayList<>();

    private StatementResult result;


    public SearchResultGetter(StatementResult result) {
        this.result = result;
    }

    public List<ResultType> get() {
        return NoEx.wrap(() -> {
            while (result.hasNext()) {
                addResult(result.next());
            }
            return searchResults;
        }).get();
    }

    private void addResult(Record row) {
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

    private SearchResultBuilder getFromRow(Record record) {
        switch (nodeTypeInRow(record)) {
            case Vertex:
                return new VertexSearchResultBuilder(record, nodePrefix);
            case Edge:
                return new RelationSearchResultBuilder(record, nodePrefix);
            case Schema:
                return new SchemaSearchResultBuilder(record, nodePrefix);
            case Property:
                return new PropertySearchResultBuilder(record, nodePrefix);
            case Meta:
                return new MetaSearchResultBuilder(record, nodePrefix);
            default:
                return null;
        }
    }

    public static GraphElementType nodeTypeInRow(Record record) {
        List<String> types = (List) record.get("type").asList();
        GraphElementType type = null;
        for (String typeStr : types) {
            GraphElementType graphElementType = GraphElementType.valueOf(typeStr);
            if (!GraphElementType.commonTypes.contains(graphElementType)) {
                type = graphElementType;
            }
        }
        return type;
    }

    private Integer getNbReferenceInRow(ResultSet row) throws SQLException {
        String nbReferencesStr = row.getString("nb_references");
        return nbReferencesStr == null ? 0 : new Integer(nbReferencesStr);
    }
}
