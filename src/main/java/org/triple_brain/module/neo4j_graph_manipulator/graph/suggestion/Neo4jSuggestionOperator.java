package org.triple_brain.module.neo4j_graph_manipulator.graph.suggestion;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
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
import org.triple_brain.module.model.suggestion.SuggestionPojo;
import org.triple_brain.module.neo4j_graph_manipulator.graph.*;

import javax.inject.Inject;
import java.net.URI;
import java.util.*;

import static org.triple_brain.module.neo4j_graph_manipulator.graph.Neo4jRestApiUtils.map;

/*
* Copyright Mozilla Public License 1.1
*/
public class Neo4jSuggestionOperator implements SuggestionOperator, Neo4jOperator {

    public static URI generateUri(){
        return URI.create(
                TripleBrainUris.BASE +
                        "suggestion/" +
                        UUID.randomUUID().toString()
        );
    }

    @Inject
    Neo4jUtils neo4jUtils;

    @Inject
    Neo4jFriendlyResourceFactory neo4jFriendlyResourceFactory;

    @Inject
    Neo4jSuggestionOriginFactory suggestionOriginFactory;

    protected Node node;

    private Neo4jFriendlyResource friendlyResource;

    @AssistedInject
    protected Neo4jSuggestionOperator(
            Neo4jFriendlyResourceFactory neo4jFriendlyResourceFactory,
            @Assisted Node node
    ) {
        this.node = node;
        this.friendlyResource = neo4jFriendlyResourceFactory.withNode(
                node
        );
    }

    @AssistedInject
    protected Neo4jSuggestionOperator(
            Neo4jUtils neo4jUtils,
            Neo4jFriendlyResourceFactory neo4jFriendlyResourceFactory,
            Neo4jSuggestionOriginFactory suggestionOriginFactory,
            @Assisted SuggestionPojo suggestionPojo
    ) {
        this(
                neo4jUtils,
                neo4jFriendlyResourceFactory,
                suggestionOriginFactory,
                suggestionPojo.sameAs().uri(),
                suggestionPojo.domain().uri(),
                suggestionPojo.label(),
                suggestionPojo.origins().iterator().next().toString()
        );
    }

    protected Neo4jSuggestionOperator(
            Neo4jUtils neo4jUtils,
            Neo4jFriendlyResourceFactory neo4jFriendlyResourceFactory,
            Neo4jSuggestionOriginFactory suggestionOriginFactory,
            URI sameAsUri,
            URI domainUri,
            String label,
            String origin
    ) {

        this.friendlyResource = neo4jFriendlyResourceFactory.withUri(
            generateUri()
        );
        friendlyResource.create();
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
    public FriendlyResourceOperator sameAs() {
        return neo4jFriendlyResourceFactory.withNode(
                getNode().getRelationships(
                        Relationships.SAME_AS
                ).iterator().next().getEndNode()
        );
    }

    @Override
    public FriendlyResourceOperator domain() {
        return neo4jFriendlyResourceFactory.withNode(
                getNode().getRelationships(
                        Relationships.DOMAIN
                ).iterator().next().getEndNode()
        );
    }

    @Override
    public URI uri() {
        return friendlyResource.uri();
    }

    @Override
    public boolean hasLabel() {
        return sameAs().hasLabel();
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
    public Boolean gotImages() {
        return sameAs().gotImages();
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
    public void create() {
        friendlyResource.createUsingInitialValues(
                map()
        );
    }

    @Override
    public void createUsingInitialValues(Map<String, Object> values) {
        friendlyResource.createUsingInitialValues(values);
    }

    @Override
    public Date creationDate() {
        return friendlyResource.creationDate();
    }

    @Override
    public Date lastModificationDate() {
        return friendlyResource.lastModificationDate();
    }

    @Override
    public Set<SuggestionOrigin> origins() {
        Set<SuggestionOrigin> suggestionOrigins = new HashSet<SuggestionOrigin>();
        for(Relationship relationship : getNode().getRelationships(Relationships.SUGGESTION_ORIGIN)){
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
        Iterable<Relationship> relationshipIt = getNode().getRelationships(
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
        neo4jUtils.removeAllRelationships(getNode());
        neo4jUtils.removeAllProperties(getNode());
        getNode().delete();
    }

    private void addSameAs(URI sameAsUri, String label) {
        Neo4jFriendlyResource sameAs = neo4jFriendlyResourceFactory.withUri(
                sameAsUri
        );
        if(!neo4jUtils.alreadyExists(sameAsUri)){
            sameAs.create();
        }
        sameAs.label(label);
        getNode().createRelationshipTo(
                sameAs.getNode(),
                Relationships.SAME_AS
        );
    }

    @Override
    public String queryPrefix() {
        return friendlyResource.queryPrefix();
    }

    @Override
    public Node getNode(){
        if (null == node) {
            return friendlyResource.getNode();
        }
        return node;
    }

    @Override
    public Map<String, Object> addCreationProperties(Map<String, Object> map) {
        return friendlyResource.addCreationProperties(
                map
        );
    }

    private void addDomain(URI domainUri) {
        Neo4jFriendlyResource domain = neo4jFriendlyResourceFactory.withUri(
                domainUri
        );
        if(!neo4jUtils.alreadyExists(domainUri)){
            domain.create();
        }
        getNode().createRelationshipTo(
                domain.getNode(),
                Relationships.DOMAIN
        );
    }

}
