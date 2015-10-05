/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.graph;

import guru.bubl.module.model.User;
import guru.bubl.module.model.UserUris;
import guru.bubl.module.model.center_graph_element.CenterGraphElement;
import guru.bubl.module.model.center_graph_element.CenterGraphElementOperatorFactory;
import guru.bubl.module.model.center_graph_element.CenterGraphElementsOperatorFactory;
import guru.bubl.module.model.graph.GraphFactory;
import guru.bubl.module.model.graph.UserGraph;
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
        VertexOperator vertex = createDefaultVertexForUser(user);
        vertex.label("me");
        centerGraphElementOperatorFactory.usingGraphElement(
                vertex
        ).incrementNumberOfVisits();
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
