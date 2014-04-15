package org.triple_brain.module.neo4j_graph_manipulator.graph.suggestion;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.neo4j.graphdb.Node;
import org.triple_brain.module.model.FriendlyResource;
import org.triple_brain.module.model.Image;
import org.triple_brain.module.model.suggestion.Suggestion;
import org.triple_brain.module.model.suggestion.SuggestionOrigin;
import org.triple_brain.module.model.suggestion.SuggestionOriginOperator;
import org.triple_brain.module.neo4j_graph_manipulator.graph.Neo4jFriendlyResource;
import org.triple_brain.module.neo4j_graph_manipulator.graph.Neo4jFriendlyResourceFactory;
import org.triple_brain.module.neo4j_graph_manipulator.graph.Neo4jOperator;
import org.triple_brain.module.neo4j_graph_manipulator.graph.Relationships;

import java.net.URI;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.triple_brain.module.neo4j_graph_manipulator.graph.Neo4jRestApiUtils.map;

/*
* Copyright Mozilla Public License 1.1
*/
public class Neo4jSuggestionOriginOperator implements SuggestionOriginOperator, Neo4jOperator{

    public static final String ORIGIN_PROPERTY = "origin";

    public static URI generateUriBasedOnSuggestion(Suggestion suggestion){
        return URI.create(
                suggestion.uri() + "/origin/" +
                        UUID.randomUUID().toString()
        );
    }

    protected Neo4jFriendlyResource friendlyResource;

    @AssistedInject
    protected Neo4jSuggestionOriginOperator(
            Neo4jFriendlyResourceFactory neo4jFriendlyResourceFactory,
            @Assisted Node node
    ){
        this.friendlyResource = neo4jFriendlyResourceFactory.withNode(
                node
        );
    }

    @AssistedInject
    protected Neo4jSuggestionOriginOperator(
            Neo4jFriendlyResourceFactory neo4jFriendlyResourceFactory,
            @Assisted String origin,
            @Assisted Neo4jSuggestionOperator suggestion
    ){
        this.friendlyResource = neo4jFriendlyResourceFactory.withUri(
                generateUriBasedOnSuggestion(
                        suggestion
                )
        );
        friendlyResource.create();
        suggestion.getNode().createRelationshipTo(
                friendlyResource.getNode(),
                Relationships.SUGGESTION_ORIGIN
        );
        setOrigin(origin);
    }

    @Override
    public Boolean isRelatedToFriendlyResource(FriendlyResource friendlyResource) {
        return getOrigin().contains(
                friendlyResource.uri().toString()
        );
    }

    @Override
    public void remove() {
        friendlyResource.getNode().delete();
    }

    @Override
    public String toString(){
        return getOrigin();
    }

    @Override
    public boolean equals(Object originToCompareAsObject) {
        SuggestionOrigin originToCompare = (SuggestionOrigin) originToCompareAsObject;
        return getOrigin().equals(originToCompare.toString());
    }

    @Override
    public int hashCode() {
        return getOrigin().hashCode();
    }

    private void setOrigin(String origin){
        friendlyResource.getNode().setProperty(
                ORIGIN_PROPERTY,
                origin
        );
    }

    public String getOrigin(){
        return friendlyResource.getNode().getProperty(
                ORIGIN_PROPERTY
        ).toString();
    }

    @Override
    public URI uri() {
        return friendlyResource.uri();
    }

    @Override
    public boolean hasLabel() {
        return friendlyResource.hasLabel();
    }

    @Override
    public String label() {
        return friendlyResource.label();
    }

    @Override
    public void label(String label) {
        friendlyResource.label(
                label
        );
    }

    @Override
    public Set<Image> images() {
        return friendlyResource.images();
    }

    @Override
    public Boolean gotImages() {
        return friendlyResource.gotImages();
    }

    @Override
    public String comment() {
        return friendlyResource.comment();
    }

    @Override
    public void comment(String comment) {
        friendlyResource.comment(
                comment
        );
    }

    @Override
    public Boolean gotComments() {
        return friendlyResource.gotComments();
    }

    @Override
    public void addImages(Set<Image> images) {
        friendlyResource.addImages(images);
    }

    @Override
    public void create() {
        createUsingInitialValues(
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
    public String queryPrefix() {
        return friendlyResource.queryPrefix();
    }

    @Override
    public Node getNode() {
        return friendlyResource.getNode();
    }

    @Override
    public Map<String, Object> addCreationProperties(Map<String, Object> map) {
        return friendlyResource.addCreationProperties(map);
    }

}
