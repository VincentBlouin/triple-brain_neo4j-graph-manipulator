package guru.bubl.module.neo4j_graph_manipulator.graph.graph.tree_copier;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static guru.bubl.module.model.UserUris.urisToString;
import static guru.bubl.module.model.graph.ShareLevel.shareLevelsToIntegers;
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
    public URI copyTreeOfUser(Tree tree, User copiedUser) {
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
        UserUris userUris = new UserUris(copier);
        URI newRootUri = null;
        Map<URI, Set<TagPojo>> tagsOfUri = new HashMap<>();
        Map<URI, URI> uriAndCopyUri = new HashMap<>();
        String[] urisAsString = urisToString(tree.getUrisOfGraphElements());
        try (Session session = driver.session()) {
            String query = "MATCH (n:GraphElement) " +
                    "WHERE n.uri IN $geUris AND " +
                    "n.owner=$copiedUser AND " +
                    "n.shareLevel IN $shareLevels " +
                    "WITH count(n) as nbGe, COLLECT(n) as nArray " +
                    "WITH (CASE WHEN nbGe = $nbGeExpected THEN nArray ELSE null END) as nArray " +
                    "UNWIND(nArray) as n " +
                    "OPTIONAL MATCH (n)-[:IDENTIFIED_TO]->(t), " +
                    "(n)-[rels:SOURCE|DESTINATION]-() " +
                    "WITH n,rels,COLLECT({externalUri:'\"'+t.external_uri+'\"',label:'\"'+t.label+'\"',desc:'\"'+t.comment+'\"', images: t.images}) as tags, count(t) as nbTags " +
                    "CALL apoc.refactor.cloneSubgraph([n], [rels], {}) YIELD input, output, error " +
                    "WITH n.uri as tagsForUri, tags, nbTags, collect(output) as createdNodes " +
                    "UNWIND createdNodes as c " +
                    "SET c.owner=$copier, " +
                    "c.shareLevel=10," +
                    "c.creation_date=timestamp(), " +
                    "c.last_modification_date=timestamp(), " +
                    "c.original_uri = c.uri, " +
                    "c.uri='" + userUris.graphUri() + "/'+ split(c.uri, '/')[5] + '/' + apoc.create.uuid() " +
                    "WITH tagsForUri, tags, c, nbTags " +
                    "RETURN c.uri as uri, c.original_uri as originalUri, tagsForUri, tags, nbTags";
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
                if (newRootUri == null && originalUri.equals(tree.getRootUri())) {
                    newRootUri = uri;
                    graphElementOperatorFactory.withUri(uri).addTag(
                            tree.getRootAsTag(), ShareLevel.PRIVATE
                    );
                }
                Integer nbTags = record.get("nbTags").asInt();
                if (nbTags >= 1) {
                    URI originalUriTags = URI.create(record.get("tagsForUri").asString());
                    if (!tagsOfUri.containsKey(originalUriTags)) {
                        tagsOfUri.put(
                                originalUriTags,
                                new HashSet<>()
                        );
                    }
                    Set<TagPojo> tags = tagsOfUri.get(originalUriTags);
                    for (Object tagObject : record.get("tags").asList()) {
                        JSONObject tagJson = new JSONObject(tagObject.toString());
                        FriendlyResourcePojo friendlyResourcePojo = new FriendlyResourcePojo(
                                tagJson.optString("label"),
                                tagJson.optString("desc")
                        );
                        friendlyResourcePojo.setImages(
                                ImageJson.fromJson(tagJson.optString("images"))
                        );
                        tags.add(
                                new TagPojo(
                                        URI.create(tagJson.getString("externalUri")),
                                        friendlyResourcePojo
                                )
                        );
                    }
                }
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        for (URI originalUri : tagsOfUri.keySet()) {
            URI copiedUri = uriAndCopyUri.get(originalUri);
            GraphElementOperator copiedGe = graphElementOperatorFactory.withUri(copiedUri);
            for (TagPojo tag : tagsOfUri.get(originalUri)) {
                copiedGe.addTag(
                        tag,
                        ShareLevel.PRIVATE
                );
            }
        }
        return newRootUri;
    }
}
