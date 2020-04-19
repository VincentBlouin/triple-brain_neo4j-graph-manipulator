package guru.bubl.module.neo4j_graph_manipulator.graph.graph.graph_element;

import guru.bubl.module.model.graph.fork.ForkOperator;
import guru.bubl.module.model.graph.graph_element.GraphElementOperator;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.group_relation.GroupRelationFactoryNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.relation.RelationFactoryNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.vertex.VertexFactoryNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.tag.TagFactoryNeo4J;

import javax.inject.Inject;
import java.net.URI;

import static guru.bubl.module.model.UserUris.getGraphElementTypeFromUri;

public class GraphElementSpecialOperatorFactory {

    @Inject
    private GroupRelationFactoryNeo4j groupRelationFactory;

    @Inject
    private VertexFactoryNeo4j vertexFactory;

    @Inject
    private TagFactoryNeo4J tagFactoryNeo4J;

    @Inject
    private RelationFactoryNeo4j relationOperator;

    public GraphElementOperator getFromUri(URI uri) {
        switch (getGraphElementTypeFromUri(uri)) {
            case Vertex:
                return vertexFactory.withUri(uri);
            case GroupRelation:
                return groupRelationFactory.withUri(uri);
            case Edge:
                return relationOperator.withUri(uri);
            case Meta:
                return tagFactoryNeo4J.withUri(uri);
            default:
                return null;
        }
    }

    public ForkOperator getForkFromUri(URI uri) {
        switch (getGraphElementTypeFromUri(uri)) {
            case Vertex:
                return vertexFactory.withUri(uri);
            case GroupRelation:
                return groupRelationFactory.withUri(uri);
            case Meta:
                return tagFactoryNeo4J.withUri(uri);
            default:
                return null;
        }
    }
}
