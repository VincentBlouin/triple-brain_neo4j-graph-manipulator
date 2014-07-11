package org.triple_brain.module.neo4j_graph_manipulator.graph.graph.extractor;

import org.triple_brain.module.model.graph.IdentificationPojo;
import org.triple_brain.module.neo4j_graph_manipulator.graph.graph.Neo4jIdentification;

import java.net.URI;
import java.util.Map;

/*
* Copyright Mozilla Public License 1.1
*/
public class IdentificationFromExtractorQueryRow {

    private Map<String, Object> row;
    private String key;

    public static IdentificationFromExtractorQueryRow usingRowAndKey(Map<String, Object> row, String key) {
        return new IdentificationFromExtractorQueryRow(
                row,
                key
        );
    }

    protected IdentificationFromExtractorQueryRow(Map<String, Object> row, String key) {
        this.row = row;
        this.key = key;
    }

    public IdentificationPojo build() {
        FriendlyResourceFromExtractorQueryRow friendlyResourceFromExtractorQueryRow = FriendlyResourceFromExtractorQueryRow.usingRowAndNodeKey(
                row,
                key
        );
        return new IdentificationPojo(
                getExternalResourceUri(),
                friendlyResourceFromExtractorQueryRow.build()
        );
    }

    private URI getExternalResourceUri() {
        String uriKey = key + "." + Neo4jIdentification.props.external_uri;
        return URI.create(
                row.get(
                        uriKey
                ).toString()
        );
    }

}
