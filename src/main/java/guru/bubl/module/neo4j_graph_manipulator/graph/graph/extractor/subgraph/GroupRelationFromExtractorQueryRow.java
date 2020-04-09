/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.subgraph;

import guru.bubl.module.model.graph.GraphElementPojo;
import guru.bubl.module.model.graph.group_relation.GroupRelationPojo;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.TagQueryBuilder;
import org.neo4j.driver.v1.Record;

import java.net.URI;

import static guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.subgraph.VertexFromExtractorQueryRow.*;

public class GroupRelationFromExtractorQueryRow {

    private Record row;

    private String keyPrefix;

    public static GroupRelationFromExtractorQueryRow withRowAndKeyPrefix(Record row, String keyPrefix) {
        return new GroupRelationFromExtractorQueryRow(
                row,
                keyPrefix
        );
    }

    public GroupRelationFromExtractorQueryRow(
            Record row,
            String keyPrefix
    ) {
        this.row = row;
        this.keyPrefix = keyPrefix;
    }

    public GroupRelationPojo build() {
        GroupRelationPojo groupRelationPojo = new GroupRelationPojo(
                new GraphElementPojo(
                        URI.create(
                                row.get(
                                        "n.uri"
                                ).asString()
                        )
                ),
                TagsFromExtractorQueryRowAsArray.usingRowAndKey(
                        row,
                        TagQueryBuilder.IDENTIFIER_QUERY_KEY
                ).build().values().iterator().next(),
                getNbNeighbors(row, keyPrefix),
                getShareLevel(keyPrefix, row)
        );
        groupRelationPojo.getGraphElement().setChildrenIndex(
                getChildrenIndexes(keyPrefix, row)
        );
        return groupRelationPojo;
    }

}
