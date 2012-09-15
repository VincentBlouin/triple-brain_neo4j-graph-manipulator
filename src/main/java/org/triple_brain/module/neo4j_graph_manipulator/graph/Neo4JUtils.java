package org.triple_brain.module.neo4j_graph_manipulator.graph;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;

/*
* Copyright Mozilla Public License 1.1
*/
public class Neo4JUtils {

    public static void removeAllProperties(PropertyContainer propertyContainer){
        for(String propertyName : propertyContainer.getPropertyKeys()){
            propertyContainer.removeProperty(propertyName);
        }
    }

    public static void removeOutgoingNodesRecursively(Node node){
        for(Relationship relationship : node.getRelationships(Direction.OUTGOING)){
            Node endNode =  relationship.getEndNode();
            removeAllProperties(endNode);
            removeAllProperties(relationship);
            relationship.delete();
            removeOutgoingNodesRecursively(endNode);
        }

    }
}
