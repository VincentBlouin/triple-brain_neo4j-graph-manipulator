/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.search.result_builder;

import guru.bubl.module.common_utils.NoExRun;
import guru.bubl.module.model.search.GraphElementSearchResult;
import guru.bubl.module.model.search.IdentificationSearchResult;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.Neo4jIdentification;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.Neo4jUserGraph;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.subgraph.GraphElementFromExtractorQueryRow;

import java.net.URI;
import java.sql.ResultSet;

public class IdentificationSearchResultBuilder implements SearchResultBuilder {

    private ResultSet row;
    private String prefix;

    public IdentificationSearchResultBuilder(ResultSet row, String prefix) {
        this.row = row;
        this.prefix = prefix;
    }

    @Override
    public GraphElementSearchResult build() {
        return NoExRun.wrap(() -> new IdentificationSearchResult(
                GraphElementFromExtractorQueryRow.usingRowAndKey(
                        row,
                        prefix
                ).build(),
                "identification",
                URI.create(row.getString(
                        prefix + "." + Neo4jIdentification.props.external_uri
                )),
                new Integer(row.getString(
                        prefix + "." + Neo4jIdentification.props.nb_references
                ))
        )).get();
    }

}
