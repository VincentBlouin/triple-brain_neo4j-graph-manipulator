package guru.bubl.module.neo4j_graph_manipulator.graph.graph.tree_copier;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import guru.bubl.module.model.FriendlyResource;
import guru.bubl.module.model.User;
import guru.bubl.module.model.UserUris;
import guru.bubl.module.model.graph.FriendlyResourcePojo;
import guru.bubl.module.model.graph.ShareLevel;
import guru.bubl.module.model.graph.Tree;
import guru.bubl.module.model.graph.graph_element.GraphElementOperator;
import guru.bubl.module.model.graph.graph_element.GraphElementOperatorFactory;
import guru.bubl.module.model.graph.graph_element.GraphElementPojo;
import guru.bubl.module.model.graph.tag.TagPojo;
import guru.bubl.module.model.graph.tree_copier.TreeCopier;
import guru.bubl.module.model.json.ImageJson;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.neo4j.driver.*;

import java.net.URI;
import java.util.*;

import static guru.bubl.module.model.UserUris.urisToString;
import static org.neo4j.driver.Values.parameters;

public class TreeCopierNeo4j implements TreeCopier {

    private User copier;

    @Inject
    private Driver driver;

    @Inject
    private GraphElementOperatorFactory graphElementOperatorFactory;

    @AssistedInject
    protected TreeCopierNeo4j(
            @Assisted User copier
    ) {
        this.copier = copier;
    }

    @Override
    public URI ofAnotherUser(Tree tree, User copiedUser) {
        UserUris userUris = new UserUris(copier);
        URI newRootUri = tree.getRootUri();
        Map<URI, Set<TagPojo>> tagsOfUri = new HashMap<>();
        Map<URI, URI> uriAndCopyUri = new HashMap<>();
        TagPojo rootAsTag;
        String query = "MATCH (n:GraphElement) " +
                "WHERE n.uri IN $geUris " +
                "OPTIONAL MATCH (n)-[:IDENTIFIED_TO]->(t), " +
                "(n)-[rels:SOURCE|DESTINATION]-() " +
                "WITH n, rels, COLLECT({externalUri:'\"'+t.external_uri+'\"',label:'\"'+t.label+'\"',desc:'\"'+t.comment+'\"', images: t.images}) as tags, count(t) as nbTags " +
                "CALL apoc.refactor.cloneSubgraph([n], [rels], {}) YIELD input, output, error " +
                "WITH n.uri as tagsForUri, tags, nbTags, collect(output) as createdNodes " +
                "UNWIND createdNodes as c " +
                "SET c.owner=$owner, " +
                "c.shareLevel=10," +
                "c.creation_date=timestamp(), " +
                "c.last_modification_date=timestamp(), " +
                "c.original_uri = c.uri, " +
                "c.uri='" + userUris.graphUri() + "/'+ split(c.uri, '/')[5] + '/' + apoc.create.uuid() " +
                "WITH tagsForUri, tags, c, nbTags " +
                "RETURN c.uri as uri, c.original_uri as originalUri, tagsForUri, tags, nbTags";
        try (Session session = driver.session()) {
            Result rs = session.run(
                    query,
                    parameters(
                            "geUris",
                            urisToString(tree.getUrisOfGraphElements()),
                            "owner",
                            copier.username()
                    )
            );
            while (rs.hasNext()) {
                Record record = rs.next();
                URI uri = URI.create(record.get("uri").asString());
                URI originalUri = URI.create(record.get("originalUri").asString());
                uriAndCopyUri.put(originalUri, uri);
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
                if (originalUri.equals(tree.getRootUri())) {
                    newRootUri = uri;
//                    if (record.get("originalLabel").asObject() != null) {
//                        patternAsTag.setLabel(record.get("originalLabel").asString());
//                    }
//                    if (record.get("originalComment").asObject() != null) {
//                        patternAsTag.setComment(
//                                record.get("originalComment").asString()
//                        );
//                    }
                }
//                if (record.get("externalUri").asObject() != null) {
//                    if (!tagThoseGraphElements.containsKey(uri)) {
//                        tagThoseGraphElements.put(uri, new HashSet<>());
//                    }
//                    Set<TagPojo> tagsForUri = (HashSet) tagThoseGraphElements.get(uri);
//                    tagsForUri.add(new TagPojo(
//                            URI.create(record.get("externalUri").asString()),
//                            new GraphElementPojo(
//                                    new FriendlyResourcePojo(
//                                            record.get("label").asString(),
//                                            record.get("comment").asString()
//                                    )
//                            )
//                    ));
//                }

            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        for (
                URI originalUri : tagsOfUri.keySet()) {
            URI copiedUri = uriAndCopyUri.get(originalUri);
            for (TagPojo tag : tagsOfUri.get(originalUri)) {
                graphElementOperatorFactory.withUri(copiedUri).addTag(
                        tag,
                        ShareLevel.PRIVATE
                );
            }
        }
        return newRootUri;
    }
}
