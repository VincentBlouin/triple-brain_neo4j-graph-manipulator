/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.admin;

import com.google.inject.Inject;
import guru.bubl.module.model.admin.WholeGraphAdmin;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Session;

public class WholeGraphAdminNeo4j implements WholeGraphAdmin {

    @Inject
    protected Driver driver;

    @Override
    public void refreshNbNeighbors() {
        try (Session session = driver.session()) {
            session.run(
                    "MATCH(n:Vertex) OPTIONAL MATCH (n)<-[:SOURCE|DESTINATION]-(e)," +
                            "(e)-[:SOURCE|DESTINATION]->(o) WHERE o.shareLevel=10 " +
                            "WITH n, count(o) as nbPrivate " +
                            "SET n.nb_private_neighbors=nbPrivate"
            );
            session.run(
                    "MATCH(n:Vertex) OPTIONAL MATCH (n)<-[:SOURCE|DESTINATION]-(e)," +
                            "(e)-[:SOURCE|DESTINATION]->(o) WHERE o.shareLevel=20 " +
                            "WITH n, count(o) as nbFriend " +
                            "SET n.nb_friend_neighbors=nbFriend;"
            );
            session.run(
                    "MATCH(n:Vertex) OPTIONAL MATCH (n)<-[:SOURCE|DESTINATION]-(e)," +
                            "(e)-[:SOURCE|DESTINATION]->(o) where o.shareLevel=30 or o.shareLevel=40 " +
                            "WITH n, count(o) as nbPublic " +
                            "SET n.nb_public_neighbors=nbPublic;"
            );
        }
    }

    @Override
    public void refreshNbNeighborsToAllTags() {
        try (Session session = driver.session()) {
            session.run(
                    "MATCH(n:Meta) OPTIONAL MATCH (n)<-[:IDENTIFIED_TO]-(ge) " +
                            "WHERE ge.shareLevel=10 " +
                            "WITH n, count(ge) as nbPrivate " +
                            "SET n.nb_private_neighbors=nbPrivate"
            );
            session.run(
                    "MATCH(n:Meta) OPTIONAL MATCH (n)<-[:IDENTIFIED_TO]-(ge) " +
                            "WHERE ge.shareLevel=20 " +
                            "WITH n, count(ge) as nbFriend " +
                            "SET n.nb_friend_neighbors=nbFriend"
            );
            session.run(
                    "MATCH(n:Meta) OPTIONAL MATCH (n)<-[:IDENTIFIED_TO]-(ge) " +
                            "WHERE ge.shareLevel=30 or ge.shareLevel=40 " +
                            "WITH n, count(ge) as nbPublic " +
                            "SET n.nb_public_neighbors=nbPublic;"
            );
        }
    }

    @Override
    public void reindexAll() {
        try (Session session = driver.session()) {
            session.run(
                    "MATCH(n:Vertex) OPTIONAL MATCH (n)<-[:SOURCE|DESTINATION]->(e)," +
                            "(e)<-[:SOURCE|DESTINATION]->(o) WHERE o.label <> '' " +
                            "WITH n, o, (o.nb_private_neighbors + o.nb_friend_neighbors + o.nb_public_neighbors) as nbNeighbors " +
                            "ORDER BY nbNeighbors DESC " +
                            "WITH reduce(a=\"\", os in collect(o) |  a + \"{{\" + substring(os.label, 0, 20)) as context, n " +
                            "SET n.private_context = substring(context, 2, 110)"
            );
            session.run(
                    "MATCH(n:Vertex) OPTIONAL MATCH (n)<-[:SOURCE|DESTINATION]->(e)," +
                            "(e)-[:SOURCE|DESTINATION]->(o) WHERE o.shareLevel in [20,30,40] AND o.label <>'' " +
                            "WITH n, o, (o.nb_friend_neighbors + o.nb_public_neighbors) as nbNeighbors " +
                            "ORDER BY nbNeighbors DESC " +
                            "WITH reduce(a=\"\", os in collect(o) |  a + \"{{\" + substring(os.label, 0, 20)) as context, n " +
                            "SET n.friend_context = substring(context, 2, 110)"
            );
            session.run(
                    "MATCH(n:Vertex) OPTIONAL MATCH (n)<-[:SOURCE|DESTINATION]->(e)," +
                            "(e)-[:SOURCE|DESTINATION]->(o) WHERE o.shareLevel in [30,40] AND o.label <>'' " +
                            "WITH n, o " +
                            "ORDER BY o.nb_public_neighbors DESC " +
                            "WITH reduce(a=\"\", os in collect(o) |  a + \"{{\" + substring(os.label, 0, 20)) as context, n " +
                            "SET n.public_context = substring(context, 2, 110)"
            );
            session.run(
                    "MATCH(n:Meta) OPTIONAL MATCH (n)<-[:IDENTIFIED_TO]-(ov) WHERE 'Vertex' in labels(ov) AND ov.label <>'' " +
                            "WITH n,ov OPTIONAL MATCH (n)<-[:IDENTIFIED_TO]-(e) WHERE 'Edge' in labels(e) " +
                            "WITH n,COLLECT(ov) as ovList,e " +
                            "OPTIONAL MATCH (e)-[:DESTINATION]->(v) WHERE v.label <>'' " +
                            "WITH n, ovList + collect(v) as allVertices " +
                            "UNWIND  allVertices as tv " +
                            "WITH n, tv, (tv.nb_private_neighbors + tv.nb_friend_neighbors + tv.nb_public_neighbors) as nbNeighbors " +
                            "ORDER BY nbNeighbors DESC " +
                            "WITH reduce(a=\"\", os in collect(tv) |  a + \"{{\" + substring(os.label, 0, 20)) as context, n " +
                            "SET n.private_context = CASE n.comment WHEN \"\" THEN substring(context, 2, 110) ELSE n.comment END"
            );

            session.run(
                    "MATCH(n:Meta) OPTIONAL MATCH (n)<-[:IDENTIFIED_TO]-(ov) WHERE 'Vertex' in labels(ov) and ov.shareLevel in [20,30,40] AND ov.label <>'' " +
                            "WITH n,ov OPTIONAL MATCH (n)<-[:IDENTIFIED_TO]-(e) WHERE 'Edge' in labels(e) and ov.shareLevel in [20,30,40] " +
                            "WITH n,COLLECT(ov) as ovList,e " +
                            "OPTIONAL MATCH (e)-[:DESTINATION]->(v) WHERE v.label <>'' " +
                            "WITH n, ovList + collect(v) as allVertices " +
                            "UNWIND  allVertices as tv " +
                            "WITH n, tv, (tv.nb_friend_neighbors + tv.nb_public_neighbors) as nbNeighbors " +
                            "ORDER BY nbNeighbors DESC " +
                            "WITH reduce(a=\"\", os in collect(tv) |  a + \"{{\" + substring(os.label, 0, 20)) as context, n " +
                            "SET n.friend_context = CASE n.comment WHEN \"\" THEN substring(context, 2, 110) ELSE n.comment END"
            );
            session.run(
                    "MATCH(n:Meta) OPTIONAL MATCH (n)<-[:IDENTIFIED_TO]-(ov) WHERE 'Vertex' in labels(ov) and ov.shareLevel in [30,40] AND ov.label <>'' " +
                            "WITH n,ov OPTIONAL MATCH (n)<-[:IDENTIFIED_TO]-(e) WHERE 'Edge' in labels(e) and ov.shareLevel in [30,40] " +
                            "WITH n,COLLECT(ov) as ovList,e " +
                            "OPTIONAL MATCH (e)-[:DESTINATION]->(v) WHERE v.label <>'' " +
                            "WITH n, ovList + collect(v) as allVertices " +
                            "UNWIND  allVertices as tv " +
                            "WITH n, tv, tv.nb_public_neighbors as nbNeighbors " +
                            "ORDER BY nbNeighbors DESC " +
                            "WITH reduce(a=\"\", os in collect(tv) |  a + \"{{\" + substring(os.label, 0, 20)) as context, n " +
                            "SET n.public_context = CASE n.comment WHEN \"\" THEN substring(context, 2, 110) ELSE n.comment END"
            );
            session.run(
                    "MATCH(n:Edge) MATCH (n)-[:SOURCE|DESTINATION]->(v) " +
                            "WITH n, v, (v.nb_private_neighbors + v.nb_friend_neighbors + v.nb_public_neighbors) as nbNeighbors " +
                            "ORDER BY nbNeighbors DESC " +
                            "WITH reduce(a=\"\", os in collect(v) |  a + \"{{\" + substring(os.label, 0, 20)) as concatContext, n " +
                            "WITH n, substring(concatContext, 2, 51) as context " +
                            "SET n.private_context = context, n.friend_context = context, n.public_context = context"
            );
        }
    }
}
