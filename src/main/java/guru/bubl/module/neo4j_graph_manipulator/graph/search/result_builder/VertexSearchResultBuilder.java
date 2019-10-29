/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.search.result_builder;

import guru.bubl.module.model.graph.GraphElementType;
import guru.bubl.module.model.search.GraphElementSearchResult;
import guru.bubl.module.model.search.GraphElementSearchResultPojo;
import guru.bubl.module.neo4j_graph_manipulator.graph.center_graph_element.CenterGraphElementOperatorNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.subgraph.GraphElementFromExtractorQueryRow;
import org.neo4j.driver.v1.Record;

public class VertexSearchResultBuilder implements SearchResultBuilder {

    private Record row;
    private String prefix;

    public VertexSearchResultBuilder(Record row, String prefix) {
        this.row = row;
        this.prefix = prefix;
    }

    @Override
    public GraphElementSearchResult build() {
        GraphElementSearchResultPojo searchResult = new GraphElementSearchResultPojo(
                GraphElementType.Vertex,
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
    }

    @Override
    public Record getRow() {
        return row;
    }

    private Integer getNbVisits() {
        return row.get("nbVisits").asObject() == null ?
                0 : row.get("nbVisits").asInt();
    }

}
