/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor;

import guru.bubl.module.common_utils.NoEx;
import guru.bubl.module.model.Image;
import guru.bubl.module.model.graph.FriendlyResourcePojo;
import guru.bubl.module.model.json.ImageJson;
import guru.bubl.module.neo4j_graph_manipulator.graph.FriendlyResourceNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.UserGraphNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.image.ImagesNeo4j;
import org.neo4j.driver.Record;

import java.net.URI;
import java.util.Date;
import java.util.Set;

public class FriendlyResourceFromExtractorQueryRow {

    private Record record;
    private String nodeKey;

    public static FriendlyResourceFromExtractorQueryRow usingRowAndNodeKey(
            Record record,
            String nodeKey
    ) {
        return new FriendlyResourceFromExtractorQueryRow(
                record,
                nodeKey
        );
    }

    public static FriendlyResourceFromExtractorQueryRow usingRowAndPrefix(
            Record record,
            String nodeKey
    ) {
        return new FriendlyResourceFromExtractorQueryRow(
                record,
                nodeKey
        );
    }

    protected FriendlyResourceFromExtractorQueryRow(Record record, String nodeKey) {
        this.record = record;
        this.nodeKey = nodeKey;
    }

    public FriendlyResourcePojo build() {
        return NoEx.wrap(() -> new FriendlyResourcePojo(
                getUri(),
                getLabel(),
                getImages(),
                getComment(),
                getCreationDate(),
                getLastModificationDate()
        )).get();
    }

    private Set<Image> getImages() {
        return ImageJson.fromJson(
                record.get(
                        nodeKey + "." + ImagesNeo4j.props.images
                ).asString()
        );
    }

    public String getLabel() {
        String labelKey = nodeKey + "." + FriendlyResourceNeo4j.props.label + "";
        return record.get(
                labelKey
        ).asObject() == null ? "" : record.get(labelKey).asString();
    }

    private String getComment() {
        String key = nodeKey + "." + FriendlyResourceNeo4j.props.comment;
        return record.get(key).asObject() == null ? "" : record.get(key).asString();
    }

    private Long getLastModificationDate() {
        String key = nodeKey + "." + FriendlyResourceNeo4j.props.last_modification_date.name();
        return record.get(key).asObject() == null ? new Date().getTime() : record.get(key).asLong();
    }

    private Long getCreationDate() {
        String key = nodeKey + "." + FriendlyResourceNeo4j.props.creation_date.name();
        return record.get(key).asObject() == null ? new Date().getTime() : record.get(key).asLong();
    }

    public URI getUri() {
        return URI.create(
                record.get(
                        nodeKey + "." + UserGraphNeo4j.URI_PROPERTY_NAME
                ).asString()
        );
    }

}
