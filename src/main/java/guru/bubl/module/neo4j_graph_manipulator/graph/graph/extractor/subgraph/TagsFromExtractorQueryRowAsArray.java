/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.subgraph;

import guru.bubl.module.model.graph.FriendlyResourcePojo;
import guru.bubl.module.model.graph.GraphElementPojo;
import guru.bubl.module.model.graph.ShareLevel;
import guru.bubl.module.model.graph.tag.TagPojo;
import guru.bubl.module.model.json.ImageJson;
import org.neo4j.driver.v1.Record;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TagsFromExtractorQueryRowAsArray {

    private Record record;
    private String key;

    public static TagsFromExtractorQueryRowAsArray usingRowAndKey(
            Record record,
            String key
    ) {
        return new TagsFromExtractorQueryRowAsArray(
                record,
                key
        );
    }

    protected TagsFromExtractorQueryRowAsArray(
            Record record,
            String key
    ) {
        this.record = record;
        this.key = key;
    }

    public Map<URI, TagPojo> build() {
        Map<URI, TagPojo> tags = new HashMap<>();
        if (!isInQuery()) {
            return tags;
        }
        for (List<Object> properties : getList()) {
            if (properties.get(0) == null) {
                return tags;
            }
            URI externalUri = URI.create(properties.get(0).toString());
            URI uri = URI.create(properties.get(1).toString());
            FriendlyResourcePojo friendlyResource = new FriendlyResourcePojo(
                    uri
            );
            friendlyResource.setLabel(
                    properties.get(2).toString()
            );
            friendlyResource.setComment(
                    (String) properties.get(3)
            );
            friendlyResource.setImages(
                    ImageJson.fromJson(
                            properties.get(4).toString()
                    )
            );
            friendlyResource.setCreationDate(
                    new Long(properties.get(7).toString())
            );
            if (properties.get(8) != null) {
                friendlyResource.setColors(
                        properties.get(8).toString()
                );
            }
            TagPojo tag = new TagPojo(
                    externalUri,
                    new Integer(properties.get(5).toString()),
                    new GraphElementPojo(
                            friendlyResource
                    )
            );
            ShareLevel shareLevel = ShareLevel.PRIVATE;
            if (properties.get(9) != null) {
                Long shareLevelLong = (Long) properties.get(9);
                shareLevel = ShareLevel.get(
                        shareLevelLong.intValue()
                );
            }
            tag.setShareLevel(
                    shareLevel
            );
            tag.setRelationExternalResourceUri(URI.create(
                    properties.get(6).toString()
            ));
            tags.put(
                    externalUri,
                    tag
            );
        }
        return tags;
    }

    private Boolean isInQuery() {
        return record.get(key).asObject() != null;
    }

    private List<List<Object>> getList() {
        return (List) record.get(key).asObject();
    }
}
