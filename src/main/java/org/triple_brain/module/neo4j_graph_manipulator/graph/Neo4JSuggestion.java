package org.triple_brain.module.neo4j_graph_manipulator.graph;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.codehaus.jettison.json.JSONObject;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.triple_brain.module.common_utils.Uris;
import org.triple_brain.module.model.FriendlyResource;
import org.triple_brain.module.model.Image;
import org.triple_brain.module.model.TripleBrainUris;
import org.triple_brain.module.model.suggestion.Suggestion;
import org.triple_brain.module.model.suggestion.SuggestionOrigin;

import javax.inject.Inject;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.triple_brain.module.model.json.SuggestionJsonFields.*;

/*
* Copyright Mozilla Public License 1.1
*/
public class Neo4JSuggestion implements Suggestion {

    private final String ORIGINS_PROPERTY_NAME = "origins";

    @Inject
    Neo4JUtils neo4JUtils;

    @Inject
    Neo4JFriendlyResourceFactory neo4JFriendlyResourceFactory;

    private Node node;

    @AssistedInject
    protected Neo4JSuggestion(
            @Assisted Node node
    ) {
        this.node = node;
    }

    @AssistedInject
    protected Neo4JSuggestion(
            Neo4JUtils neo4JUtils,
            Neo4JFriendlyResourceFactory neo4JFriendlyResourceFactory,
            @Assisted JSONObject suggestionAsJson
    ) {
        this(
                neo4JUtils,
                neo4JFriendlyResourceFactory,
                URI.create(suggestionAsJson.optString(TYPE_URI)),
                URI.create(suggestionAsJson.optString(DOMAIN_URI)),
                suggestionAsJson.optString(LABEL),
                SuggestionOrigin.valueOf(
                        suggestionAsJson.optString(ORIGIN)
                )
        );
    }

    protected Neo4JSuggestion(
            Neo4JUtils neo4JUtils,
            Neo4JFriendlyResourceFactory neo4JFriendlyResourceFactory,
            URI sameAsUri,
            URI domainUri,
            String label,
            SuggestionOrigin origin
    ) {
        this.node = neo4JUtils.create(
                Uris.get(
                        TripleBrainUris.BASE +
                                "suggestion/" +
                                UUID.randomUUID().toString()
                )
        );
        this.neo4JUtils = neo4JUtils;
        this.neo4JFriendlyResourceFactory = neo4JFriendlyResourceFactory;
        addSameAs(
                sameAsUri,
                label
        );
        addDomain(
                domainUri
        );
        addOrigin(
                origin
        );
    }

    @Override
    public FriendlyResource sameAs() {
        return neo4JFriendlyResourceFactory.createOrLoadFromNode(
                node.getRelationships(
                        Relationships.SAME_AS
                ).iterator().next().getEndNode()
        );
    }

    @Override
    public FriendlyResource domain() {
        return neo4JFriendlyResourceFactory.createOrLoadFromNode(
                node.getRelationships(
                        Relationships.DOMAIN
                ).iterator().next().getEndNode()
        );
    }

    @Override
    public URI uri() {
        return neo4JFriendlyResourceFactory.createOrLoadFromNode(
                node
        ).uri();
    }

    @Override
    public String label() {
        return sameAs().label();
    }

    @Override
    public void label(String label) {
        sameAs().label(
                label
        );
    }

    @Override
    public Set<Image> images() {
        return sameAs().images();
    }

    @Override
    public Boolean gotTheImages() {
        return sameAs().gotTheImages();
    }

    @Override
    public String description() {
        return sameAs().description();
    }

    @Override
    public void description(String description) {
        sameAs().description(
                description
        );
    }

    @Override
    public Boolean gotADescription() {
        return sameAs().gotADescription();
    }

    @Override
    public void addImages(Set<Image> images) {
        sameAs().addImages(
                images
        );
    }

    @Override
    public Set<SuggestionOrigin> origins() {
        Set<SuggestionOrigin> suggestionOrigins = new HashSet<SuggestionOrigin>();
        for(Relationship relationship : node.getRelationships(Relationships.SUGGESTION_ORIGIN)){
            suggestionOrigins.add(
                    suggestionOriginFromNode(
                            relationship.getEndNode()
                    )
            );
        }
        return suggestionOrigins;
    }

    @Override
    public void removeOriginsThatDependOnResource(FriendlyResource resource) {
        Iterable<Relationship> relationshipIt = node.getRelationships(Relationships.SUGGESTION_ORIGIN);
        while(relationshipIt.iterator().hasNext()){
            Relationship relationship = relationshipIt.iterator().next();
            Node suggestionOriginAsNode = relationship.getEndNode();
            SuggestionOrigin suggestionOrigin = suggestionOriginFromNode(
                    suggestionOriginAsNode
            );
            if(suggestionOrigin.isTheIdentificationWithUri(
                    resource.uri()
            )){
                relationship.delete();
                suggestionOriginAsNode.delete();
            }
        }
    }

    @Override
    public void remove() {
        neo4JUtils.removeAllRelationships(node);
        neo4JUtils.removeAllProperties(node);
        node.delete();
    }

    private void addSameAs(URI sameAsUri, String label) {
        Neo4JFriendlyResource sameAs = neo4JFriendlyResourceFactory.createOrLoadFromNode(
                neo4JUtils.getOrCreate(sameAsUri)
        );
        sameAs.label(label);
        node.createRelationshipTo(
                sameAs.getNode(),
                Relationships.SAME_AS
        );
    }

    public Node getNode(){
        return node;
    }

    private void addDomain(URI domainUri) {
        Neo4JFriendlyResource domain = neo4JFriendlyResourceFactory.createOrLoadFromNode(
                neo4JUtils.getOrCreate(domainUri)
        );
        node.createRelationshipTo(
                domain.getNode(),
                Relationships.DOMAIN
        );
    }

    private void addOrigin(SuggestionOrigin suggestionOrigin) {
        node.createRelationshipTo(
                createNodeFromSuggestionOrigin(
                        suggestionOrigin
                ),
                Relationships.SUGGESTION_ORIGIN
        );
    }
    private void setOrigins(Set<SuggestionOrigin> origins) {
        node.setProperty(
                ORIGINS_PROPERTY_NAME,
                origins.toString()
        );
    }

    private SuggestionOrigin suggestionOriginFromNode(Node node){
        return SuggestionOrigin.valueOf(
                node.getProperty(
                        SuggestionOrigin.ORIGIN_PROPERTY
                ).toString()
        );
    }

    private Node createNodeFromSuggestionOrigin(SuggestionOrigin suggestionOrigin){
        Node suggestionOriginAsNode = node.getGraphDatabase().createNode();
        suggestionOriginAsNode.setProperty(
                SuggestionOrigin.ORIGIN_PROPERTY,
                suggestionOrigin.toString()
        );
        return suggestionOriginAsNode;
    }
}
