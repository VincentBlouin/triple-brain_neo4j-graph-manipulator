/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.search.result_builder;

import guru.bubl.module.common_utils.NoExRun;
import guru.bubl.module.model.graph.FriendlyResourcePojo;
import guru.bubl.module.model.graph.identification.IdentifierPojo;
import guru.bubl.module.model.search.GraphElementSearchResult;
import guru.bubl.module.model.search.IdentifierSearchResult;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.FriendlyResourceFromExtractorQueryRow;

import java.net.URI;
import java.sql.ResultSet;

public class IdentifierSearchResultBuilder implements SearchResultBuilder {

    private ResultSet row;
    private String prefix;

    public IdentifierSearchResultBuilder(ResultSet row, String prefix) {
        this.row = row;
        this.prefix = prefix;
    }

    @Override
    public GraphElementSearchResult build() {

        FriendlyResourcePojo friendlyResourcePojo = FriendlyResourceFromExtractorQueryRow.usingRowAndNodeKey(
                row,
                prefix
        ).build();
        IdentifierPojo identifierPojo = new IdentifierPojo(
                friendlyResourcePojo
        );
        NoExRun.wrap(() -> {
            identifierPojo.setExternalResourceUri(
                    URI.create(
                            row.getString("node.external_uri")
                    )
            );
            return identifierPojo.setNbRefences(
                    new Integer(
                            row.getString("node.nb_references")
                    )
            );
        }).get();
        return new IdentifierSearchResult(
                identifierPojo
        );
    }
}
