package org.triple_brain.module.neo4j_graph_manipulator.graph;

import org.neo4j.graphdb.Relationship;
import org.triple_brain.module.model.User;

import java.net.URI;

/*
* Copyright Mozilla Public License 1.1
*/
public interface Neo4JEdgeFactory {
    public Neo4JEdge loadWithRelationshipOfOwner(Relationship relationship, User owner);
    public Neo4JEdge createWithRelationshipAndOwner(Relationship relationship, User owner, URI uri);
}
