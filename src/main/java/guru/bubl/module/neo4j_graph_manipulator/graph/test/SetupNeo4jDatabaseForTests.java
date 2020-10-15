package guru.bubl.module.neo4j_graph_manipulator.graph.test;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;

import java.util.Arrays;
import java.util.List;

public class SetupNeo4jDatabaseForTests {

    private final List<String> queries = Arrays.asList(
            "CREATE INDEX ON :Resource(uri)",
            "CREATE CONSTRAINT ON (n:User) ASSERT n.email IS UNIQUE",
            "CREATE INDEX ON :GraphElement(owner)",
            "CALL db.index.fulltext.createNodeIndex('graphElementLabel',['GraphElement'],['label'])",
            "CALL db.index.fulltext.createNodeIndex('vertexLabel',['Vertex'],['label'])",
            "CALL db.index.fulltext.createNodeIndex('tagLabel',['Meta'],['label'])",
            "CALL db.index.fulltext.createNodeIndex('patternLabel',['Pattern'],['label'])",
            "CALL db.index.fulltext.createNodeIndex('username',['User'],['username'])",
            "CREATE INDEX ON :GraphElement(shareLevel)",
            "CREATE INDEX ON :GraphElement(last_center_date)",
            "CREATE INDEX ON :Meta(external_uri)",
            "CREATE INDEX ON :GraphElement(isUnderPattern)",
            "CREATE INDEX ON :GraphElement(nb_visits)",
            "CREATE INDEX ON :GraphElement(nb_private_neighbors)",
            "CREATE INDEX ON :GraphElement(nb_friend_neighbors)",
            "CREATE INDEX ON :GraphElement(nb_public_neighbors)"
    );

    public void doItWithDriver(Driver driver) {
        try (Session session = driver.session()) {
            for (String query : queries) {
                session.run(query);
            }
        }
    }
}
