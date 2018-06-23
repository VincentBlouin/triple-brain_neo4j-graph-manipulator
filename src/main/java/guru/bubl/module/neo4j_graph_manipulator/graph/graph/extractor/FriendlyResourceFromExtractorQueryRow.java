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

import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class FriendlyResourceFromExtractorQueryRow {

    private ResultSet row;
    private String nodeKey;

    public static FriendlyResourceFromExtractorQueryRow usingRowAndNodeKey(
            ResultSet row,
            String nodeKey
    ) {
        return new FriendlyResourceFromExtractorQueryRow(
                row,
                nodeKey
        );
    }

    public static FriendlyResourceFromExtractorQueryRow usingRowAndPrefix(
            ResultSet row,
            String nodeKey
    ) {
        return new FriendlyResourceFromExtractorQueryRow(
                row,
                nodeKey
        );
    }

    protected FriendlyResourceFromExtractorQueryRow(ResultSet row, String nodeKey) {
        this.row = row;
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
        try {
            return ImageJson.fromJson(
                    row.getString(
                            nodeKey + "." + ImagesNeo4j.props.images
                    )
            );
        } catch (SQLException e) {
            return new HashSet<>();
        }


    }

    public String getLabel() throws SQLException {
        String labelKey = nodeKey + "." + FriendlyResourceNeo4j.props.label + "";
        return row.getString(
                labelKey
        ) != null ? row.getString(labelKey) : "";
    }

    private String getComment() {
        try {
            return row.getString(
                    nodeKey + "." + FriendlyResourceNeo4j.props.comment
            );
        } catch (SQLException e) {
            return "";
        }

    }

    private Long getLastModificationDate() throws SQLException {
        String key = nodeKey + "." + FriendlyResourceNeo4j.props.last_modification_date.name();
        if (row.getString(key) == null) {
            return new Date().getTime();
        }
        return row.getLong(
                key
        );
    }

    private Long getCreationDate() throws SQLException {
        String key = nodeKey + "." + FriendlyResourceNeo4j.props.creation_date.name();
        if (row.getString(key) == null) {
            return new Date().getTime();
        }
        return row.getLong(
                key
        );
    }

    public URI getUri() throws SQLException {
        return URI.create(
                row.getString(
                        nodeKey + "." + UserGraphNeo4j.URI_PROPERTY_NAME
                )
        );
    }

}
