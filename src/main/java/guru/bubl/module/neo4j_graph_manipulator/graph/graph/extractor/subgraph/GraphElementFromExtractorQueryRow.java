/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.subgraph;

import guru.bubl.module.model.graph.GraphElementPojo;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.FriendlyResourceFromExtractorQueryRow;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.IdentificationQueryBuilder;

import java.util.Map;

public class GraphElementFromExtractorQueryRow {

    private Map<String, Object> row;
    private String key;

    public static GraphElementFromExtractorQueryRow usingRowAndKey(Map<String, Object> row, String key) {
        return new GraphElementFromExtractorQueryRow(
                row,
                key
        );
    }

    protected GraphElementFromExtractorQueryRow(Map<String, Object> row, String key) {
        this.row = row;
        this.key = key;
    }

    public GraphElementPojo build() {
        return new GraphElementPojo(
                FriendlyResourceFromExtractorQueryRow.usingRowAndNodeKey(
                        row,
                        key
                ).build(),
                IdentificationsFromExtractorQueryRow.usingRowAndKey(
                        row,
                        IdentificationQueryBuilder.IDENTIFICATION_QUERY_KEY
                ).build()
        );
    }
}
