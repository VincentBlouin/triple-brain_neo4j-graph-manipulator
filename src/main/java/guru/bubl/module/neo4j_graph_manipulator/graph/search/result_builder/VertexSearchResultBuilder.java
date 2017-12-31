/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.search.result_builder;

import guru.bubl.module.common_utils.NoEx;
import guru.bubl.module.model.graph.GraphElementType;
import guru.bubl.module.model.search.GraphElementSearchResult;
import guru.bubl.module.model.search.GraphElementSearchResultPojo;
import guru.bubl.module.neo4j_graph_manipulator.graph.center_graph_element.Neo4jCenterGraphElementOperator;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.subgraph.GraphElementFromExtractorQueryRow;

import java.sql.ResultSet;
import java.sql.SQLException;

public class VertexSearchResultBuilder implements SearchResultBuilder {

    private ResultSet row;
    private String prefix;

    public VertexSearchResultBuilder(ResultSet row, String prefix) {
        this.row = row;
        this.prefix = prefix;
    }

    @Override
    public GraphElementSearchResult build() {
        return NoEx.wrap(() -> {
            GraphElementSearchResultPojo searchResult = new GraphElementSearchResultPojo(
                    GraphElementType.vertex,
                    GraphElementFromExtractorQueryRow.usingRowAndKey(
                            row,
                            prefix
                    ).build(),
                    getContext()
            );
            searchResult.setNbVisits(
                    getNbVisits()
            );
            return searchResult;
        }).get();
    }

    @Override
    public ResultSet getRow() {
        return row;
    }

    private Integer getNbVisits() throws SQLException {
        String numberOfVisits = row.getString(
                prefix + "." + Neo4jCenterGraphElementOperator.props.number_of_visits.name()
        );
        return numberOfVisits == null ? 0 : new Integer(numberOfVisits);
    }

}
