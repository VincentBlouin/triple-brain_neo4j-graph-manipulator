/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor;

import guru.bubl.module.common_utils.NoEx;
import guru.bubl.module.model.Image;
import guru.bubl.module.model.graph.FriendlyResourcePojo;
import guru.bubl.module.model.json.ImageJson;
import guru.bubl.module.neo4j_graph_manipulator.graph.Neo4jFriendlyResource;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.Neo4jUserGraph;
import guru.bubl.module.neo4j_graph_manipulator.graph.image.Neo4jImages;

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
                            nodeKey + "." + Neo4jImages.props.images
                    )
            );
        } catch (SQLException e) {
            return new HashSet<>();
        }


    }

    public String getLabel() throws SQLException {
        String labelKey = nodeKey + "." + Neo4jFriendlyResource.props.label + "";
        return row.getString(
                labelKey
        ) != null ? row.getString(labelKey) : "";
    }

    private String getComment() {
        try {
            return row.getString(
                    nodeKey + "." + Neo4jFriendlyResource.props.comment
            );
        } catch (SQLException e) {
            return "";
        }

    }

    private Long getLastModificationDate() throws SQLException {
        String key = nodeKey + "." + Neo4jFriendlyResource.props.last_modification_date.name();
        if (row.getString(key) == null) {
            return new Date().getTime();
        }
        return row.getLong(
                key
        );
    }

    private Long getCreationDate() throws SQLException {
        String key = nodeKey + "." + Neo4jFriendlyResource.props.creation_date.name();
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
                        nodeKey + "." + Neo4jUserGraph.URI_PROPERTY_NAME
                )
        );
    }

}
