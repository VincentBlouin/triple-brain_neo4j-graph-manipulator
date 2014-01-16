package org.triple_brain.module.neo4j_graph_manipulator.graph;

import org.neo4j.graphdb.Node;
import org.triple_brain.module.model.suggestion.SuggestionFactory;

/*
* Copyright Mozilla Public License 1.1
*/
public interface Neo4jSuggestionFactory extends SuggestionFactory{
    public Neo4jSuggestion getFromNode(Node node);
}
