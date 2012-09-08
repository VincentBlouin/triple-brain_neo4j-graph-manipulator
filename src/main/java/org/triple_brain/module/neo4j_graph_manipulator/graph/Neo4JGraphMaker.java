package org.triple_brain.module.neo4j_graph_manipulator.graph;

import org.neo4j.graphdb.GraphDatabaseService;
import org.triple_brain.module.model.User;
import org.triple_brain.module.model.graph.GraphMaker;
import org.triple_brain.module.model.graph.UserGraph;
import org.triple_brain.module.model.graph.Vertex;

import javax.inject.Inject;

/*
* Copyright Mozilla Public License 1.1
*/
public class Neo4JGraphMaker implements GraphMaker {

    @Inject
    protected GraphDatabaseService graphDb;

    @Inject
    protected Neo4JUserGraphFactory neo4JUserGraphFactory;

    @Override
    public UserGraph createForUser(User user) {
        Vertex vertex = createDefaultVertexForUser(user);
        vertex.label("me");
        return neo4JUserGraphFactory.withUser(user);
    }

    private Vertex createDefaultVertexForUser(User user) {
        return Neo4JVertex.createUsingEmptyNodeUriAndOwner(
                graphDb.createNode(),
                user.defaultVertexUri(),
                user
        );
    }
}
