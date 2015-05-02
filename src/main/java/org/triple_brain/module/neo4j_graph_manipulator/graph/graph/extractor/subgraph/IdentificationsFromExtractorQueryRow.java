/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package org.triple_brain.module.neo4j_graph_manipulator.graph.graph.extractor.subgraph;

import org.triple_brain.module.model.graph.IdentificationPojo;
import org.triple_brain.module.model.json.IdentificationJson;
import org.triple_brain.module.neo4j_graph_manipulator.graph.graph.Neo4jGraphElementOperator;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class IdentificationsFromExtractorQueryRow {

    private Map<String, Object> row;
    private String key;

    public static IdentificationsFromExtractorQueryRow usingRowAndKey(Map<String, Object> row, String key) {
        return new IdentificationsFromExtractorQueryRow(
                row,
                key
        );
    }

    protected IdentificationsFromExtractorQueryRow(Map<String, Object> row, String key) {
        this.row = row;
        this.key = key;
    }

    public Map<URI, IdentificationPojo> build() {
        Object identificationsValue = row.get(
                key + "." + Neo4jGraphElementOperator.props.identifications
        );
        if (identificationsValue == null) {
            return new HashMap<URI, IdentificationPojo>();
        }
        return IdentificationJson.fromJson(
                identificationsValue.toString()
        );
    }
}
