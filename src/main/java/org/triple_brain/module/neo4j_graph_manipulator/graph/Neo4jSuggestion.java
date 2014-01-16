package org.triple_brain.module.neo4j_graph_manipulator.graph;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.codehaus.jettison.json.JSONObject;
import org.joda.time.DateTime;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.triple_brain.module.common_utils.Uris;
import org.triple_brain.module.model.FriendlyResource;
import org.triple_brain.module.model.Image;
import org.triple_brain.module.model.TripleBrainUris;
import org.triple_brain.module.model.graph.FriendlyResourceOperator;
import org.triple_brain.module.model.suggestion.SuggestionOperator;
import org.triple_brain.module.model.suggestion.SuggestionOrigin;
import org.triple_brain.module.model.suggestion.SuggestionOriginOperator;

import javax.inject.Inject;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.triple_brain.module.model.json.SuggestionJsonFields.*;

/*
* Copyright Mozilla Public License 1.1
*/
public class Neo4jSuggestion implements SuggestionOperator{

    @Inject
    Neo4jUtils neo4jUtils;

    @Inject
    Neo4jFriendlyResourceFactory neo4jFriendlyResourceFactory;

    @Inject
    Neo4jSuggestionOriginFactory suggestionOriginFactory;

    protected Node node;

    private FriendlyResource friendlyResource;

    @AssistedInject
    protected Neo4jSuggestion(
            Neo4jFriendlyResourceFactory neo4jFriendlyResourceFactory,
            @Assisted Node node
    ) {
        this.node = node;
        this.friendlyResource = neo4jFriendlyResourceFactory.createOrLoadFromNode(
                node
        );
    }

    @AssistedInject
    protected Neo4jSuggestion(
            Neo4jUtils neo4jUtils,
            Neo4jFriendlyResourceFactory neo4jFriendlyResourceFactory,
            Neo4jSuggestionOriginFactory suggestionOriginFactory,
            @Assisted JSONObject suggestionAsJson
    ) {
        this(
                neo4jUtils,
                neo4jFriendlyResourceFactory,
                suggestionOriginFactory,
                URI.create(suggestionAsJson.optString(TYPE_URI)),
                URI.create(suggestionAsJson.optString(DOMAIN_URI)),
                suggestionAsJson.optString(LABEL),
                suggestionAsJson.optString(ORIGIN)
        );
    }

    protected Neo4jSuggestion(
            Neo4jUtils neo4jUtils,
            Neo4jFriendlyResourceFactory neo4jFriendlyResourceFactory,
            Neo4jSuggestionOriginFactory suggestionOriginFactory,
            URI sameAsUri,
            URI domainUri,
            String label,
            String origin
    ) {
        this(
                neo4jFriendlyResourceFactory,
                neo4jUtils.create(
                    Uris.get(
                            TripleBrainUris.BASE +
                                    "suggestion/" +
                                    UUID.randomUUID().toString()
                    )
                )
        );
        this.neo4jUtils = neo4jUtils;
        this.neo4jFriendlyResourceFactory = neo4jFriendlyResourceFactory;
        addSameAs(
                sameAsUri,
                label
        );
        addDomain(
                domainUri
        );
        suggestionOriginFactory.createFromStringAndSuggestion(
                origin,
                this
        );
    }

    @Override
    public FriendlyResource sameAs() {
        return neo4jFriendlyResourceFactory.createOrLoadFromNode(
                node.getRelationships(
                        Relationships.SAME_AS
                ).iterator().next().getEndNode()
        );
    }

    @Override
    public FriendlyResource domain() {
        return neo4jFriendlyResourceFactory.createOrLoadFromNode(
                node.getRelationships(
                        Relationships.DOMAIN
                ).iterator().next().getEndNode()
        );
    }

    @Override
    public URI uri() {
        return neo4jFriendlyResourceFactory.createOrLoadFromNode(
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
    public String comment() {
        return sameAs().comment();
    }

    @Override
    public void comment(String comment) {
        sameAs().comment(
                comment
        );
    }

    @Override
    public Boolean gotComments() {
        return sameAs().gotComments();
    }

    @Override
    public void addImages(Set<Image> images) {
        FriendlyResourceOperator sameAsOperator = (FriendlyResourceOperator) sameAs();
        sameAsOperator.addImages(
                images
        );
    }

    @Override
    public DateTime creationDate() {
        return friendlyResource.creationDate();
    }

    @Override
    public DateTime lastModificationDate() {
        return friendlyResource.lastModificationDate();
    }

    @Override
    public Set<SuggestionOrigin> origins() {
        Set<SuggestionOrigin> suggestionOrigins = new HashSet<SuggestionOrigin>();
        for(Relationship relationship : node.getRelationships(Relationships.SUGGESTION_ORIGIN)){
            suggestionOrigins.add(
                    suggestionOriginFactory.loadFromNode(
                            relationship.getEndNode()
                    )
            );
        }
        return suggestionOrigins;
    }

    @Override
    public void removeOriginsThatDependOnResource(FriendlyResource resource) {
        Iterable<Relationship> relationshipIt = node.getRelationships(
                Relationships.SUGGESTION_ORIGIN
        );
        while(relationshipIt.iterator().hasNext()){
            Relationship relationship = relationshipIt.iterator().next();
            SuggestionOriginOperator suggestionOrigin = suggestionOriginFactory.loadFromNode(
                    relationship.getEndNode()
            );
            if(suggestionOrigin.isRelatedToFriendlyResource(
                    resource
            )){
                relationship.delete();
                suggestionOrigin.remove();
            }
        }
    }

    @Override
    public void remove() {
        neo4jUtils.removeAllRelationships(node);
        neo4jUtils.removeAllProperties(node);
        node.delete();
    }

    private void addSameAs(URI sameAsUri, String label) {
        Neo4jFriendlyResource sameAs = neo4jFriendlyResourceFactory.createOrLoadFromNode(
                neo4jUtils.getOrCreate(sameAsUri)
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
        Neo4jFriendlyResource domain = neo4jFriendlyResourceFactory.createOrLoadFromNode(
                neo4jUtils.getOrCreate(domainUri)
        );
        node.createRelationshipTo(
                domain.getNode(),
                Relationships.DOMAIN
        );
    }
}
