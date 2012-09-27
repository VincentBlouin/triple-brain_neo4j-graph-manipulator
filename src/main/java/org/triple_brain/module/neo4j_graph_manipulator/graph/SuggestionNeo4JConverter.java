package org.triple_brain.module.neo4j_graph_manipulator.graph;

import com.hp.hpl.jena.vocabulary.RDFS;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;
import org.triple_brain.module.common_utils.Uris;
import org.triple_brain.module.model.Suggestion;
import org.triple_brain.module.model.TripleBrainUris;

import javax.inject.Inject;
import java.net.URI;
import java.util.UUID;

/*
* Copyright Mozilla Public License 1.1
*/
public class SuggestionNeo4JConverter {

    @Inject
    Neo4JExternalResourceUtils externalResourceUtils;

    public Node createSuggestion(Suggestion suggestion) {
        Node node = externalResourceUtils.create(
                Uris.get(
                        TripleBrainUris.BASE + "suggestion/" + UUID.randomUUID().toString()
                )
        );
        addType(node, suggestion);
        addDomain(node, suggestion);
        addLabel(node, suggestion);
        return node;
    }

    private void addType(Node suggestionAsNode, Suggestion suggestion) {
        Node suggestionType = externalResourceUtils.getOrCreateNodeWithUri(
                suggestion.typeUri()
        );
        suggestionAsNode.createRelationshipTo(suggestionType, Relationships.TYPE);
    }

    private void addDomain(Node suggestionAsNode, Suggestion suggestion) {
        Node suggestionDomain = externalResourceUtils.getOrCreateNodeWithUri(
                suggestion.domainUri()
        );
        suggestionAsNode.createRelationshipTo(suggestionDomain, Relationships.DOMAIN);
    }

    private void addLabel(Node suggestionAsNode, Suggestion suggestion) {
        suggestionAsNode.setProperty(
                RDFS.label.getURI(),
                suggestion.label()
        );
    }

    public Suggestion nodeToSuggestion(Node node) {
        return Suggestion.withTypeDomainAndLabel(
                getTypeUri(node),
                getDomainUri(node),
                getLabel(node)
        );
    }

    private String getLabel(Node node) {
        return node.getProperty(RDFS.label.getURI()).toString();
    }

    private URI getTypeUri(Node node) {
        return getUriOfEndNodeUsingRelationship(node, Relationships.TYPE);
    }

    private URI getDomainUri(Node node) {
        return getUriOfEndNodeUsingRelationship(node, Relationships.DOMAIN);
    }

    private URI getUriOfEndNodeUsingRelationship(Node node, RelationshipType relationshipType){
        return Uris.get(node
                .getSingleRelationship(relationshipType, Direction.OUTGOING)
                .getEndNode()
                .getProperty(
                        Neo4JUserGraph.URI_PROPERTY_NAME
                ).toString()
        );
    }

}
