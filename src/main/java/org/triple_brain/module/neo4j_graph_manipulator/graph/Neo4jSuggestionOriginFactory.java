package org.triple_brain.module.neo4j_graph_manipulator.graph;

import org.neo4j.graphdb.Node;

/*
* Copyright Mozilla Public License 1.1
*/
public interface Neo4jSuggestionOriginFactory {
    public Neo4jSuggestionOrigin loadFromNode(Node node);
    public Neo4jSuggestionOrigin createFromStringAndSuggestion(
            String origin,
            Neo4jSuggestion suggestion
    );
}
