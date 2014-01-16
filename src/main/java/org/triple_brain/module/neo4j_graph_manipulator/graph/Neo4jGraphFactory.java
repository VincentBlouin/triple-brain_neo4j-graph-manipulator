package org.triple_brain.module.neo4j_graph_manipulator.graph;

import org.neo4j.graphdb.GraphDatabaseService;
import org.triple_brain.module.model.User;
import org.triple_brain.module.model.UserUris;
import org.triple_brain.module.model.graph.GraphFactory;
import org.triple_brain.module.model.graph.UserGraph;
import org.triple_brain.module.model.graph.vertex.Vertex;

import javax.inject.Inject;

/*
* Copyright Mozilla Public License 1.1
*/
public class Neo4jGraphFactory implements GraphFactory {

    @Inject
    protected GraphDatabaseService graphDb;

    @Inject
    protected Neo4jUserGraphFactory neo4jUserGraphFactory;

    @Inject
    protected Neo4jVertexFactory vertexFactory;

    @Override
    public UserGraph createForUser(User user) {
        Vertex vertex = createDefaultVertexForUser(user);
        vertex.label("me");
        return neo4jUserGraphFactory.withUser(user);
    }

    @Override
    public UserGraph loadForUser(User user) {
        return neo4jUserGraphFactory.withUser(user);
    }

    private Vertex createDefaultVertexForUser(User user) {
        return vertexFactory.createOrLoadUsingUri(
                new UserUris(user).defaultVertexUri()
        );
    }
}
