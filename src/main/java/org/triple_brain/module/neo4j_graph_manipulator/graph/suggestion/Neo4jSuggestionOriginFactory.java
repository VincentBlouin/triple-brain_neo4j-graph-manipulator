package org.triple_brain.module.neo4j_graph_manipulator.graph.suggestion;

import org.neo4j.graphdb.Node;

/*
* Copyright Mozilla Public License 1.1
*/
public interface Neo4jSuggestionOriginFactory {
    public Neo4jSuggestionOriginOperator loadFromNode(Node node);
    public Neo4jSuggestionOriginOperator createFromStringAndSuggestion(
            String origin,
            Neo4jSuggestionOperator suggestion
    );
}
