/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.search.result_builder;

import com.google.common.collect.ImmutableMap;
import guru.bubl.module.common_utils.NoExRun;
import guru.bubl.module.model.graph.FriendlyResourcePojo;
import guru.bubl.module.model.graph.GraphElementPojo;
import guru.bubl.module.model.graph.GraphElementType;
import guru.bubl.module.model.graph.identification.IdentifierPojo;
import guru.bubl.module.model.search.GraphElementSearchResult;
import guru.bubl.module.model.search.GraphElementSearchResultPojo;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.FriendlyResourceFromExtractorQueryRow;

import java.net.URI;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.HashSet;

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

        GraphElementPojo identifierAsGraphElement = new GraphElementPojo(
                identifierPojo.getFriendlyResource(),
                ImmutableMap.of(
                        identifierPojo.getExternalResourceUri(),
                        identifierPojo
                )
        );

        return new GraphElementSearchResultPojo(
                GraphElementType.meta,
                identifierAsGraphElement,
                new HashMap<>()
        );
    }

    @Override
    public ResultSet getRow() {
        return row;
    }
}
