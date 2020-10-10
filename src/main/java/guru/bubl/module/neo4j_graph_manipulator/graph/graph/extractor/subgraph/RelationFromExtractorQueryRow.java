/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.subgraph;

import guru.bubl.module.model.graph.relation.Relation;
import guru.bubl.module.model.graph.relation.RelationPojo;
import org.neo4j.driver.Record;

public class RelationFromExtractorQueryRow {

    private Record row;
    private String key;


    public static RelationFromExtractorQueryRow usingRow(Record row) {
        return new RelationFromExtractorQueryRow(
                row
        );
    }

    protected RelationFromExtractorQueryRow(Record row) {
        this(
                row,
                SubGraphExtractorNeo4j.GRAPH_ELEMENT_QUERY_KEY
        );
    }

    protected RelationFromExtractorQueryRow(Record row, String key) {
        this.row = row;
        this.key = key;
    }

    public Relation build() {
        return init();
    }

    private RelationPojo init() {
        RelationPojo edge = new RelationPojo(
                GraphElementFromExtractorQueryRow.usingRowAndKey(
                        row, key
                ).build()
        );
        return edge;
    }
}
