package guru.bubl.module.neo4j_graph_manipulator.graph.graph;

import guru.bubl.module.model.UserUris;
import guru.bubl.module.model.graph.GraphElementOperator;
import guru.bubl.module.model.graph.edge.EdgeOperatorFactory;
import guru.bubl.module.model.graph.relation.RelationFactory;
import guru.bubl.module.model.graph.relation.RelationOperator;
import guru.bubl.module.model.graph.tag.TagFactory;
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
}
