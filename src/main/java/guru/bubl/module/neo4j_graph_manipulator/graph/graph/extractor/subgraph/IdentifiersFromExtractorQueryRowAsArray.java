/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.subgraph;

import guru.bubl.module.model.graph.FriendlyResourcePojo;
import guru.bubl.module.model.graph.identification.IdentifierPojo;
import guru.bubl.module.model.json.ImageJson;

import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IdentifiersFromExtractorQueryRowAsArray {

    private ResultSet row;
    private String key;

    public static IdentifiersFromExtractorQueryRowAsArray usingRowAndKey(
            ResultSet row,
            String key
    ) {
        return new IdentifiersFromExtractorQueryRowAsArray(
                row,
                key
        );
    }

    protected IdentifiersFromExtractorQueryRowAsArray(
            ResultSet row,
            String key
    ) {
        this.row = row;
        this.key = key;
    }

    public Map<URI, IdentifierPojo> build() throws SQLException {
        Map<URI, IdentifierPojo> identifications = new HashMap<>();
        if (!isInQuery()) {
            return identifications;
        }
        for (List<Object> properties : getList()) {
            if (properties.get(0) == null) {
                return identifications;
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
            IdentifierPojo identification = new IdentifierPojo(
                    externalUri,
                    new Integer(properties.get(5).toString()),
                    friendlyResource
            );
            identification.setRelationExternalResourceUri(URI.create(
                    properties.get(6).toString()
            ));
            identifications.put(
                    externalUri,
                    identification
            );
        }
        return identifications;
    }

    private Boolean isInQuery() throws SQLException {
        try {
            row.getString(key);
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    private List<List<Object>> getList() throws SQLException {
        return (List) row.getObject(key);
    }
}
