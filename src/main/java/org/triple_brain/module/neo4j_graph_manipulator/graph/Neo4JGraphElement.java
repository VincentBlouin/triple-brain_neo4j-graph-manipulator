package org.triple_brain.module.neo4j_graph_manipulator.graph;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.joda.time.DateTime;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.triple_brain.module.model.FriendlyResource;
import org.triple_brain.module.model.Image;
import org.triple_brain.module.model.TripleBrainUris;
import org.triple_brain.module.model.UserUris;
import org.triple_brain.module.model.graph.GraphElement;

import javax.inject.Inject;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;

/*
* Copyright Mozilla Public License 1.1
*/
public class Neo4JGraphElement implements GraphElement {

    protected Node node;
    protected Neo4JFriendlyResource friendlyResource;
    protected Neo4JFriendlyResourceFactory friendlyResourceFactory;

    @Inject
    protected Neo4JUtils neo4JUtils;

    @AssistedInject
    protected Neo4JGraphElement(
            Neo4JFriendlyResourceFactory friendlyResourceFactory,
            @Assisted Node node
    ) {
        friendlyResource = friendlyResourceFactory.createOrLoadFromNode(
                node
        );
        this.friendlyResourceFactory = friendlyResourceFactory;
        this.node = node;
    }

    @AssistedInject
    protected Neo4JGraphElement(
            Neo4JUtils utils,
            Neo4JFriendlyResourceFactory friendlyResourceFactory,
            @Assisted URI uri
    ) {
        Node node = utils.getFromUri(uri);
        friendlyResource = friendlyResourceFactory.createOrLoadFromNode(
                node
        );
        this.friendlyResourceFactory = friendlyResourceFactory;
        this.node = node;
    }

    @Override
    public DateTime creationDate() {
        return friendlyResource.creationDate();
    }

    @Override
    public DateTime lastModificationDate() {
        return friendlyResource.lastModificationDate();
    }

    public void updateLastModificationDate() {
        friendlyResource.updateLastModificationDate();
    }

    @Override
    public URI uri() {
        return friendlyResource.uri();
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
    public Boolean gotTheImages() {
        return friendlyResource.gotTheImages();
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
        friendlyResource.addImages(
                images
        );
    }

    @Override
    public boolean hasLabel() {
        return !friendlyResource.label().isEmpty();
    }

    @Override
    public void addGenericIdentification(FriendlyResource friendlyResource) throws IllegalArgumentException{
        ifIdentificationIsGraphElementThrowException(friendlyResource);
        if(getGenericIdentifications().contains(friendlyResource)){
            throw duplicateIdentificationError();
        }
        Node identificationAsNode = neo4JUtils.getFromUri(
                friendlyResource.uri()
        );
        node.createRelationshipTo(
                identificationAsNode,
                Relationships.IDENTIFIED_TO
        );
        updateLastModificationDate();
    }

    @Override
    public Set<FriendlyResource> getGenericIdentifications() {
        Set<FriendlyResource> genericIdentifications = new HashSet<FriendlyResource>();
        for (Relationship relationship : node.getRelationships(Relationships.IDENTIFIED_TO, Direction.OUTGOING)) {
            FriendlyResource genericIdentification = friendlyResourceFactory.createOrLoadFromNode(
                    relationship.getEndNode()
            );
            genericIdentifications.add(genericIdentification);
        }
        return genericIdentifications;
    }

    @Override
    public void addSameAs(FriendlyResource friendlyResource)throws IllegalArgumentException{
        ifIdentificationIsGraphElementThrowException(friendlyResource);
        if(getSameAs().contains(friendlyResource)){
            throw duplicateIdentificationError();
        }
        Node sameAsAsNode = neo4JUtils.getFromUri(
                friendlyResource.uri()
        );
        node.createRelationshipTo(
                sameAsAsNode,
                Relationships.SAME_AS
        );
        updateLastModificationDate();
    }

    @Override
    public Set<FriendlyResource> getSameAs() {
        Set<FriendlyResource> sameAsSet = new HashSet<FriendlyResource>();
        for (Relationship relationship : node.getRelationships(Relationships.SAME_AS, Direction.OUTGOING)) {
            FriendlyResource sameAs = friendlyResourceFactory.createOrLoadFromNode(
                    relationship.getEndNode()
            );
            sameAsSet.add(sameAs);
        }
        return sameAsSet;
    }

    @Override
    public void addType(FriendlyResource type) throws IllegalArgumentException{
        ifIdentificationIsGraphElementThrowException(type);
        if(getAdditionalTypes().contains(type)){
            throw duplicateIdentificationError();
        }
        Node typeAsNode = neo4JUtils.getFromUri(
                type.uri()
        );
        node.createRelationshipTo(
                typeAsNode,
                Relationships.TYPE
        );
        updateLastModificationDate();
    }

    @Override
    public void removeIdentification(FriendlyResource friendlyResource) {
        Node friendlyResourceAsNode = neo4JUtils.getFromUri(
                friendlyResource.uri()
        );
        for (Relationship relationship : node.getRelationships(Direction.OUTGOING)) {
            Node endNode = relationship.getEndNode();
            if (endNode.equals(friendlyResourceAsNode)) {
                relationship.delete();
            }
        }
        updateLastModificationDate();
    }

    @Override
    public Set<FriendlyResource> getAdditionalTypes() {
        Set<FriendlyResource> additionalTypes = new HashSet<FriendlyResource>();
        for (Relationship relationship : node.getRelationships(Relationships.TYPE, Direction.OUTGOING)) {
            FriendlyResource type = friendlyResourceFactory.createOrLoadFromNode(
                    relationship.getEndNode()
            );
            if (!type.uri().toString().equals(TripleBrainUris.TRIPLE_BRAIN_VERTEX)) {
                additionalTypes.add(type);
            }
        }
        return additionalTypes;
    }

    @Override
    public Set<FriendlyResource> getIdentifications() {
        Set<FriendlyResource> identifications = getSameAs();
        identifications.addAll(getAdditionalTypes());
        identifications.addAll(getGenericIdentifications());
        return identifications;
    }

    @Override
    public void remove(){
        for (Relationship relationship : node.getRelationships()) {
            //removing explicitly so node index gets reindexed
            neo4JUtils.removeAllProperties(
                    relationship
            );
            relationship.delete();
        }
        //removing explicitly so node index gets reindexed
        node.removeProperty(Neo4JUserGraph.URI_PROPERTY_NAME);
        node.delete();
    }

    @Override
    public String ownerUsername() {
        return UserUris.ownerUserNameFromUri(
                uri()
        );
    }

    public void ifIdentificationIsGraphElementThrowException(FriendlyResource identification)throws IllegalArgumentException{
        if(identification.equals(this)){
            throw new IllegalArgumentException(
                    "identification cannot be the same"
            );
        }
    }
    public IllegalArgumentException duplicateIdentificationError(){
        return new IllegalArgumentException(
                "cannot have duplicate identifications"
        );
    }

    @Override
    public boolean equals(Object graphElementToCompare) {
        return friendlyResource.equals(graphElementToCompare);
    }

    @Override
    public int hashCode() {
        return friendlyResource.hashCode();
    }
}
