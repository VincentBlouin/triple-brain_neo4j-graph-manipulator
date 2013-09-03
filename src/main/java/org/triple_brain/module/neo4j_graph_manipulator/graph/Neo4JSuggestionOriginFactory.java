package org.triple_brain.module.neo4j_graph_manipulator.graph;

import org.neo4j.graphdb.Node;

/*
* Copyright Mozilla Public License 1.1
*/
public interface Neo4JSuggestionOriginFactory {
    public Neo4JSuggestionOrigin loadFromNode(Node node);
    public Neo4JSuggestionOrigin createFromStringAndSuggestion(
            String origin,
            Neo4JSuggestion suggestion
    );
}
