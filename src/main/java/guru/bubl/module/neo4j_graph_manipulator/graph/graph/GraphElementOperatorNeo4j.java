/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.graph;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import guru.bubl.module.model.Image;
import guru.bubl.module.model.User;
import guru.bubl.module.model.UserUris;
import guru.bubl.module.model.graph.*;
import guru.bubl.module.model.graph.tag.Tag;
import guru.bubl.module.model.graph.tag.TagPojo;
import guru.bubl.module.model.graph.vertex.NbNeighborsPojo;
import guru.bubl.module.model.json.ImageJson;
import guru.bubl.module.neo4j_graph_manipulator.graph.FriendlyResourceFactoryNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.FriendlyResourceNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.OperatorNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.RestApiUtilsNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.subgraph.VertexFromExtractorQueryRow;
import guru.bubl.module.neo4j_graph_manipulator.graph.image.ImagesNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.tag.TagFactoryNeo4J;
import guru.bubl.module.neo4j_graph_manipulator.graph.search.GraphIndexerNeo4j;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;

import java.net.URI;
import java.util.*;

import static guru.bubl.module.neo4j_graph_manipulator.graph.RestApiUtilsNeo4j.map;
import static org.neo4j.driver.v1.Values.parameters;

public class GraphElementOperatorNeo4j implements GraphElementOperator, OperatorNeo4j {

    public enum props {
        identifications,
        sort_date,
        move_date
    }

    protected FriendlyResourceNeo4j friendlyResource;
    protected FriendlyResourceFactoryNeo4j friendlyResourceFactory;
    protected Driver driver;

    protected TagFactoryNeo4J identificationFactory;

    protected GraphElementOperatorFactory graphElementOperatorFactory;

    public static String incrementNbNeighborsQueryPart(ShareLevel shareLevel, String variableName, String prefix) {
        return incrementOrDecrementNbFriendsOrPublicQueryPart(
                shareLevel,
                variableName,
                prefix,
                false
        );
    }

    public static String decrementNbNeighborsQueryPart(ShareLevel shareLevel, String variableName, String prefix) {
        return incrementOrDecrementNbFriendsOrPublicQueryPart(
                shareLevel,
                variableName,
                prefix,
                true
        );
    }

    private static String incrementOrDecrementNbFriendsOrPublicQueryPart(ShareLevel shareLevel, String variableName, String prefix, Boolean decrement) {
        String propertyName = shareLevel.getNbNeighborsPropertyName();
        return String.format(
                prefix + "%s.%s = %s.%s " + (decrement ? " - " : " + ") + " 1 ",
                variableName,
                propertyName,
                variableName,
                propertyName
        );
    }


    @AssistedInject
    protected GraphElementOperatorNeo4j(
            FriendlyResourceFactoryNeo4j friendlyResourceFactory,
            Driver driver,
            TagFactoryNeo4J identificationFactory,
            GraphElementOperatorFactory graphElementOperatorFactory,
            @Assisted URI uri
    ) {
        this.friendlyResource = friendlyResourceFactory.withUri(
                uri
        );
        this.identificationFactory = identificationFactory;
        this.driver = driver;
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
    public String getColors() {
        return friendlyResource.getColors();
    }

    @Override
    public String getFont() {
        try (Session session = driver.session()) {
            Record record = session.run(
                    queryPrefix() + "RETURN n.font as font",
                    parameters(
                            "uri",
                            this.uri().toString()
                    )
            ).single();
            return record.get(
                    "font"
            ).asObject() == null ? "" : record.get("font").asString();
        }
    }

    @Override
    public void setColors(String colors) {
        friendlyResource.setColors(colors);
    }

    @Override
    public void setFont(String font) {
        try (Session session = driver.session()) {
            session.run(
                    queryPrefix() + "SET n.font=$font",
                    parameters(
                            "uri",
                            uri().toString(),
                            "font",
                            font
                    )
            );
        }
    }

    @Override
    public void setChildrenIndex(String childrenIndex) {
        try (Session session = driver.session()) {
            session.run(
                    queryPrefix() + "SET n.childrenIndexes=$childrenIndexes",
                    parameters(
                            "uri",
                            uri().toString(),
                            "childrenIndexes",
                            childrenIndex
                    )
            );
        }
    }

    @Override
    public Boolean isUnderPattern() {
        try (Session session = driver.session()) {
            Record record = session.run(
                    queryPrefix() + "RETURN n.isUnderPattern",
                    parameters(
                            "uri", uri().toString()
                    )
            ).single();
            return record.get("n.isUnderPattern").asObject() == null ?
                    false :
                    record.get("n.isUnderPattern").asBoolean();
        }
    }

    @Override
    public Boolean isPatternOrUnderPattern() {
        try (Session session = driver.session()) {
            Record record = session.run(
                    queryPrefix() + "RETURN n.isUnderPattern,'Pattern' IN LABELS(n) as isPattern",
                    parameters(
                            "uri", uri().toString()
                    )
            ).single();
            Boolean isUnderPattern = record.get("n.isUnderPattern").asObject() == null ?
                    false :
                    record.get("n.isUnderPattern").asBoolean();
            return isUnderPattern || record.get("isPattern").asBoolean();
        }
    }

    @Override
    public String getChildrenIndex() {
        try (Session session = driver.session()) {
            Record record = session.run(
                    queryPrefix() + "RETURN n.childrenIndexes as childrenIndexes",
                    parameters(
                            "uri",
                            this.uri().toString()
                    )
            ).single();
            return record.get(
                    "childrenIndexes"
            ).asObject() == null ? "" : record.get("childrenIndexes").asString();
        }
    }

    @Override
    public URI getPatternUri() {
        try (Session session = driver.session()) {
            Record record = session.run(
                    queryPrefix() + "RETURN n.pattern_uri as patternUri",
                    parameters(
                            "uri",
                            this.uri().toString()
                    )
            ).single();
            return record.get(
                    "patternUri"
            ).asObject() == null ? null : URI.create(record.get("patternUri").asString());
        }
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
    public Map<URI, TagPojo> addTag(
            Tag tag,
            ShareLevel sourceShareLevel
    ) {
        TagPojo identificationPojo;
        Boolean isIdentifyingToAnIdentification = UserUris.isUriOfATag(
                tag.getExternalResourceUri()
        );
        if (isIdentifyingToAnIdentification) {
            identificationPojo = new TagPojo(
                    identificationFactory.withUri(
                            tag.getExternalResourceUri()
                    ).getExternalResourceUri(),
                    new GraphElementPojo(
                            new FriendlyResourcePojo(
                                    tag.getExternalResourceUri()
                            )
                    )
            );
        } else {
            identificationPojo = new TagPojo(
                    new UserUris(getOwnerUsername()).generateTagUri(),
                    tag
            );
        }

        identificationPojo.setCreationDate(new Date().getTime());
        identificationPojo.setUri(
                tag.hasUri() && UserUris.isUriOfATag(tag.uri()) ? tag.uri() : new UserUris(getOwnerUsername()).generateTagUri()
        );
        if (!UserUris.ownerUserNameFromUri(identificationPojo.uri()).equals(getOwnerUsername())) {
            return new HashMap<>();
        }
        Map<URI, TagPojo> identifications = new HashMap<>();
        Date tagCreationDate = new Date();
        String searchContext = GraphIndexerNeo4j.descriptionToContext(
                tag.comment()
        );

        try (Session session = driver.session()) {
            StatementResult result = session.run(
                    AddTagQueryBuilder.usingIdentificationForGraphElement(
                            queryPrefix(),
                            sourceShareLevel,
                            !tag.hasUri() && shouldTagExternalUri(tag.getExternalResourceUri())
                    ).build(),
                    parameters(
                            "uri",
                            uri().toString(),
                            "metaUri",
                            identificationPojo.uri().toString(),
                            "label",
                            tag.label(),
                            "comment",
                            tag.comment(),
                            "privateContext",
                            searchContext,
                            "publicContext",
                            searchContext,
                            ImagesNeo4j.props.images.name(),
                            ImageJson.toJsonArray(tag.images()),
                            "creationDate",
                            tagCreationDate.getTime(),
                            "external_uri",
                            identificationPojo.getExternalResourceUri().toString(),
                            "relationExternalUri",
                            identificationPojo.getRelationExternalResourceUri().toString(),
                            FriendlyResourceNeo4j.props.last_modification_date.name(),
                            new Date().getTime(),
                            FriendlyResourceNeo4j.props.owner.name(),
                            UserUris.ownerUserNameFromUri(uri()),
                            "shareLevel",
                            tag.getShareLevel().getIndex()
                    )
            );
            while (result.hasNext()) {
                Record record = result.next();
                URI externalUri = URI.create(
                        record.get("external_uri").asString()
                );
                TagPojo tagPojo = new TagPojo(
                        externalUri,
                        new GraphElementPojo(
                                new FriendlyResourcePojo(
                                        URI.create(
                                                record.get("uri").asString()
                                        ),
                                        record.get("label").asObject() == null ?
                                                "" : record.get("label").asString(),
                                        record.get("images").asObject() == null ?
                                                new HashSet<>() : ImageJson.fromJson(record.get("images").asString()),
                                        record.get("comment").asObject() == null ?
                                                "" : record.get("comment").asString(),
                                        record.get("creation_date").asLong(),
                                        record.get("last_modification_date").asLong()
                                )
                        ),
                        new NbNeighborsPojo(
                                record.get("nbPrivateNeighbors").asInt(),
                                record.get("nbFriendNeighbors").asInt(),
                                record.get("nbPublicNeighbors").asInt()
                        )
                );
                tagPojo.setShareLevel(
                        VertexFromExtractorQueryRow.getShareLevel(
                                "f",
                                record
                        )
                );
                identifications.put(
                        tag.getExternalResourceUri(),
                        tagPojo
                );
            }
            return identifications;
        }
    }

    private Boolean shouldTagExternalUri(URI externalUri) {
        Boolean isSelfReference = externalUri.equals(
                this.uri()
        );
        if (isSelfReference) {
            return false;
        }
        if (!UserUris.isMindRespectUri(externalUri)) {
            return false;
        }
        Boolean isOwnerOfExternalUri = UserUris.ownerUserNameFromUri(
                externalUri
        ).equals(getOwnerUsername());
        Boolean isVoidReference = externalUri.toString().contains("/void/ref/");
        return isOwnerOfExternalUri && !isVoidReference;
    }

    @Override
    public ShareLevel getShareLevel() {
        try (Session session = driver.session()) {
            Record record = session.run(
                    queryPrefix() + "RETURN n.shareLevel as shareLevel",
                    parameters(
                            "uri",
                            this.uri().toString()
                    )
            ).single();
            if (record.get("shareLevel").asObject() == null) {
                return ShareLevel.PRIVATE;
            }
            Integer shareLevel = record.get("shareLevel").asInt();
            return ShareLevel.get(shareLevel);
        }
    }

    @Override
    public void removeTag(Tag identification) {
        try (Session session = driver.session()) {
            session.run(
                    String.format(
                            "%s MATCH (n)-[r:IDENTIFIED_TO]->(i{uri:$metaUri}) " +
                                    "DELETE r " +
                                    "SET i.nb_references=i.nb_references -1, " +
                                    FriendlyResourceNeo4j.LAST_MODIFICATION_QUERY_PART +
                                    "RETURN i.uri as uri",
                            queryPrefix()
                    ),
                    parameters(
                            "uri",
                            uri().toString(),
                            "metaUri",
                            identification.uri().toString(),
                            "last_modification_date",
                            new Date().getTime()
                    )
            );
            if (identification.getExternalResourceUri() != null && identification.getExternalResourceUri().equals(this.uri())) {
                identificationFactory.withUri(identification.uri()).setExternalResourceUri(
                        identification.uri()
                );
            }
        }
    }

    @Override
    public void remove() {
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
    public Map<String, Object> addCreationProperties(Map<String, Object> map) {
        Map<String, Object> newMap = RestApiUtilsNeo4j.map(
                "nb_private_neighbors", 0,
                "nb_friend_neighbors", 0,
                "nb_public_neighbors", 0,
                "nb_visits", 0
        );
        newMap.putAll(
                map
        );
        return friendlyResource.addCreationProperties(
                newMap
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
    public Map<URI, TagPojo> getTags() {
        Map<URI, TagPojo> identifications = new HashMap<>();
        try (Session session = driver.session()) {
            StatementResult rs = session.run(
                    String.format(
                            "%sMATCH (n)-[r:IDENTIFIED_TO]->(tag) " +
                                    "RETURN tag.uri as uri, " +
                                    "tag.external_uri as external_uri, " +
                                    "tag.nb_private_neighbors as nbPrivateNeighbors, " +
                                    "tag.nb_friend_neighbors as nbFriendNeighbors, " +
                                    "tag.nb_public_neighbors as nbPublicNeighbors, " +
                                    "r.relation_external_uri as r_x_u",
                            queryPrefix()
                    ),
                    parameters(
                            "uri", uri().toString()
                    )
            );
            while (rs.hasNext()) {
                Record record = rs.next();
                URI uri = URI.create(
                        record.get("uri").asString()
                );
                URI externalUri = URI.create(
                        record.get("external_uri").asString()
                );
                TagPojo tag = new TagPojo(
                        externalUri,
                        new GraphElementPojo(
                                new FriendlyResourcePojo(
                                        uri
                                )
                        )
                );
                tag.getNbNeighbors().setPrivate(
                        record.get("nbPrivateNeighbors").asObject() == null ? 0 :
                                record.get("nbPrivateNeighbors").asInt()
                );
                tag.getNbNeighbors().setFriend(
                        record.get("nbFriendNeighbors").asObject() == null ? 0 :
                                record.get("nbFriendNeighbors").asInt()
                );
                tag.getNbNeighbors().setPublic(
                        record.get("nbPublicNeighbors").asObject() == null ? 0 :
                                record.get("nbPublicNeighbors").asInt()
                );
                String relationExternalUriString = record.get("r_x_u").asString();
                tag.setRelationExternalResourceUri(
                        relationExternalUriString == null ? Tag.DEFAULT_IDENTIFIER_RELATION_EXTERNAL_URI :
                                URI.create(
                                        relationExternalUriString
                                )
                );
                identifications.put(
                        externalUri,
                        tag
                );
            }
            return identifications;
        }
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
        clone.addTag(
                new TagPojo(
                        this.uri(),
                        new GraphElementPojo(
                                original
                        )
                )
        );
        cache.getTags().values().forEach(
                clone::addTag
        );
        return clone;
    }
}
