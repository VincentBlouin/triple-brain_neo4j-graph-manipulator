package org.triple_brain.module.neo4j_graph_manipulator.graph;

import org.neo4j.graphdb.PropertyContainer;
import org.triple_brain.module.model.User;

import java.net.URI;

/*
* Copyright Mozilla Public License 1.1
*/
public interface Neo4JGraphElementFactory {
    Neo4JGraphElement withPropertyContainerAndOwner(
            PropertyContainer propertyContainer,
            User owner
    );
    Neo4JGraphElement initiatePropertiesAndSetOwner(
            PropertyContainer propertyContainer,
            URI uri,
            User owner
    );
}
