/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.subgraph;

import guru.bubl.module.model.graph.FriendlyResourcePojo;
import guru.bubl.module.model.graph.IdentificationPojo;
import guru.bubl.module.model.graph.IdentificationType;
import guru.bubl.module.model.json.ImageJson;

import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IdentificationsFromExtractorQueryRow {

    private ResultSet row;
    private String key;

    public static IdentificationsFromExtractorQueryRow usingRowAndKey(
            ResultSet row,
            String key
    ) {
        return new IdentificationsFromExtractorQueryRow(
                row,
                key
        );
    }

    protected IdentificationsFromExtractorQueryRow(
            ResultSet row,
            String key
    ) {
        this.row = row;
        this.key = key;
    }

    public Map<URI, IdentificationPojo> build() throws SQLException{
        Map<URI, IdentificationPojo> identifications = new HashMap<>();
        if(!isInQuery()){
            return identifications;
        }
        for (List<String> properties : getList()) {
            if (properties.get(0) == null) {
                return identifications;
            }
            URI externalUri = URI.create(properties.get(0));
            URI uri = URI.create(properties.get(1));
            FriendlyResourcePojo friendlyResource = new FriendlyResourcePojo(
                    uri
            );
            friendlyResource.setLabel(
                    properties.get(2)
            );
            friendlyResource.setComment(
                    properties.get(3)
            );
            friendlyResource.setImages(
                    ImageJson.fromJson(
                            properties.get(4)
                    )
            );
            IdentificationPojo identification = new IdentificationPojo(
                    externalUri,
                    friendlyResource
            );
            identification.setType(IdentificationType.valueOf(
                    properties.get(5)
            ));
            identifications.put(
                    externalUri,
                    identification
            );
        }
        return identifications;
    }

    private Boolean isInQuery() throws SQLException{
        try{
            row.getString(key);
            return true;
        }catch(SQLException e){
            return false;
        }
    }

    private List<List<String>> getList() throws SQLException{
        return (List) row.getObject(key);
    }
}
