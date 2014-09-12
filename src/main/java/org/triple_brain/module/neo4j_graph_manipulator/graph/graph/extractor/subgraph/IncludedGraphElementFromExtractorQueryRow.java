/*
 * Copyright Vincent Blouin under the Mozilla Public License 1.1
 */

package org.triple_brain.module.neo4j_graph_manipulator.graph.graph.extractor.subgraph;

import com.hp.hpl.jena.vocabulary.RDFS;

import java.net.URI;
import java.util.Map;

public class IncludedGraphElementFromExtractorQueryRow {

    private Map<String, Object> row;

    private String key;

    public IncludedGraphElementFromExtractorQueryRow(
            Map<String, Object> row,
            String key
    ) {
        this.row = row;
        this.key = key;
    }

    public Boolean isInRow() {
        return row.get(
                key + ".uri"
        ) != null;
    }

    public URI getUri() {
        return URI.create(
                row.get(
                        key + ".uri"
                ).toString()
        );
    }

    public String getLabel() {
        String labelKey = key+ ".`" + RDFS.label.getURI().toString() + "`";
        return row.get(
                labelKey
        ) != null ? row.get(labelKey).toString() : "";
    }

}
