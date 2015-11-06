/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.graph;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import guru.bubl.module.common_utils.NamedParameterStatement;
import guru.bubl.module.common_utils.NoExRun;
import guru.bubl.module.model.Image;
import guru.bubl.module.model.UserUris;
import guru.bubl.module.model.graph.*;
import guru.bubl.module.model.json.ImageJson;
import guru.bubl.module.neo4j_graph_manipulator.graph.Neo4jFriendlyResource;
import guru.bubl.module.neo4j_graph_manipulator.graph.Neo4jFriendlyResourceFactory;
import guru.bubl.module.neo4j_graph_manipulator.graph.Neo4jOperator;
import guru.bubl.module.neo4j_graph_manipulator.graph.Relationships;
import guru.bubl.module.neo4j_graph_manipulator.graph.image.Neo4jImages;
import org.neo4j.graphdb.Node;

import java.net.URI;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import static guru.bubl.module.neo4j_graph_manipulator.graph.Neo4jRestApiUtils.map;

public class Neo4jGraphElementOperator implements GraphElementOperator, Neo4jOperator {

    public enum props {
        identifications
    }

    protected Node node;
    protected Neo4jFriendlyResource friendlyResource;
    protected URI uri;
    protected Neo4jFriendlyResourceFactory friendlyResourceFactory;

    protected Connection connection;

    @AssistedInject
    protected Neo4jGraphElementOperator(
            Neo4jFriendlyResourceFactory friendlyResourceFactory,
            Connection connection,
            @Assisted Node node
    ) {
        friendlyResource = friendlyResourceFactory.withNode(
                node
        );
        this.friendlyResourceFactory = friendlyResourceFactory;
        this.connection = connection;
        this.node = node;
    }

    @AssistedInject
    protected Neo4jGraphElementOperator(
            Neo4jFriendlyResourceFactory friendlyResourceFactory,
            Connection connection,
            @Assisted URI uri
    ) {
        this.friendlyResource = friendlyResourceFactory.withUri(
                uri
        );
        this.connection = connection;
        this.friendlyResourceFactory = friendlyResourceFactory;
    }

    @Override
    public Date creationDate() {
        return friendlyResource.creationDate();
    }

    @Override
    public Date lastModificationDate() {
        return friendlyResource.lastModificationDate();
    }

    @Override
    public String getOwnerUsername() {
        return friendlyResource.getOwnerUsername();
    }

    public void updateLastModificationDate() {
        friendlyResource.updateLastModificationDate();
    }

    @Override
    public URI uri() {
        return friendlyResource.uri();
    }

    @Override
    public String label() {
        return friendlyResource.label();
    }

    @Override
    public void label(String label) {
        friendlyResource.label(
                label
        );
    }

    @Override
    public Set<Image> images() {
        return friendlyResource.images();
    }

    @Override
    public Boolean gotImages() {
        return friendlyResource.gotImages();
    }

    @Override
    public String comment() {
        return friendlyResource.comment();
    }

    @Override
    public void comment(String comment) {
        friendlyResource.comment(
                comment
        );
    }

    @Override
    public Boolean gotComments() {
        return friendlyResource.gotComments();
    }

    @Override
    public void addImages(Set<Image> images) {
        friendlyResource.addImages(
                images
        );
    }

    @Override
    public boolean hasLabel() {
        return friendlyResource.hasLabel();
    }

    @Override
    public IdentificationPojo addGenericIdentification(Identification genericIdentification) throws IllegalArgumentException {
        return addIdentificationUsingType(
                genericIdentification,
                IdentificationType.generic
        );
    }

    @Override
    public void create() {
        createUsingInitialValues(
                map()
        );
    }

    @Override
    public void createUsingInitialValues(Map<String, Object> values) {
        friendlyResource.createUsingInitialValues(values);
    }

    @Override
    public Map<URI, IdentificationPojo> getGenericIdentifications() {
        return getIdentificationsUsingRelation(
                IdentificationType.generic
        );
    }

    @Override
    public IdentificationPojo addSameAs(Identification sameAs) throws IllegalArgumentException {
        return addIdentificationUsingType(
                sameAs,
                IdentificationType.same_as
        );
    }

    private IdentificationPojo addIdentificationUsingType(
            Identification identification,
            IdentificationType identificationType
    ) {
        ifIdentificationIsSelfThrowException(identification);
        IdentificationPojo identificationPojo = new IdentificationPojo(
                new UserUris(getOwnerUsername()).generateIdentificationUri(),
                identification
        );
        identificationPojo.setCreationDate(new Date());
        identificationPojo.setType(
                identificationType
        );
        Boolean isOwnerOfIdentification = UserUris.ownerUserNameFromUri(
                identification.getExternalResourceUri()
        ).equals(getOwnerUsername());
        final Neo4jFriendlyResource neo4jFriendlyResource = friendlyResourceFactory.withUri(
                new UserUris(getOwnerUsername()).generateIdentificationUri()
        );
        final String queryPrefix = this.friendlyResource.queryPrefix();

        String query = String.format(
                "%sMERGE (f {%s: @external_uri, %s: @owner}) " +
                        "ON CREATE SET f.uri = @uri, " +
                        "f.%s=@type, " +
                        "f.%s=@label, " +
                        "f.%s=@comment, " +
                        "f.%s=@images, " +
                        "f.%s=@%s, " +
                        "f.%s=timestamp(), " +
                        "f.%s=timestamp(), " +
                        "f.%s=0 " +
                        "CREATE UNIQUE n-[r:%s]->f%s " +
                        "SET r.type=@type,%s " +
                        "f.%s=f.%s + %s, " +
                        Neo4jFriendlyResource.LAST_MODIFICATION_QUERY_PART +
                        "RETURN f.uri as uri, " +
                        "f.external_uri as external_uri, " +
                        "f.%s as label, " +
                        "f.%s as comment, " +
                        "f.%s as images, " +
                        "f.%s as creation_date, " +
                        "f.%s as last_modification_date, " +
                        "f.%s as nbReferences",
                isOwnerOfIdentification ?
                        String.format(
                                queryPrefix + ", i=node:node_auto_index(\"uri:%s\") ",
                                identification.getExternalResourceUri()
                        ) : queryPrefix,
                Neo4jIdentification.props.external_uri,
                Neo4jFriendlyResource.props.owner,
                Neo4jFriendlyResource.props.type,
                Neo4jFriendlyResource.props.label,
                Neo4jFriendlyResource.props.comment,
                Neo4jImages.props.images,
                Neo4jFriendlyResource.props.type,
                Neo4jFriendlyResource.props.type,
                Neo4jFriendlyResource.props.creation_date,
                Neo4jFriendlyResource.props.last_modification_date,
                Neo4jIdentification.props.nb_references,
                Relationships.IDENTIFIED_TO,
                isOwnerOfIdentification ?
                        String.format(
                                ", i-[r2:%s]->f ",
                                Relationships.IDENTIFIED_TO
                        ) : " ",
                isOwnerOfIdentification ?
                        String.format(
                                " r2.type='%s', ",
                                IdentificationType.generic
                        ) : " ",
                Neo4jIdentification.props.nb_references,
                Neo4jIdentification.props.nb_references,
                isOwnerOfIdentification ? "2" : "1",
                Neo4jFriendlyResource.props.label,
                Neo4jFriendlyResource.props.comment,
                Neo4jImages.props.images,
                Neo4jFriendlyResource.props.creation_date,
                Neo4jFriendlyResource.props.last_modification_date,
                Neo4jIdentification.props.nb_references
        );
        return NoExRun.wrap(() -> {
            NamedParameterStatement statement = new NamedParameterStatement(
                    connection,
                    query
            );
            statement.setString(
                    "label",
                    identification.label()
            );
            statement.setString(
                    "comment",
                    identification.comment()
            );
            statement.setString(
                    Neo4jImages.props.images.name(),
                    ImageJson.toJsonArray(identification.images())
            );
            statement.setString(
                    "external_uri",
                    identification.getExternalResourceUri().toString()
            );
            statement.setString(
                    "type",
                    identificationType.name()
            );
            statement.setLong(
                    Neo4jFriendlyResource.props.last_modification_date.name(),
                    new Date().getTime()
            );
            neo4jFriendlyResource.setNamedCreationProperties(
                    statement
            );
            ResultSet rs = statement.executeQuery();
            rs.next();
            return new IdentificationPojo(
                    URI.create(
                            rs.getString("external_uri")
                    ),
                    new Integer(rs.getString("nbReferences")),
                    new FriendlyResourcePojo(
                            URI.create(
                                    rs.getString("uri")
                            ),
                            rs.getString("label") == null ?
                                    "" : rs.getString("label"),
                            rs.getString("images") == null ?
                                    new HashSet<>() : ImageJson.fromJson(rs.getString("images")),
                            rs.getString("comment") == null ? "" : rs.getString("comment"),
                            new Date(
                                    rs.getLong("creation_date")
                            ),
                            new Date(
                                    rs.getLong("last_modification_date")
                            )
                    )
            );
        }).get();
    }

    @Override
    public Map<URI, IdentificationPojo> getSameAs() {
        return getIdentificationsUsingRelation(
                IdentificationType.same_as
        );
    }

    @Override
    public IdentificationPojo addType(Identification type) throws IllegalArgumentException {
        return addIdentificationUsingType(
                type,
                IdentificationType.type
        );
    }

    @Override
    public void removeIdentification(Identification identification) {
        String query = String.format(
                "%s MATCH n-[r:%s]->(i {%s:'%s'}) " +
                        "DELETE r " +
                        "SET i.%s=i.%s -1, " +
                        Neo4jFriendlyResource.LAST_MODIFICATION_QUERY_PART +
                        "RETURN i.uri as uri",
                queryPrefix(),
                Relationships.IDENTIFIED_TO,
                Neo4jFriendlyResource.props.uri.name(),
                identification.uri().toString(),
                Neo4jIdentification.props.nb_references,
                Neo4jIdentification.props.nb_references
        );
        NoExRun.wrap(() -> {
            NamedParameterStatement statement = new NamedParameterStatement(
                    connection,
                    query
            );
            statement.setLong(
                    Neo4jFriendlyResource.props.last_modification_date.name(),
                    new Date().getTime()
            );
            return statement.executeQuery();
        }).get();
    }

    @Override
    public Map<URI, IdentificationPojo> getAdditionalTypes() {
        return getIdentificationsUsingRelation(
                IdentificationType.type
        );
    }

    @Override
    public void remove() {
        removeAllIdentifications();
        friendlyResource.remove();
    }

    private Map<URI, IdentificationPojo> getIdentificationsUsingRelation(IdentificationType identificationType) {
        String query = String.format(
                "%sMATCH n-[r:%s]->identification WHERE r.type='%s' " +
                        "RETURN identification.uri as uri, " +
                        "identification.external_uri as external_uri",
                queryPrefix(),
                Relationships.IDENTIFIED_TO,
                identificationType
        );
        Map<URI, IdentificationPojo> identifications = new HashMap<>();
        return NoExRun.wrap(() -> {
            ResultSet rs = connection.createStatement().executeQuery(
                    query
            );
            while (rs.next()) {
                URI uri = URI.create(
                        rs.getString("uri")
                );
                URI externalUri = URI.create(
                        rs.getString("external_uri")
                );
                identifications.put(
                        externalUri,
                        new IdentificationPojo(
                                externalUri,
                                new FriendlyResourcePojo(
                                        uri
                                )
                        )
                );
            }
            return identifications;
        }).get();
    }

    private void ifIdentificationIsSelfThrowException(Identification identification) throws IllegalArgumentException {
        if (identification.getExternalResourceUri().equals(this.uri())) {
            throw new IllegalArgumentException(
                    "identification cannot be the same"
            );
        }
    }

    @Override
    public boolean equals(Object graphElementToCompare) {
        return friendlyResource.equals(graphElementToCompare);
    }

    @Override
    public int hashCode() {
        return friendlyResource.hashCode();
    }

    @Override
    public String queryPrefix() {
        return friendlyResource.queryPrefix();
    }

    @Override
    public Node getNode() {
        if (null == node) {
            node = friendlyResource.getNode();
        }
        return node;
    }

    @Override
    public Map<String, Object> addCreationProperties(Map<String, Object> map) {
        return friendlyResource.addCreationProperties(
                map
        );
    }

    @Override
    public void setNamedCreationProperties(NamedParameterStatement statement) throws SQLException {
        friendlyResource.setNamedCreationProperties(
                statement
        );
    }

    @Override
    public Map<URI, IdentificationPojo> getIdentifications() {
        String query = String.format(
                "%sMATCH n-[r:%s]->identification " +
                        "RETURN identification.uri as uri, " +
                        "identification.external_uri as external_uri, " +
                        "identification.%s as nbReferences, " +
                        "r.type as type",
                queryPrefix(),
                Relationships.IDENTIFIED_TO,
                Neo4jIdentification.props.nb_references
        );
        Map<URI, IdentificationPojo> identifications = new HashMap<>();
        return NoExRun.wrap(() -> {
            ResultSet rs = connection.createStatement().executeQuery(query);
            while (rs.next()) {
                URI uri = URI.create(
                        rs.getString("uri")
                );
                URI externalUri = URI.create(
                        rs.getString("external_uri")
                );
                IdentificationPojo identification = new IdentificationPojo(
                        externalUri,
                        new Integer(rs.getString("nbReferences")),
                        new FriendlyResourcePojo(
                                uri
                        )
                );
                identification.setType(
                        IdentificationType.valueOf(
                                rs.getString("type")
                        )
                );
                identifications.put(
                        externalUri,
                        identification
                );
            }
            return identifications;
        }).get();
    }

    public void removeAllIdentifications(){
        NoExRun.wrap(()-> connection.createStatement().executeQuery(
                String.format(
                        "%s MATCH n-[r:%s]->i " +
                                "DELETE r " +
                                "SET i.%s=i.%s -1 ",
                        queryPrefix(),
                        Relationships.IDENTIFIED_TO,
                        Neo4jIdentification.props.nb_references,
                        Neo4jIdentification.props.nb_references
                )
        )).get();
    }
}
