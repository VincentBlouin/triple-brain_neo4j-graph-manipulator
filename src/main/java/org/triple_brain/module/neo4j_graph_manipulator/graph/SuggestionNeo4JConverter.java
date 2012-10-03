package org.triple_brain.module.neo4j_graph_manipulator.graph;

import com.hp.hpl.jena.vocabulary.RDFS;
import org.neo4j.graphdb.Node;
import org.triple_brain.module.common_utils.Misc;
import org.triple_brain.module.common_utils.Uris;
import org.triple_brain.module.model.suggestion.PersistedSuggestion;
import org.triple_brain.module.model.suggestion.Suggestion;
import org.triple_brain.module.model.suggestion.SuggestionOrigin;
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

    @Inject
    Neo4JUtils neo4JUtils;

    private final String ORIGINS_PROPERTY_NAME = "origins";

    public Node createSuggestion(Suggestion suggestion) {
        Node node = externalResourceUtils.create(
                Uris.get(
                        TripleBrainUris.BASE + "suggestion/" + UUID.randomUUID().toString()
                )
        );
        addSameAs(node, suggestion);
        addDomain(node, suggestion);
        addLabel(node, suggestion);
        addOrigins(node, suggestion);
        return node;
    }

    public void remove(PersistedSuggestion suggestion){
        Node node = neo4JUtils.nodeOfUri(
                suggestion.id()
        );
        neo4JUtils.removeAllRelationships(node);
        neo4JUtils.removeAllProperties(node);
        node.delete();
    }

    private void addSameAs(Node suggestionAsNode, Suggestion suggestion) {
        Node suggestionType = externalResourceUtils.getOrCreateNodeWithUri(
                suggestion.sameAsUri()
        );
        suggestionAsNode.createRelationshipTo(suggestionType, Relationships.SAME_AS);
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

    private void addOrigins(Node suggestionAsNode, Suggestion suggestion) {
        suggestionAsNode.setProperty(
                ORIGINS_PROPERTY_NAME,
                suggestion.origins().toString()
        );
    }

    public PersistedSuggestion nodeToSuggestion(Node node) {
        Suggestion suggestion = Suggestion.withSameAsDomainLabelAndOrigins(
                getSameAsUri(node),
                getDomainUri(node),
                getLabel(node),
                getOrigins(node)
        );
        URI id = neo4JUtils.uriOfNode(node);
        return PersistedSuggestion.withSuggestionAndItsId(
                suggestion,
                id
        );
    }

    private String getLabel(Node node) {
        return node.getProperty(
                RDFS.label.getURI()
        ).toString();
    }

    private SuggestionOrigin[] getOrigins(Node node) {
        String setAsString = node.getProperty(
                ORIGINS_PROPERTY_NAME
        ).toString();

        String[] suggestionOriginsAsString = Misc.setAsStringToArray(
                setAsString
        );
        SuggestionOrigin[] suggestionOrigins = new SuggestionOrigin
                [suggestionOriginsAsString.length];
        for(int i = 0 ; i< suggestionOriginsAsString.length; i++){
            String suggestionOriginAsString = suggestionOriginsAsString[i];
            suggestionOrigins[i] = new SuggestionOrigin(suggestionOriginAsString);
        }
        return suggestionOrigins;
    }

    private URI getSameAsUri(Node node) {
        return neo4JUtils
                .getUriOfEndNodeUsingRelationship(node, Relationships.SAME_AS);
    }

    private URI getDomainUri(Node node) {
        return neo4JUtils
                .getUriOfEndNodeUsingRelationship(node, Relationships.DOMAIN);
    }



}
