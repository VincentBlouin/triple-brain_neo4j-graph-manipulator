package guru.bubl.module.neo4j_graph_manipulator.graph.graph;

import guru.bubl.module.model.UserUris;
import guru.bubl.module.model.graph.GraphElementOperator;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.group_relation.GroupRelationFactoryNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.vertex.VertexFactoryNeo4j;

import javax.inject.Inject;
import java.net.URI;

public class GraphElementSpecialOperatorFactory {

    @Inject
    protected
    GroupRelationFactoryNeo4j groupRelationFactory;

    @Inject
    protected
    VertexFactoryNeo4j vertexFactory;


    public GraphElementOperator getFromUri(URI uri) {
        if (UserUris.isUriOfAGroupRelation(uri)) {
            return groupRelationFactory.withUri(uri);
        }
        return vertexFactory.withUri(uri);
    }

}
