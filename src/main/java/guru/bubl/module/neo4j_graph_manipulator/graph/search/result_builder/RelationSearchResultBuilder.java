/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.search.result_builder;

import guru.bubl.module.common_utils.NoEx;
import guru.bubl.module.model.graph.GraphElementType;
import guru.bubl.module.model.search.GraphElementSearchResult;
import guru.bubl.module.model.search.GraphElementSearchResultPojo;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.subgraph.GraphElementFromExtractorQueryRow;

import java.sql.ResultSet;

public class RelationSearchResultBuilder implements SearchResultBuilder {

    private ResultSet row;
    private String prefix;

    public RelationSearchResultBuilder(ResultSet row, String prefix) {
        this.row = row;
        this.prefix = prefix;
    }

    @Override
    public GraphElementSearchResult build() {
        return NoEx.wrap(() -> new GraphElementSearchResultPojo(
                GraphElementType.edge,
                GraphElementFromExtractorQueryRow.usingRowAndKey(
                        row,
                        prefix
                ).build(),
                getContext()
        )).get();
    }

    @Override
    public ResultSet getRow() {
        return row;
    }
}
