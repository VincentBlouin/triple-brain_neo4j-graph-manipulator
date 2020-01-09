package guru.bubl.module.neo4j_graph_manipulator.graph.graph.pattern;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import guru.bubl.module.model.User;
import guru.bubl.module.model.UserUris;
import guru.bubl.module.model.graph.FriendlyResourcePojo;
import guru.bubl.module.model.graph.GraphElementOperatorFactory;
import guru.bubl.module.model.graph.GraphElementPojo;
import guru.bubl.module.model.graph.tag.TagPojo;
import guru.bubl.module.model.graph.pattern.PatternUser;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;

import java.net.URI;

import static org.neo4j.driver.v1.Values.parameters;

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
        String query = "MATCH (n:Pattern{uri:$uri}) " +
                "CALL apoc.path.subgraphAll(n, {relationshipFilter:'SOURCE_VERTEX|DESTINATION_VERTEX|IDENTIFIED_TO>'}) YIELD nodes, relationships " +
                "CALL apoc.refactor.cloneSubgraph(nodes, relationships, {}) YIELD input, output, error " +
                "WITH collect(output) as createdNodes " +
                "UNWIND createdNodes as c " +
                "REMOVE c:Pattern " +
                "SET c.owner=$owner, " +
                "c.shareLevel=10," +
                "c.creation_date=timestamp(), " +
                "c.last_modification_date=timestamp(), " +
                "c.pattern_uri = c.uri, " +
                "c.uri=(CASE WHEN 'Edge' IN LABELS(c) THEN '" + userUris.baseEdgeUri() + "/' ELSE '" + userUris.baseVertexUri() + "/' END)+apoc.create.uuid() " +
                "WITH c " +
                "OPTIONAL MATCH (c)-[:IDENTIFIED_TO]->(tag) " +
                "WITH c.uri as uri, c.pattern_uri as patternUri, tag, tag.external_uri as externalUri, tag.label as label, tag.comment as comment, tag.images as images " +
                "DETACH DELETE tag " +
                "RETURN uri, patternUri, externalUri, label, comment, images";
        try (Session session = driver.session()) {
            StatementResult rs = session.run(
                    query,
                    parameters(
                            "uri",
                            patternUri.toString(),
                            "owner",
                            user.username()
                    )
            );
            URI centerUri = null;
            while (rs.hasNext()) {
                Record record = rs.next();
                String patternUri = record.get("patternUri").asString();
                URI uri = URI.create(record.get("uri").asString());
                if (patternUri.equals(this.patternUri.toString())) {
                    centerUri = uri;
                }
                if (record.get("externalUri").asObject() != null)
                    graphElementFactory.withUri(
                            uri
                    ).addMeta(
                            new TagPojo(
                                    URI.create(record.get("externalUri").asString()),
                                    new GraphElementPojo(
                                            new FriendlyResourcePojo(
                                                    record.get("label").asString(),
                                                    record.get("comment").asString()
                                            )
                                    )
                            )
                    );
            }
            return centerUri;
        }
    }
}
