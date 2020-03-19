/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.search.result_builder;

import guru.bubl.module.model.graph.GraphElementType;
import guru.bubl.module.model.graph.ShareLevel;
import guru.bubl.module.model.search.GraphElementSearchResult;
import guru.bubl.module.model.search.GraphElementSearchResultPojo;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.subgraph.GraphElementFromExtractorQueryRow;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.subgraph.VertexFromExtractorQueryRow;
import org.neo4j.driver.v1.Record;

import java.util.List;
import java.util.Set;

public class VertexSearchResultBuilder implements SearchResultBuilder {

    private Record row;
    private String prefix;
    private Set<ShareLevel> inShareLevels;

    public VertexSearchResultBuilder(Record row, String prefix, Set<ShareLevel> inShareLevels) {
        this.row = row;
        this.prefix = prefix;
        this.inShareLevels = inShareLevels;
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
        searchResult.getGraphElement().setColors(
                VertexFromExtractorQueryRow.getColors(
                        prefix,
                        row
                )
        );
        searchResult.setNbVisits(
                getNbVisits()
        );
        searchResult.setNbNeighbors(
                buildNbNeighbors()
        );
        searchResult.setShareLevel(this.extractShareLevel());
        searchResult.setIsPattern(
                isPattern()
        );
        return searchResult;
    }

    @Override
    public Record getRow() {
        return row;
    }

    private Boolean isPattern() {
        List<String> types = (List) row.get("type").asList();
        Boolean isPattern = false;
        for (String typeStr : types) {
            GraphElementType graphElementType = GraphElementType.valueOf(typeStr);
            if (graphElementType == GraphElementType.Pattern) {
                isPattern = true;
            }
        }
        return isPattern;
    }

    @Override
    public Set<ShareLevel> getInShareLevels() {
        return this.inShareLevels;
    }

}
