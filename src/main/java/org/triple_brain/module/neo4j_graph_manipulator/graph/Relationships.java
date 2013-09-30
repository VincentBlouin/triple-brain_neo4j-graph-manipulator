package org.triple_brain.module.neo4j_graph_manipulator.graph;

import org.neo4j.graphdb.RelationshipType;

/*
* Copyright Mozilla Public License 1.1
*/
public enum Relationships implements RelationshipType {
    LABEL, //RDFS.LABEL
    SOURCE_VERTEX,
    DESTINATION_VERTEX,
    TYPE, //RDF.TYPE
    SAME_AS, //OWL2.sameAs
    DOMAIN, //RDFS.DOMAIN
    IDENTIFIED_TO,
    HAS_IMAGE, //todo find common rdf property for image
    SUGGESTION,
    SUGGESTION_ORIGIN,
    HAS_INCLUDED_VERTEX
}
