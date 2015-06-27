/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.graph;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import guru.bubl.module.model.Image;
import guru.bubl.module.model.graph.IdentificationOperator;
import guru.bubl.module.neo4j_graph_manipulator.graph.Neo4jFriendlyResource;
import guru.bubl.module.neo4j_graph_manipulator.graph.Neo4jFriendlyResourceFactory;
import guru.bubl.module.neo4j_graph_manipulator.graph.Neo4jOperator;
import org.neo4j.graphdb.Node;
import org.neo4j.rest.graphdb.query.QueryEngine;

import java.net.URI;
import java.util.Date;
import java.util.Map;
import java.util.Set;

public class Neo4jIdentification implements IdentificationOperator, Neo4jOperator {

    Neo4jFriendlyResource friendlyResourceOperator;
    QueryEngine queryEngine;

    @AssistedInject
    protected Neo4jIdentification(
            QueryEngine queryEngine,
            Neo4jFriendlyResourceFactory friendlyResourceFactory,
            @Assisted Node node
    ){
        this.queryEngine = queryEngine;
        this.friendlyResourceOperator = friendlyResourceFactory.withNode(
                node
        );
    }

    @AssistedInject
    protected Neo4jIdentification(
            QueryEngine queryEngine,
            Neo4jFriendlyResourceFactory friendlyResourceFactory,
            @Assisted URI uri
            ){
        this.queryEngine = queryEngine;
        this.friendlyResourceOperator = friendlyResourceFactory.withUri(
                uri
        );
    }

    @Override
    public URI getExternalResourceUri() {
        return uri();
    }

    @Override
    public URI uri() {
        return friendlyResourceOperator.uri();
    }

    @Override
    public boolean hasLabel() {
        return friendlyResourceOperator.hasLabel();
    }

    @Override
    public String label() {
        return friendlyResourceOperator.label();
    }

    @Override
    public Set<Image> images() {
        return friendlyResourceOperator.images();
    }

    @Override
    public Boolean gotImages() {
        return friendlyResourceOperator.gotImages();
    }

    @Override
    public String comment() {
        return friendlyResourceOperator.comment();
    }

    @Override
    public Boolean gotComments() {
        return friendlyResourceOperator.gotComments();
    }

    @Override
    public Date creationDate() {
        return friendlyResourceOperator.creationDate();
    }

    @Override
    public Date lastModificationDate() {
        return friendlyResourceOperator.lastModificationDate();
    }

    @Override
    public String getOwnerUsername() {
        return friendlyResourceOperator.getOwnerUsername();
    }

    @Override
    public void comment(String comment) {
        friendlyResourceOperator.comment(
                comment
        );
    }

    @Override
    public void label(String label) {
        friendlyResourceOperator.label(
                label
        );
    }

    @Override
    public void addImages(Set<Image> images) {
        friendlyResourceOperator.addImages(
                images
        );
    }

    @Override
    public void create() {
        friendlyResourceOperator.create();
    }

    @Override
    public void createUsingInitialValues(Map<String, Object> values) {
        friendlyResourceOperator.createUsingInitialValues(
                values
        );
    }
    @Override
    public void remove() {
        friendlyResourceOperator.remove();
    }

    @Override
    public String queryPrefix() {
        return friendlyResourceOperator.queryPrefix();
    }

    @Override
    public Node getNode() {
        return friendlyResourceOperator.getNode();
    }

    @Override
    public Map<String, Object> addCreationProperties(Map<String, Object> map) {
        return friendlyResourceOperator.addCreationProperties(map);
    }

    public void updateLastModificationDate() {
        friendlyResourceOperator.updateLastModificationDate();
    }

    @Override
    public boolean equals(Object toCompare) {
        return friendlyResourceOperator.equals(toCompare);
    }

    @Override
    public int hashCode() {
        return friendlyResourceOperator.hashCode();
    }
}
