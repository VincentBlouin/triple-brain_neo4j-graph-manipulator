package org.triple_brain.module.neo4j_graph_manipulator.graph.graph.extractor;

import com.hp.hpl.jena.vocabulary.RDFS;
import org.triple_brain.module.model.Image;
import org.triple_brain.module.model.graph.FriendlyResourcePojo;
import org.triple_brain.module.model.json.ImageJson;
import org.triple_brain.module.neo4j_graph_manipulator.graph.Neo4jFriendlyResource;
import org.triple_brain.module.neo4j_graph_manipulator.graph.graph.Neo4jUserGraph;
import org.triple_brain.module.neo4j_graph_manipulator.graph.image.Neo4jImages;

import java.net.URI;
import java.util.*;

/*
* Copyright Mozilla Public License 1.1
*/
public class FriendlyResourceFromExtractorQueryRow {

    private Map<String, Object> row;
    private String nodeKey;

    public static FriendlyResourceFromExtractorQueryRow usingRowAndNodeKey(
            Map<String, Object> row,
            String nodeKey
    ) {
        return new FriendlyResourceFromExtractorQueryRow(
                row,
                nodeKey
        );
    }

    public static FriendlyResourceFromExtractorQueryRow usingRowAndPrefix(
            Map<String, Object> row,
            String nodeKey
    ) {
        return new FriendlyResourceFromExtractorQueryRow(
                row,
                nodeKey
        );
    }

    protected FriendlyResourceFromExtractorQueryRow(Map<String, Object> row, String nodeKey) {
        this.row = row;
        this.nodeKey = nodeKey;
    }

    public FriendlyResourcePojo build() {
        return new FriendlyResourcePojo(
                URI.create(
                        row.get(nodeKey + "." + Neo4jUserGraph.URI_PROPERTY_NAME).toString()
                ),
                getLabel(),
                getImages(),
                getComment(),
                getCreationDate(),
                getLastModificationDate()
        );
    }

    private Set<Image> getImages() {
        Object imagesValue = row.get(
                nodeKey + "." + Neo4jImages.props.images
        );
        if (imagesValue == null) {
            return new HashSet<>();
        }
        return ImageJson.fromJson(
                imagesValue.toString()
        );
    }

    private String getLabel() {
        String labelKey = nodeKey + ".`" + RDFS.label.getURI().toString() + "`";
        return row.get(
                labelKey
        ) != null ? row.get(labelKey).toString() : "";
    }

    private String getComment() {
        String commmentKey = nodeKey + ".`" + RDFS.comment.getURI().toString() + "`";
        return row.get(
                commmentKey
        ) != null ? row.get(commmentKey).toString() : "";
    }

    private Date getLastModificationDate() {
        return new Date((Long) row.get(
                nodeKey + "." + Neo4jFriendlyResource.props.last_modification_date.name()
        ));
    }

    private Date getCreationDate() {
        return new Date((Long) row.get(
                nodeKey + "." + Neo4jFriendlyResource.props.creation_date.name()
        ));
    }
}
