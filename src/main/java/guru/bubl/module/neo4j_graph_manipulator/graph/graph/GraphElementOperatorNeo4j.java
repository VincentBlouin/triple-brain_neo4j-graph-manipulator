/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.graph;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import guru.bubl.module.common_utils.NamedParameterStatement;
import guru.bubl.module.common_utils.NoEx;
import guru.bubl.module.model.Image;
import guru.bubl.module.model.UserUris;
import guru.bubl.module.model.graph.*;
import guru.bubl.module.model.graph.identification.Identifier;
import guru.bubl.module.model.graph.identification.IdentifierPojo;
import guru.bubl.module.model.json.ImageJson;
import guru.bubl.module.neo4j_graph_manipulator.graph.*;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.identification.IdentificationNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.image.ImagesNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.meta.IdentificationFactoryNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.search.GraphIndexerNeo4j;
import org.neo4j.graphdb.Node;

import java.net.URI;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static guru.bubl.module.neo4j_graph_manipulator.graph.RestApiUtilsNeo4j.map;

public class GraphElementOperatorNeo4j implements GraphElementOperator, OperatorNeo4j {

    public enum props {
        identifications,
        sort_date,
        move_date
    }

    protected Node node;
    protected FriendlyResourceNeo4j friendlyResource;
    protected FriendlyResourceFactoryNeo4j friendlyResourceFactory;
    protected Connection connection;

    protected IdentificationFactoryNeo4j identificationFactory;

    protected GraphElementOperatorFactory graphElementOperatorFactory;

    public static String incrementNbFriendsOrPublicQueryPart(ShareLevel shareLevel, String variableName, String prefix) {
        return incrementOrDecrementNbFriendsOrPublicQueryPart(
                shareLevel,
                variableName,
                prefix,
                false
        );
    }

    public static String decrementNbFriendsOrPublicQueryPart(ShareLevel shareLevel, String variableName, String prefix) {
        return incrementOrDecrementNbFriendsOrPublicQueryPart(
                shareLevel,
                variableName,
                prefix,
                true
        );
    }

    private static String incrementOrDecrementNbFriendsOrPublicQueryPart(ShareLevel shareLevel, String variableName, String prefix, Boolean decrement) {
        String queryPart = "";
        if (shareLevel == ShareLevel.FRIENDS) {
            queryPart = prefix + "%s.nb_friend_neighbors = %s.nb_friend_neighbors " + (decrement ? "-" : "+") + "1 ";
        } else if (shareLevel.isPublic()) {
            queryPart = prefix + "%s.nb_public_neighbors = %s.nb_public_neighbors " + (decrement ? "-" : "+") + "1 ";
        } else {
            return queryPart;
        }
        return String.format(
                queryPart,
                variableName,
                variableName
        );
    }

    @AssistedInject
    protected GraphElementOperatorNeo4j(
            FriendlyResourceFactoryNeo4j friendlyResourceFactory,
            Connection connection,
            IdentificationFactoryNeo4j identificationFactory,
            GraphElementOperatorFactory graphElementOperatorFactory,
            @Assisted Node node
    ) {
        friendlyResource = friendlyResourceFactory.withNode(
                node
        );
        this.graphElementOperatorFactory = graphElementOperatorFactory;
        this.friendlyResourceFactory = friendlyResourceFactory;
        this.identificationFactory = identificationFactory;
        this.connection = connection;
        this.node = node;
    }

    @AssistedInject
    protected GraphElementOperatorNeo4j(
            FriendlyResourceFactoryNeo4j friendlyResourceFactory,
            Connection connection,
            IdentificationFactoryNeo4j identificationFactory,
            GraphElementOperatorFactory graphElementOperatorFactory,
            @Assisted URI uri
    ) {
        this.friendlyResource = friendlyResourceFactory.withUri(
                uri
        );
        this.identificationFactory = identificationFactory;
        this.connection = connection;
        this.friendlyResourceFactory = friendlyResourceFactory;
        this.graphElementOperatorFactory = graphElementOperatorFactory;
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
        NoEx.wrap(() -> {
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
    public String getColors() {
        String query = queryPrefix() + "RETURN n.colors as colors";
        return NoEx.wrap(() -> {
            ResultSet rs = connection.createStatement().executeQuery(
                    query
            );
            rs.next();
            String colors = rs.getString("colors");
            return colors == null ? "" : colors;
        }).get();
    }

    @Override
    public String getFont() {
        String query = queryPrefix() + "RETURN n.font as font";
        return NoEx.wrap(() -> {
            ResultSet rs = connection.createStatement().executeQuery(
                    query
            );
            rs.next();
            String font = rs.getString("font");
            return font == null ? "" : font;
        }).get();
    }

    @Override
    public void setColors(String colors) {
        String query = queryPrefix()
                + "SET n.colors = @colors";
        NoEx.wrap(() -> {
            NamedParameterStatement statement = new NamedParameterStatement(
                    connection, query
            );
            statement.setString("colors", colors);
            return statement.execute();
        }).get();
    }

    @Override
    public void setFont(String font) {
        String query = queryPrefix()
                + "SET n.font = @font";
        NoEx.wrap(() -> {
            NamedParameterStatement statement = new NamedParameterStatement(
                    connection, query
            );
            statement.setString("font", font);
            return statement.execute();
        }).get();
    }

    @Override
    public void setChildrenIndex(String childrenIndex) {
        String query = queryPrefix()
                + "SET n.childrenIndexes = @childrenIndexes";
        NoEx.wrap(() -> {
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
        return NoEx.wrap(() -> {
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
        return addTagAndOriginalReferenceOnesOrNot(
                identification,
                true
        );
    }

    public ShareLevel getShareLevel() {
        String query = queryPrefix() + "RETURN n.shareLevel as shareLevel";
        return NoEx.wrap(() -> {
            ResultSet rs = connection.createStatement().executeQuery(
                    query
            );
            rs.next();
            Integer shareLevel = rs.getInt("shareLevel");
            return ShareLevel.get(shareLevel);
        }).get();
    }

    public Boolean isPublic() {
        return this.getShareLevel().isPublic();
    }

    private Map<URI, IdentifierPojo> addTagAndOriginalReferenceOnesOrNot(Identifier tag, Boolean addOriginalReferenceTags) {
        IdentifierPojo identificationPojo;
        Boolean isIdentifyingToAnIdentification = UserUris.isUriOfAnIdentifier(
                tag.getExternalResourceUri()
        );
        if (isIdentifyingToAnIdentification) {
            identificationPojo = new IdentifierPojo(
                    identificationFactory.withUri(
                            tag.getExternalResourceUri()
                    ).getExternalResourceUri(),
                    new FriendlyResourcePojo(
                            tag.getExternalResourceUri()
                    )
            );
        } else {
            identificationPojo = new IdentifierPojo(
                    new UserUris(getOwnerUsername()).generateIdentificationUri(),
                    tag
            );
        }

        identificationPojo.setCreationDate(new Date().getTime());
        final FriendlyResourceNeo4j neo4jFriendlyResource = friendlyResourceFactory.withUri(
                new UserUris(getOwnerUsername()).generateIdentificationUri()
        );
        Map<URI, IdentifierPojo> identifications = new HashMap<>();
        Date tagCreationDate = new Date();
        try {
            NamedParameterStatement statement = new NamedParameterStatement(
                    connection,
                    AddIdentificationQueryBuilder.usingIdentificationForGraphElement(
                            identificationPojo, this
                    ).build()
            );
            String searchContext = GraphIndexerNeo4j.descriptionToContext(
                    tag.comment()
            );
            statement.setString(
                    "label",
                    tag.label()
            );
            statement.setString(
                    "comment",
                    tag.comment()
            );
            statement.setString(
                    "privateContext",
                    searchContext
            );
            statement.setString(
                    "publicContext",
                    searchContext
            );
            statement.setString(
                    ImagesNeo4j.props.images.name(),
                    ImageJson.toJsonArray(tag.images())
            );
            statement.setLong(
                    "creationDate",
                    tagCreationDate.getTime()
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
                    FriendlyResourceNeo4j.props.last_modification_date.name(),
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
                IdentifierPojo tagPojo = new IdentifierPojo(
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
                );
                Boolean isReference = tag.getExternalResourceUri().equals(
                        this.uri()
                );

                Boolean isOwnerOfExternalUri = UserUris.ownerUserNameFromUri(
                        tag.getExternalResourceUri()
                ).equals(getOwnerUsername());

                if (isOwnerOfExternalUri && !isReference && addOriginalReferenceTags) {
                    Map<URI, IdentifierPojo> existingTags = getIdentifications();
                    Map<URI, IdentifierPojo> referenceTags = graphElementOperatorFactory.withUri(
                            tag.getExternalResourceUri()
                    ).getIdentifications();
                    for (IdentifierPojo otherIdentifier : referenceTags.values()) {
                        if (!existingTags.containsKey(otherIdentifier.getExternalResourceUri())) {
                            otherIdentifier = addTagAndOriginalReferenceOnesOrNot(
                                    otherIdentifier,
                                    false
                            ).get(otherIdentifier.getExternalResourceUri());
                        }
                        identifications.put(otherIdentifier.getExternalResourceUri(), otherIdentifier);
                    }
                }
                Boolean justCreatedTag = tagCreationDate.equals(tagPojo.creationDate());
                if (!isReference && isOwnerOfExternalUri && justCreatedTag) {
                    Map<URI, IdentifierPojo> tags = graphElementOperatorFactory.withUri(
                            tag.getExternalResourceUri()
                    ).addMeta(
                            tagPojo
                    );
                    tagPojo = tags.get(tag.getExternalResourceUri());
                }
                identifications.put(
                        tag.getExternalResourceUri(),
                        tagPojo
                );
            }
            return identifications;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    public void removeIdentification(Identifier identification) {
        String query = String.format(
                "%s MATCH n-[r:%s]->(i {%s:'%s'}) " +
                        "DELETE r " +
                        "SET i.%s=i.%s -1, " +
                        FriendlyResourceNeo4j.LAST_MODIFICATION_QUERY_PART +
                        "RETURN i.uri as uri",
                queryPrefix(),
                Relationships.IDENTIFIED_TO,
                FriendlyResourceNeo4j.props.uri.name(),
                identification.uri().toString(),
                IdentificationNeo4j.props.nb_references,
                IdentificationNeo4j.props.nb_references
        );
        NoEx.wrap(() -> {
            NamedParameterStatement statement = new NamedParameterStatement(
                    connection,
                    query
            );
            statement.setLong(
                    FriendlyResourceNeo4j.props.last_modification_date.name(),
                    new Date().getTime()
            );
            return statement.executeQuery();
        }).get();
        if (identification.getExternalResourceUri() != null && identification.getExternalResourceUri().equals(this.uri())) {
            identificationFactory.withUri(identification.uri()).setExternalResourceUri(
                    identification.uri()
            );
        }
    }

    @Override
    public void remove() {
        removeAllIdentifications();
        friendlyResource.remove();
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
                IdentificationNeo4j.props.nb_references,
                IdentificationNeo4j.props.relation_external_uri
        );
        Map<URI, IdentifierPojo> identifications = new HashMap<>();
        return NoEx.wrap(() -> {
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
        NoEx.wrap(() -> connection.createStatement().executeQuery(
                String.format(
                        "%s MATCH n-[r:%s]->i " +
                                "DELETE r " +
                                "SET i.%s=i.%s -1 ",
                        queryPrefix(),
                        Relationships.IDENTIFIED_TO,
                        IdentificationNeo4j.props.nb_references,
                        IdentificationNeo4j.props.nb_references
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
                FriendlyResourceNeo4j.props.label.name(), original.label(),
                FriendlyResourceNeo4j.props.comment.name(), original.comment()
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
