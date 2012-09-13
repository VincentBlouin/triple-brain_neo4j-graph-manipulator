package org.triple_brain.module.neo4j_graph_manipulator.graph;

import org.neo4j.graphdb.RelationshipType;

/*
* Copyright Mozilla Public License 1.1
*/
public enum Relationships implements RelationshipType {
    TRIPLE_BRAIN_EDGE,
    TYPE,
    LABEL,
    SUGGESTION
}
