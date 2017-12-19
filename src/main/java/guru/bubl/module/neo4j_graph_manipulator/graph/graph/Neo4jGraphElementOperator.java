/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.graph;

import com.google.common.reflect.TypeToken;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import guru.bubl.module.common_utils.NamedParameterStatement;
import guru.bubl.module.common_utils.NoExRun;
import guru.bubl.module.model.Image;
import guru.bubl.module.model.UserUris;
import guru.bubl.module.model.graph.*;
import guru.bubl.module.model.graph.identification.Identifier;
import guru.bubl.module.model.graph.identification.IdentifierPojo;
import guru.bubl.module.model.json.ImageJson;
import guru.bubl.module.model.json.JsonUtils;
import guru.bubl.module.neo4j_graph_manipulator.graph.*;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.identification.Neo4jIdentification;
import guru.bubl.module.neo4j_graph_manipulator.graph.image.Neo4jImages;
import guru.bubl.module.neo4j_graph_manipulator.graph.meta.Neo4jIdentificationFactory;
import org.neo4j.graphdb.Node;
import scala.util.parsing.json.JSON;

import java.net.URI;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import static guru.bubl.module.neo4j_graph_manipulator.graph.Neo4jRestApiUtils.map;

public class Neo4jGraphElementOperator implements GraphElementOperator, Neo4jOperator {

    public enum props {
        identifications,
        sort_date,
        move_date
    }

    protected Node node;
    protected Neo4jFriendlyResource friendlyResource;
    protected URI uri;
    protected Neo4jFriendlyResourceFactory friendlyResourceFactory;
    protected Connection connection;

    protected Neo4jIdentificationFactory identificationFactory;

    @AssistedInject
    protected Neo4jGraphElementOperator(
            Neo4jFriendlyResourceFactory friendlyResourceFactory,
            Connection connection,
            Neo4jIdentificationFactory identificationFactory,
            @Assisted Node node
    ) {
        friendlyResource = friendlyResourceFactory.withNode(
                node
        );
        this.friendlyResourceFactory = friendlyResourceFactory;
        this.identificationFactory = identificationFactory;
        this.connection = connection;
        this.node = node;
    }

    @AssistedInject
    protected Neo4jGraphElementOperator(
            Neo4jFriendlyResourceFactory friendlyResourceFactory,
            Connection connection,
            Neo4jIdentificationFactory identificationFactory,
            @Assisted URI uri
    ) {
        this.friendlyResource = friendlyResourceFactory.withUri(
                uri
        );
        this.identificationFactory = identificationFactory;
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
    public void setSortDate(Date sortDate, Date moveDate) {
        String query = String.format(queryPrefix() +
                        "SET " +
                        "n.%s=@%s, " +
                        "n.%s=@%s ",
                props.sort_date,
                props.sort_date,
                props.move_date,
                props.move_date
        );
        NoExRun.wrap(() -> {
            NamedParameterStatement statement = new NamedParameterStatement(
                    connection,
                    query
            );
            statement.setLong(
                    props.sort_date.name(),
                    sortDate.getTime()
            );
            statement.setLong(
                    props.move_date.name(),
                    moveDate.getTime()
            );
            return statement.execute();
        }).get();
    }

    @Override
    public Map<colorProps, String> getColors() {
        String query = queryPrefix() + "RETURN n.colors as colors";
        return NoExRun.wrap(() -> {
            ResultSet rs = connection.createStatement().executeQuery(
                    query
            );
            rs.next();
            String colorsStr = rs.getString("colors");
            if (colorsStr == null) {
                return new HashMap<colorProps, String>();
            }
            Map<colorProps, String> colors = JsonUtils.getGson().fromJson(
                    colorsStr,
                    new TypeToken<Map<colorProps, String>>() {
                    }.getType()
            );
            return colors;
        }).get();
    }

    @Override
    public void setColors(Map<colorProps, String> colors) {
        String query = queryPrefix()
                + "SET n.colors = @colors";
        NoExRun.wrap(() -> {
            NamedParameterStatement statement = new NamedParameterStatement(
                    connection, query
            );
            statement.setString("colors", JsonUtils.getGson().toJson(colors));
            return statement.execute();
        }).get();
    }

    @Override
    public void setChildrenIndex(String childrenIndex) {
        String query = queryPrefix()
                + "SET n.childrenIndexes = @childrenIndexes";
        NoExRun.wrap(() -> {
            NamedParameterStatement statement = new NamedParameterStatement(
                    connection, query
            );
            statement.setString("childrenIndexes", childrenIndex);
            return statement.execute();
        }).get();
    }

    @Override
    public String getChildrenIndex() {
        String query = queryPrefix() + "RETURN n.childrenIndexes as childrenIndexes";
        return NoExRun.wrap(() -> {
            ResultSet rs = connection.createStatement().executeQuery(
                    query
            );
            rs.next();
            String childrenIndexes = rs.getString("childrenIndexes");
            return childrenIndexes == null ? "" : childrenIndexes;
        }).get();
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
    public Map<URI, IdentifierPojo> addMeta(
            Identifier identification
    ) {
        ifIdentificationIsSelfThrowException(identification);
        IdentifierPojo identificationPojo;
        Boolean isIdentifyingToAnIdentification = UserUris.isUriOfAnIdentifier(
                identification.getExternalResourceUri()
        );
        if (isIdentifyingToAnIdentification) {
            identificationPojo = new IdentifierPojo(
                    identificationFactory.withUri(
                            identification.getExternalResourceUri()
                    ).getExternalResourceUri(),
                    new FriendlyResourcePojo(
                            identification.getExternalResourceUri()
                    )
            );
        } else {
            identificationPojo = new IdentifierPojo(
                    new UserUris(getOwnerUsername()).generateIdentificationUri(),
                    identification
            );
        }

        identificationPojo.setCreationDate(new Date().getTime());
        final Neo4jFriendlyResource neo4jFriendlyResource = friendlyResourceFactory.withUri(
                new UserUris(getOwnerUsername()).generateIdentificationUri()
        );
        Map<URI, IdentifierPojo> identifications = new HashMap<>();
        return NoExRun.wrap(() -> {
            NamedParameterStatement statement = new NamedParameterStatement(
                    connection,
                    AddIdentificationQueryBuilder.usingIdentificationForGraphElement(
                            identificationPojo, this
                    ).build()
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
                    identificationPojo.getExternalResourceUri().toString()
            );
            statement.setString(
                    "type",
                    GraphElementType.meta.name()
            );
            statement.setString(
                    "relationExternalUri",
                    identificationPojo.getRelationExternalResourceUri().toString()
            );
            statement.setLong(
                    Neo4jFriendlyResource.props.last_modification_date.name(),
                    new Date().getTime()
            );
            neo4jFriendlyResource.setNamedCreationProperties(
                    statement
            );
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                URI externalUri = URI.create(
                        rs.getString("external_uri")
                );
                identifications.put(
                        externalUri,
                        new IdentifierPojo(
                                externalUri,
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

                                        rs.getLong("creation_date"),
                                        rs.getLong("last_modification_date")
                                )
                        )
                );
            }
            return identifications;
        }).get();
    }

    @Override
    public void removeIdentification(Identifier identification) {
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
    public void remove() {
        removeAllIdentifications();
        friendlyResource.remove();
    }

    private void ifIdentificationIsSelfThrowException(Identifier identification) throws IllegalArgumentException {
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

    public GraphElementPojo pojoFromCreationProperties(Map<String, Object> creationProperties) {
        return new GraphElementPojo(
                friendlyResource.pojoFromCreationProperties(
                        creationProperties
                )
        );
    }

    @Override
    public void setNamedCreationProperties(NamedParameterStatement statement) throws SQLException {
        friendlyResource.setNamedCreationProperties(
                statement
        );
    }

    @Override
    public Map<URI, IdentifierPojo> getIdentifications() {
        String query = String.format(
                "%sMATCH n-[r:%s]->identification " +
                        "RETURN identification.uri as uri, " +
                        "identification.external_uri as external_uri, " +
                        "identification.%s as nbReferences, " +
                        "r.%s as r_x_u",
                queryPrefix(),
                Relationships.IDENTIFIED_TO,
                Neo4jIdentification.props.nb_references,
                Neo4jIdentification.props.relation_external_uri
        );
        Map<URI, IdentifierPojo> identifications = new HashMap<>();
        return NoExRun.wrap(() -> {
            ResultSet rs = connection.createStatement().executeQuery(query);
            while (rs.next()) {
                URI uri = URI.create(
                        rs.getString("uri")
                );
                URI externalUri = URI.create(
                        rs.getString("external_uri")
                );
                IdentifierPojo identification = new IdentifierPojo(
                        externalUri,
                        new Integer(rs.getString("nbReferences")),
                        new FriendlyResourcePojo(
                                uri
                        )
                );
                String relationExternalUriString = rs.getString("r_x_u");
                identification.setRelationExternalResourceUri(
                        relationExternalUriString == null ? Identifier.DEFAULT_IDENTIFIER_RELATION_EXTERNAL_URI :
                                URI.create(
                                        relationExternalUriString
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

    public void removeAllIdentifications() {
        NoExRun.wrap(() -> connection.createStatement().executeQuery(
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

    public GraphElementOperator forkUsingCreationPropertiesAndCache(GraphElementOperator clone, Map<String, Object> additionalCreateValues, GraphElement cache) {
        FriendlyResourcePojo original = new FriendlyResourcePojo(
                uri(),
                cache.label()
        );
        original.setComment(
                cache.comment()
        );
        Map<String, Object> createValues = map(
                Neo4jFriendlyResource.props.label.name(), original.label(),
                Neo4jFriendlyResource.props.comment.name(), original.comment()
        );
        createValues.putAll(
                additionalCreateValues
        );
        clone.createUsingInitialValues(
                createValues
        );
        clone.addMeta(
                new IdentifierPojo(
                        this.uri(),
                        original
                )
        );
        cache.getIdentifications().values().forEach(
                clone::addMeta
        );
        return clone;
    }
}
