package org.triple_brain.module.neo4j_graph_manipulator.graph;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.index.ReadableIndex;
import org.triple_brain.module.model.graph.Vertex;

import javax.inject.Inject;

/*
* Copyright Mozilla Public License 1.1
*/
public class Neo4JUtils {

    @Inject
    private ReadableIndex<Node> nodeIndex;

    public void removeAllProperties(PropertyContainer propertyContainer){
        for(String propertyName : propertyContainer.getPropertyKeys()){
            propertyContainer.removeProperty(propertyName);
        }
    }

    public void removeOutgoingNodesRecursively(Node node){
        for(Relationship relationship : node.getRelationships(Direction.OUTGOING)){
            Node endNode =  relationship.getEndNode();
            removeAllProperties(endNode);
            removeAllProperties(relationship);
            relationship.delete();
            removeOutgoingNodesRecursively(endNode);
        }
    }

    public Node nodeOfVertex(Vertex vertex){
        return nodeIndex.get(
                Neo4JUserGraph.URI_PROPERTY_NAME,
                vertex.id()
        ).getSingle();
    }

}
