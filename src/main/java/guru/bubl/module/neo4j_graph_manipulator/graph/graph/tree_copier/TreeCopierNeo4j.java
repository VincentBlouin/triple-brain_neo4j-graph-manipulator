package guru.bubl.module.neo4j_graph_manipulator.graph.graph.tree_copier;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import guru.bubl.module.common_utils.Uris;
import guru.bubl.module.model.User;
import guru.bubl.module.model.UserUris;
import guru.bubl.module.model.friend.FriendManager;
import guru.bubl.module.model.friend.FriendManagerFactory;
import guru.bubl.module.model.friend.FriendStatus;
import guru.bubl.module.model.graph.FriendlyResourcePojo;
import guru.bubl.module.model.graph.ShareLevel;
import guru.bubl.module.model.graph.Tree;
import guru.bubl.module.model.graph.graph_element.GraphElementOperator;
import guru.bubl.module.model.graph.graph_element.GraphElementOperatorFactory;
import guru.bubl.module.model.graph.tag.TagPojo;
import guru.bubl.module.model.graph.tree_copier.TreeCopier;
import guru.bubl.module.model.json.ImageJson;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static guru.bubl.module.model.UserUris.urisToString;
import static guru.bubl.module.model.graph.ShareLevel.shareLevelsToIntegers;
import static guru.bubl.module.neo4j_graph_manipulator.graph.graph.graph_element.GraphElementOperatorNeo4j.incrementNbNeighborsQueryPart;
import static org.neo4j.driver.Values.parameters;

public class TreeCopierNeo4j implements TreeCopier {

    private User copier;

    @Inject
    private Driver driver;

    @Inject
    private GraphElementOperatorFactory graphElementOperatorFactory;

    @Inject
    private FriendManagerFactory friendManagerFactory;

    @AssistedInject
    protected TreeCopierNeo4j(
            @Assisted User copier
    ) {
        this.copier = copier;
    }

    @Override
    public Map<URI, URI> copyTreeOfUser(Tree tree, User copiedUser) {
        return copyTreeOfUserWithNewParentUri(tree, copiedUser, null);
    }

    @Override
    public Map<URI, URI> copyTreeOfUserWithNewParentUri(Tree tree, User copiedUser, URI newParentUri) {
        Map<URI, URI> uriAndCopyUri = new HashMap<>();
        UserUris userUris = new UserUris(copier);
        if (newParentUri != null && !userUris.isOwnerOfUri(newParentUri)) {
            return uriAndCopyUri;
        }
        if (UserUris.isUriOfAGroupRelation(tree.getRootUri()) && newParentUri == null) {
            return uriAndCopyUri;
        }
        Boolean isOwner = copier.username().equals(copiedUser.username());
        Set<ShareLevel> inShareLevels;
        if (isOwner) {
            inShareLevels = ShareLevel.allShareLevels;
        } else {
            Boolean isFriend = FriendStatus.confirmed == friendManagerFactory.forUser(
                    copier
            ).getStatusWithUser(copiedUser);
            inShareLevels = isFriend ? ShareLevel.friendShareLevels : ShareLevel.publicShareLevels;
        }
        Boolean areTagsAdded = false;
        Map<URI, Set<TagPojo>> tagsOfUri = new HashMap<>();
        String[] urisAsString = urisToString(tree.getUrisOfGraphElements());
        Set<TagPojo> tagsOfRootBubble = new HashSet<>();
        tagsOfRootBubble.add(tree.getRootAsTag());
        tagsOfUri.put(
                tree.getRootUri(), tagsOfRootBubble
        );
        try (Session session = driver.session()) {
            String query = "MATCH (n:GraphElement) " +
                    "WHERE n.uri IN $geUris AND " +
                    "n.owner=$copiedUser AND " +
                    "n.shareLevel IN $shareLevels " +
                    "WITH count(n) as nbGe, COLLECT(n) as nArray " +
                    "WITH (CASE WHEN nbGe = $nbGeExpected THEN nArray ELSE null END) as nArray " +
                    "UNWIND(nArray) as n " +
                    "OPTIONAL MATCH (n)<-[r:SOURCE|DESTINATION]->() " +
                    "OPTIONAL MATCH (n)-[:IDENTIFIED_TO]->(t) " +
                    "WITH nArray,COLLECT(r) as rArray, COLLECT({nUri:'\"'+n.uri+'\"', externalUri:'\"'+t.external_uri+'\"',label:'\"'+t.label+'\"',desc:'\"'+t.comment+'\"', images: t.images}) as tags " +
                    "CALL apoc.refactor.cloneSubgraph(nArray, rArray, {}) YIELD input, output, error " +
                    "WITH collect(output) as createdNodes, tags " +
                    "UNWIND createdNodes as c " +
                    "SET c.owner=$copier, " +
                    "c.shareLevel=10," +
                    "c.creation_date=timestamp(), " +
                    "c.last_modification_date=timestamp(), " +
                    "c.copied_from_uri = c.uri, " +
                    "c.uri='" + userUris.graphUri() + "/'+ split(c.uri, '/')[5] + '/' + apoc.create.uuid() " +
                    "WITH c, tags " +
                    "RETURN c.uri as uri, c.copied_from_uri as originalUri, tags";
            Result rs = session.run(
                    query,
                    parameters(
                            "geUris",
                            urisAsString,
                            "copier",
                            copier.username(),
                            "nbGeExpected",
                            tree.getUrisOfGraphElements().size(),
                            "copiedUser",
                            copiedUser.username(),
                            "shareLevels",
                            shareLevelsToIntegers(inShareLevels)
                    )
            );
            while (rs.hasNext()) {
                Record record = rs.next();
                URI uri = URI.create(record.get("uri").asString());
                URI originalUri = URI.create(record.get("originalUri").asString());
                uriAndCopyUri.put(originalUri, uri);
                if (!areTagsAdded) {
                    areTagsAdded = true;
                    for (Object tagObject : record.get("tags").asList()) {
                        JSONObject tagJson = new JSONObject(tagObject.toString());
                        String externalUriAsString = tagJson.getString("externalUri");
                        if (externalUriAsString != null) {
                            URI graphElementUri = URI.create(tagJson.getString("nUri"));
                            if (!tagsOfUri.containsKey(graphElementUri)) {
                                tagsOfUri.put(
                                        graphElementUri,
                                        new HashSet<>()
                                );
                            }
                            Set<TagPojo> tags = tagsOfUri.get(graphElementUri);
                            FriendlyResourcePojo friendlyResourcePojo = new FriendlyResourcePojo(
                                    tagJson.optString("label"),
                                    tagJson.optString("desc")
                            );
                            friendlyResourcePojo.setImages(
                                    ImageJson.fromJson(tagJson.optString("images"))
                            );
                            tags.add(
                                    new TagPojo(
                                            URI.create(externalUriAsString),
                                            friendlyResourcePojo
                                    )
                            );
                        }
                    }
                }
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        for (URI originalUri : tagsOfUri.keySet()) {
            URI copiedUri = uriAndCopyUri.get(originalUri);
            if (copiedUri != null) {
                GraphElementOperator copiedGe = graphElementOperatorFactory.withUri(copiedUri);
                for (TagPojo tag : tagsOfUri.get(originalUri)) {
                    copiedGe.addTag(
                            tag,
                            ShareLevel.PRIVATE
                    );
                }
            }

        }
        if (UserUris.isUriOfAGroupRelation(tree.getRootUri())) {
            String nbNeighborsProperty = ShareLevel.PRIVATE.getNbNeighborsPropertyName();
            String query = String.format(
                    "MATCH(gr:GroupRelation{uri:$grUri})  " +
                            "MATCH(source:GraphElement{uri:$sourceUri}) " +
                            "MERGE (gr)-[:SOURCE]->(source) " +
                            "SET source.%s = source.%s + 1",
                    nbNeighborsProperty,
                    nbNeighborsProperty
            );
            try (Session session = driver.session()) {
                session.run(
                        query,
                        parameters(
                                "grUri",
                                tree.getRootUri().toString(),
                                "sourceUri",
                                newParentUri.toString()
                        )
                );
            }
        }
        return uriAndCopyUri;
    }
}
