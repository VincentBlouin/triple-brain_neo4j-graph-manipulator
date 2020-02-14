/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.search.result_builder;

import com.google.common.collect.ImmutableMap;
import guru.bubl.module.model.graph.FriendlyResourcePojo;
import guru.bubl.module.model.graph.GraphElementPojo;
import guru.bubl.module.model.graph.GraphElementType;
import guru.bubl.module.model.graph.ShareLevel;
import guru.bubl.module.model.graph.tag.TagPojo;
import guru.bubl.module.model.search.GraphElementSearchResult;
import guru.bubl.module.model.search.GraphElementSearchResultPojo;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.FriendlyResourceFromExtractorQueryRow;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.subgraph.VertexFromExtractorQueryRow;
import org.neo4j.driver.v1.Record;

import java.net.URI;
import java.util.Set;

public class MetaSearchResultBuilder implements SearchResultBuilder {

    private Record row;
    private String prefix;

    public MetaSearchResultBuilder(Record row, String prefix) {
        this.row = row;
        this.prefix = prefix;
    }

    @Override
    public GraphElementSearchResult build() {
        FriendlyResourcePojo friendlyResourcePojo = FriendlyResourceFromExtractorQueryRow.usingRowAndNodeKey(
                row,
                prefix
        ).build();
        TagPojo tagPojo = new TagPojo(
                new GraphElementPojo(
                        friendlyResourcePojo
                )
        );

        tagPojo.setExternalResourceUri(
                URI.create(
                        row.get("n.external_uri").asString()
                )
        );

        tagPojo.setNbRefences(
                row.get("nbReferences").asInt()
        );

        GraphElementPojo identifierAsGraphElement = new GraphElementPojo(
                tagPojo.getGraphElement().getFriendlyResource(),
                ImmutableMap.of(
                        tagPojo.getExternalResourceUri(),
                        tagPojo
                )
        );
        GraphElementSearchResultPojo searchResult = new GraphElementSearchResultPojo(
                GraphElementType.Meta,
                identifierAsGraphElement,
                getContext()
        );
        searchResult.getGraphElement().setColors(
                VertexFromExtractorQueryRow.getColors(
                        prefix,
                        row
                )
        );
        searchResult.setShareLevel(this.extractShareLevel());
        searchResult.setNbReferences(
                tagPojo.getNbReferences()
        );
        return searchResult;
    }

    @Override
    public Record getRow() {
        return row;
    }
}
