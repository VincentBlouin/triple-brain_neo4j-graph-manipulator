/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.graph;

import guru.bubl.module.model.User;
import guru.bubl.module.model.UserUris;
import guru.bubl.module.model.center_graph_element.CenterGraphElementOperatorFactory;
import guru.bubl.module.model.graph.GraphFactory;
import guru.bubl.module.model.graph.subgraph.UserGraph;
import guru.bubl.module.model.graph.vertex.VertexOperator;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.vertex.Neo4jVertexFactory;

import javax.inject.Inject;

public class Neo4jGraphFactory implements GraphFactory {

    @Inject
    protected Neo4jUserGraphFactory neo4jUserGraphFactory;

    @Inject
    protected Neo4jVertexFactory vertexFactory;

    @Inject
    protected CenterGraphElementOperatorFactory centerGraphElementOperatorFactory;

    @Override
    public UserGraph createForUser(User user) {
        VertexOperator vertex = vertexFactory.withUri(
                new UserUris(user).generateVertexUri()
        );
        vertex.create();
        centerGraphElementOperatorFactory.usingGraphElement(
                vertex
        ).incrementNumberOfVisits();
        return neo4jUserGraphFactory.withUser(user);
    }

    @Override
    public UserGraph loadForUser(User user) {
        return neo4jUserGraphFactory.withUser(user);
    }

}
