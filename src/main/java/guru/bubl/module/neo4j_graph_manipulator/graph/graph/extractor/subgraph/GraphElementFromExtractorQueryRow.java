/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.subgraph;

import guru.bubl.module.model.graph.graph_element.GraphElementPojo;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.FriendlyResourceFromExtractorQueryRow;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.TagQueryBuilder;
import org.neo4j.driver.Record;

import java.net.URI;

public class GraphElementFromExtractorQueryRow {

    private Record record;
    private String key;
    private String identificationKey = TagQueryBuilder.TAG_QUERY_KEY;

    public static GraphElementFromExtractorQueryRow usingRowAndKey(
            Record record,
            String key
    ) {
        return new GraphElementFromExtractorQueryRow(
                record,
                key
        );
    }

    public static GraphElementFromExtractorQueryRow usingRowKeyAndIdentificationKey(
            Record record,
            String key,
            String identificationKey
    ) {
        return new GraphElementFromExtractorQueryRow(
                record,
                key,
                identificationKey
        );
    }

    protected GraphElementFromExtractorQueryRow(Record record, String key) {
        this.record = record;
        this.key = key;
    }

    protected GraphElementFromExtractorQueryRow(Record record, String key, String identificationKey) {
        this.record = record;
        this.key = key;
        this.identificationKey = identificationKey;
    }

    public GraphElementPojo build() {
        return new GraphElementPojo(
                FriendlyResourceFromExtractorQueryRow.usingRowAndNodeKey(
                        record,
                        key
                ).build(),
                TagsFromExtractorQueryRowAsArray.usingRowAndKey(
                        record,
                        identificationKey
                ).build(),
                getCopiedFromUri()
        );
    }

    private URI getCopiedFromUri() {
        String copiedFromKey = key + "." + "copied_from_uri";
        return record.get(copiedFromKey).asObject() == null ? null : URI.create(record.get(copiedFromKey).asString());
    }
}
