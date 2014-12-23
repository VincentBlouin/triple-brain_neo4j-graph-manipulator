/*
 * Copyright Vincent Blouin under the Mozilla Public License 1.1
 */

package org.triple_brain.module.neo4j_graph_manipulator.graph.graph;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.neo4j.graphdb.Node;
import org.neo4j.rest.graphdb.query.QueryEngine;
import org.neo4j.rest.graphdb.util.QueryResult;
import org.triple_brain.module.model.Image;
import org.triple_brain.module.model.graph.Identification;
import org.triple_brain.module.model.graph.IdentificationOperator;
import org.triple_brain.module.model.json.IdentificationJson;
import org.triple_brain.module.neo4j_graph_manipulator.graph.Neo4jFriendlyResource;
import org.triple_brain.module.neo4j_graph_manipulator.graph.Neo4jFriendlyResourceFactory;
import org.triple_brain.module.neo4j_graph_manipulator.graph.Neo4jOperator;
import scala.util.parsing.combinator.testing.Ident;

import java.net.URI;
import java.util.*;

import static org.triple_brain.module.neo4j_graph_manipulator.graph.Neo4jRestApiUtils.map;

public class Neo4jIdentification implements IdentificationOperator, Neo4jOperator {

    public enum props {
        external_uri
    }

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
        String query = friendlyResourceOperator.queryPrefix() + "return n."
                + props.external_uri + " as external_uri";
        QueryResult<Map<String, Object>> result = queryEngine.query(
                query,
                map()
        );
        Iterator<Map<String, Object>> it = result.iterator();
        Object externalUriValue = it.next().get("external_uri");
        return externalUriValue == null ? friendlyResourceOperator.uri() : URI.create(
                externalUriValue.toString()
        );
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
