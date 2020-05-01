/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.subgraph;

import guru.bubl.module.model.graph.ShareLevel;
import guru.bubl.module.model.graph.fork.NbNeighborsPojo;
import guru.bubl.module.model.graph.vertex.Vertex;
import guru.bubl.module.model.graph.vertex.VertexPojo;
import org.neo4j.driver.v1.Record;

import java.util.List;

public class VertexFromExtractorQueryRow {

    private Record row;

    private String keyPrefix;

    public VertexFromExtractorQueryRow(
            Record row,
            String keyPrefix
    ) {
        this.row = row;
        this.keyPrefix = keyPrefix;
    }

    public Vertex build() {
        VertexPojo vertex = new VertexPojo(
                GraphElementFromExtractorQueryRow.usingRowAndKey(
                        row,
                        keyPrefix
                ).build(),
                getNbNeighbors(row, keyPrefix),
                getShareLevel(keyPrefix, row)
        );
        vertex.getGraphElement().setColors(
                getColors(keyPrefix, row)
        );
        vertex.getGraphElement().setFont(
                getFont(keyPrefix, row)
        );
        if (this.isPattern()) {
            vertex.setAsPattern();
        }
        return vertex;
    }

    public static NbNeighborsPojo getNbNeighbors(Record row, String keyPrefix) {
        NbNeighborsPojo nbNeighborsPojo = new NbNeighborsPojo();
        if (row.get(keyPrefix + ".nb_private_neighbors").asObject() != null) {
            nbNeighborsPojo.setPrivate(row.get(
                    keyPrefix + ".nb_private_neighbors"
            ).asInt());
        }
        if (row.get(keyPrefix + ".nb_friend_neighbors").asObject() != null) {
            nbNeighborsPojo.setFriend(row.get(
                    keyPrefix + ".nb_friend_neighbors"
            ).asInt());
        }
        if (row.get(keyPrefix + ".nb_public_neighbors").asObject() != null) {
            nbNeighborsPojo.setPublic(row.get(
                    keyPrefix + ".nb_public_neighbors"
            ).asInt());
        }
        return nbNeighborsPojo;
    }

    public static String getColors(String keyPrefix, Record row) {
        String key = keyPrefix + "." + "colors";
        if (row.get(key) == null) {
            return null;
        }
        return row.get(
                key
        ).asString();
    }

    public static String getFont(String keyPrefix, Record row) {
        String key = keyPrefix + "." + "font";
        if (row.get(key) == null) {
            return null;
        }
        return row.get(
                key
        ).asString();
    }

    public static ShareLevel getShareLevel(String keyPrefix, Record row) {
        return ShareLevel.get(row.get(keyPrefix + ".shareLevel").asInt());
    }

    private Boolean isPattern() {
        List<String> types = (List) row.get("type").asList();
        Boolean isPattern = false;
        for (String typeStr : types) {
            if (typeStr.equals("Pattern")) {
                isPattern = true;
            }
        }
        return isPattern;
    }
}
