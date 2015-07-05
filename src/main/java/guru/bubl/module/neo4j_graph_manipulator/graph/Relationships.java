/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph;

import guru.bubl.module.model.graph.IdentificationType;
import org.neo4j.graphdb.RelationshipType;

public enum Relationships implements RelationshipType {
    LABEL, //RDFS.LABEL
    SOURCE_VERTEX,
    DESTINATION_VERTEX,
    HAS_TYPE, //RDF.TYPE
    SAME_AS, //OWL2.sameAs
    DOMAIN, //RDFS.DOMAIN
    IDENTIFIED_TO,
    HAS_IMAGE, //todo find common rdf property for image
    SUGGESTION,
    SUGGESTION_ORIGIN,
    HAS_INCLUDED_VERTEX,
    HAS_INCLUDED_EDGE,
    HAS_PROPERTY;

    public static IdentificationType getIdentificationTypeForRelationship(Relationships relationship) {
        switch (relationship) {
            case SAME_AS:
                return IdentificationType.same_as;
            case HAS_TYPE:
                return IdentificationType.type;
        }
        return IdentificationType.generic;
    }
    public static final String[] IDENTIFICATION_RELATIONSHIPS = {
            HAS_TYPE.name(),
            SAME_AS.name(),
            IDENTIFIED_TO.name()
    };
}