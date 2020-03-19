package guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.subgraph;

import guru.bubl.module.model.graph.tag.TagPojo;
import org.neo4j.driver.v1.Record;

import java.net.URI;

public class TagFromExtractorQueryRow {

    private Record record;
    private String key;

    public static TagFromExtractorQueryRow usingRowAndKey(
            Record record,
            String key
    ) {
        return new TagFromExtractorQueryRow(
                record,
                key
        );
    }

    protected TagFromExtractorQueryRow(Record record, String key) {
        this.record = record;
        this.key = key;
    }

    public TagPojo build() {
        TagPojo tag = new TagPojo(
                getExternalUri(),
                GraphElementFromExtractorQueryRow.usingRowAndKey(
                        record,
                        key
                ).build()
        );
        if (record.get(key + ".nb_private_neighbors").asObject() != null) {
            tag.getNbNeighbors().setPrivate(
                    record.get(
                            key + ".nb_private_neighbors"
                    ).asInt()
            );
        }
        if (record.get(key + ".nb_friend_neighbors").asObject() != null) {
            tag.getNbNeighbors().setFriend(
                    record.get(
                            key + ".nb_friend_neighbors"
                    ).asInt()
            );
        }
        if (record.get(key + ".nb_public_neighbors").asObject() != null) {
            tag.getNbNeighbors().setPublic(
                    record.get(
                            key + ".nb_public_neighbors"
                    ).asInt()
            );
        }
        tag.setShareLevel(
                VertexFromExtractorQueryRow.getShareLevel(
                        key,
                        record
                )
        );
        tag.getGraphElement().setChildrenIndex(
                VertexFromExtractorQueryRow.getChildrenIndexes(
                        key,
                        record
                )
        );
        tag.getGraphElement().setColors(
                VertexFromExtractorQueryRow.getColors(
                        key,
                        record
                )
        );
        tag.getGraphElement().setFont(
                VertexFromExtractorQueryRow.getFont(
                        key,
                        record
                )
        );
        return tag;
    }

    private URI getExternalUri() {
        String externalUriKey = key + "." + "external_uri";
        return URI.create(record.get(externalUriKey).asString());
    }

}
