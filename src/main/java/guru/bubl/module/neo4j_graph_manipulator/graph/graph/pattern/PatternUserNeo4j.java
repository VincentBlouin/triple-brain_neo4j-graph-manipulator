package guru.bubl.module.neo4j_graph_manipulator.graph.graph.pattern;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import guru.bubl.module.model.User;
import guru.bubl.module.model.UserUris;
import guru.bubl.module.model.graph.FriendlyResourcePojo;
import guru.bubl.module.model.graph.ShareLevel;
import guru.bubl.module.model.graph.graph_element.GraphElementOperator;
import guru.bubl.module.model.graph.graph_element.GraphElementOperatorFactory;
import guru.bubl.module.model.graph.graph_element.GraphElementPojo;
import guru.bubl.module.model.graph.tag.Tag;
import guru.bubl.module.model.graph.tag.TagPojo;
import guru.bubl.module.model.graph.pattern.PatternUser;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.neo4j.driver.Result;

import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.neo4j.driver.Values.parameters;

public class PatternUserNeo4j implements PatternUser {

    private Driver driver;
    private GraphElementOperatorFactory graphElementFactory;
    private User user;
    private URI patternUri;

    @AssistedInject
    protected PatternUserNeo4j(
            Driver driver,
            GraphElementOperatorFactory graphElementFactory,
            @Assisted User user,
            @Assisted URI patternUri
    ) {
        this.driver = driver;
        this.graphElementFactory = graphElementFactory;
        this.user = user;
        this.patternUri = patternUri;
    }


    @Override
    public URI use() {
        UserUris userUris = new UserUris(user);
        URI centerUri = null;
        TagPojo patternAsTag;
        Map<URI, Set<TagPojo>> tagThoseGraphElements = new HashMap<URI, Set<TagPojo>>();
        String query = "MATCH (n:Pattern{uri:$uri}) " +
                "SET n.nbPatternUsage=n.nbPatternUsage+1 " +
                "WITH n " +
                "CALL apoc.path.subgraphAll(n, {relationshipFilter:'SOURCE|DESTINATION|IDENTIFIED_TO>'}) YIELD nodes, relationships " +
                "CALL apoc.refactor.cloneSubgraph(nodes, relationships, {}) YIELD input, output, error " +
                "WITH collect(output) as createdNodes, n " +
                "UNWIND createdNodes as c " +
                "REMOVE c:Pattern, " +
                "c.isUnderPattern " +
                "SET c.owner=$owner, " +
                "c.shareLevel=10," +
                "c.creation_date=timestamp(), " +
                "c.last_modification_date=timestamp(), " +
                "c.copied_from_uri = c.uri, " +
                "c.uri='" + userUris.graphUri() + "/'+ split(c.uri, '/')[5] + '/' + apoc.create.uuid() " +
                "WITH c, n " +
                "OPTIONAL MATCH (c)-[:IDENTIFIED_TO]->(tag) " +
                "WITH c.uri as uri, c.copied_from_uri as copiedFromUri, n.label as originalLabel, n.comment as originalComment, tag, tag.external_uri as externalUri, tag.label as label, tag.comment as comment, tag.images as images " +
                "DETACH DELETE tag " +
                "RETURN uri, originalLabel, originalComment, copiedFromUri, externalUri, label, comment, images";
        try (Session session = driver.session()) {
            Result rs = session.run(
                    query,
                    parameters(
                            "uri",
                            patternUri.toString(),
                            "owner",
                            user.username()
                    )
            );
            patternAsTag = new TagPojo(
                    this.patternUri
            );
            while (rs.hasNext()) {
                Record record = rs.next();
                String patternUri = record.get("copiedFromUri").asString();
                URI uri = URI.create(record.get("uri").asString());
                if (patternUri.equals(this.patternUri.toString())) {
                    centerUri = uri;
                    if (record.get("originalLabel").asObject() != null) {
                        patternAsTag.setLabel(record.get("originalLabel").asString());
                    }
                    if (record.get("originalComment").asObject() != null) {
                        patternAsTag.setComment(
                                record.get("originalComment").asString()
                        );
                    }
                }
                if (record.get("externalUri").asObject() != null) {
                    if (!tagThoseGraphElements.containsKey(uri)) {
                        tagThoseGraphElements.put(uri, new HashSet<>());
                    }
                    Set<TagPojo> tagsForUri = (HashSet) tagThoseGraphElements.get(uri);
                    tagsForUri.add(new TagPojo(
                            URI.create(record.get("externalUri").asString()),
                            new GraphElementPojo(
                                    new FriendlyResourcePojo(
                                            record.get("label").asString(),
                                            record.get("comment").asString()
                                    )
                            )
                    ));
                }
            }
            graphElementFactory.withUri(
                    centerUri
            ).addTag(
                    patternAsTag,
                    ShareLevel.PRIVATE
            );
            for (URI uri : tagThoseGraphElements.keySet()) {
                GraphElementOperator graphElementOperator = graphElementFactory.withUri(uri);
                Set<TagPojo> tags = tagThoseGraphElements.get(uri);
                for (TagPojo tag : tags) {
                    graphElementOperator.addTag(
                            tag,
                            ShareLevel.PRIVATE
                    );
                }
            }
            return centerUri;
        }
    }
}
