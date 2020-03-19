/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.search;

import guru.bubl.module.common_utils.NoEx;
import guru.bubl.module.model.graph.GraphElementType;
import guru.bubl.module.model.graph.ShareLevel;
import guru.bubl.module.model.search.GraphElementSearchResult;
import guru.bubl.module.neo4j_graph_manipulator.graph.search.result_builder.*;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.StatementResult;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class SearchResultGetter<ResultType extends GraphElementSearchResult> {

    public static final String nodePrefix = "n";

    private List<ResultType> searchResults = new ArrayList<>();

    private StatementResult result;
    private Set<ShareLevel> inShareLevels;

    public SearchResultGetter(StatementResult result, Set<ShareLevel> inShareLevels) {
        this.result = result;
        this.inShareLevels = inShareLevels;
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

    private SearchResultBuilder getFromRow(Record record) {
        switch (nodeTypeInRow(record)) {
            case Vertex:
                return new VertexSearchResultBuilder(record, nodePrefix, inShareLevels);
            case Edge:
                return new RelationSearchResultBuilder(record, nodePrefix);
            case Meta:
                return new MetaSearchResultBuilder(record, nodePrefix, inShareLevels);
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
}
