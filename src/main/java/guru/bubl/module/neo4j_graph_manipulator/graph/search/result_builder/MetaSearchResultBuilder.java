/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.search.result_builder;

import com.google.common.collect.ImmutableMap;
import guru.bubl.module.model.graph.FriendlyResourcePojo;
import guru.bubl.module.model.graph.graph_element.GraphElementPojo;
import guru.bubl.module.model.graph.graph_element.GraphElementType;
import guru.bubl.module.model.graph.ShareLevel;
import guru.bubl.module.model.graph.tag.TagPojo;
import guru.bubl.module.model.graph.fork.NbNeighborsPojo;
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
    private Set<ShareLevel> inShareLevels;

    public MetaSearchResultBuilder(Record row, String prefix, Set<ShareLevel> inShareLevels) {
        this.row = row;
        this.prefix = prefix;
        this.inShareLevels = inShareLevels;
    }

    @Override
    public GraphElementSearchResult build() {
        FriendlyResourcePojo friendlyResourcePojo = FriendlyResourceFromExtractorQueryRow.usingRowAndNodeKey(
                row,
                prefix
        ).build();
        NbNeighborsPojo nbNeighbors = buildNbNeighbors();
        TagPojo tagPojo = new TagPojo(
                URI.create(
                        row.get("n.external_uri").asString()
                ),
                new GraphElementPojo(
                        friendlyResourcePojo
                ),
                nbNeighbors
        );
        GraphElementPojo tagAsGraphElement = new GraphElementPojo(
                tagPojo.getGraphElement().getFriendlyResource(),
                ImmutableMap.of(
                        tagPojo.getExternalResourceUri(),
                        tagPojo
                )
        );
        GraphElementSearchResultPojo searchResult = new GraphElementSearchResultPojo(
                GraphElementType.Meta,
                tagAsGraphElement,
                getContext()
        );
        searchResult.getGraphElement().setColors(
                VertexFromExtractorQueryRow.getColors(
                        prefix,
                        row
                )
        );
        searchResult.setNbNeighbors(nbNeighbors);
        searchResult.setShareLevel(this.extractShareLevel());
        searchResult.setNbVisits(
                getNbVisits()
        );
        return searchResult;
    }

    @Override
    public Record getRow() {
        return row;
    }

    @Override
    public Set<ShareLevel> getInShareLevels() {
        return this.inShareLevels;
    }
}
