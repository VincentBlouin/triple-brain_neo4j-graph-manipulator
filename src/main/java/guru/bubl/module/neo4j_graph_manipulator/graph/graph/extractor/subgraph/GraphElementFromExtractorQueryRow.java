/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.subgraph;

import guru.bubl.module.model.graph.GraphElementPojo;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.FriendlyResourceFromExtractorQueryRow;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.IdentificationQueryBuilder;

import javax.xml.transform.Result;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

public class GraphElementFromExtractorQueryRow {

    private ResultSet row;
    private String key;

    public static GraphElementFromExtractorQueryRow usingRowAndKey(
            ResultSet row,
            String key
    ) {
        return new GraphElementFromExtractorQueryRow(
                row,
                key
        );
    }

    protected GraphElementFromExtractorQueryRow(ResultSet row, String key) {
        this.row = row;
        this.key = key;
    }

    public GraphElementPojo build() throws SQLException{
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
