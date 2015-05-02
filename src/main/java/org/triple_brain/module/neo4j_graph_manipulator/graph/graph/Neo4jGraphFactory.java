/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package org.triple_brain.module.neo4j_graph_manipulator.graph.graph;

import org.triple_brain.module.model.User;
import org.triple_brain.module.model.UserUris;
import org.triple_brain.module.model.graph.GraphFactory;
import org.triple_brain.module.model.graph.UserGraph;
import org.triple_brain.module.model.graph.vertex.VertexOperator;
import org.triple_brain.module.neo4j_graph_manipulator.graph.graph.vertex.Neo4jVertexFactory;

import javax.inject.Inject;

public class Neo4jGraphFactory implements GraphFactory {

    @Inject
    protected Neo4jUserGraphFactory neo4jUserGraphFactory;

    @Inject
    protected Neo4jVertexFactory vertexFactory;

    @Override
    public UserGraph createForUser(User user) {
        VertexOperator vertex = createDefaultVertexForUser(user);
        vertex.label("me");
        return neo4jUserGraphFactory.withUser(user);
    }

    @Override
    public UserGraph loadForUser(User user) {
        return neo4jUserGraphFactory.withUser(user);
    }

    private VertexOperator createDefaultVertexForUser(User user) {
        VertexOperator operator = vertexFactory.withUri(
                new UserUris(user).generateVertexUri()
        );
        operator.create();
        return operator;
    }
}
