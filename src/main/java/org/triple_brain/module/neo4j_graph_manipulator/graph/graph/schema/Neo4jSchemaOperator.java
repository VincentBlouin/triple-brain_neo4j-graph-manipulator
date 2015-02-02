/*
 * Copyright Vincent Blouin under the Mozilla Public License 1.1
 */

package org.triple_brain.module.neo4j_graph_manipulator.graph.graph.schema;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.neo4j.graphdb.Node;
import org.neo4j.rest.graphdb.query.QueryEngine;
import org.neo4j.rest.graphdb.util.QueryResult;
import org.triple_brain.module.model.Image;
import org.triple_brain.module.model.UserUris;
import org.triple_brain.module.model.graph.GraphElement;
import org.triple_brain.module.model.graph.GraphElementOperator;
import org.triple_brain.module.model.graph.schema.SchemaOperator;
import org.triple_brain.module.neo4j_graph_manipulator.graph.Neo4jFriendlyResource;
import org.triple_brain.module.neo4j_graph_manipulator.graph.Neo4jFriendlyResourceFactory;
import org.triple_brain.module.neo4j_graph_manipulator.graph.Neo4jOperator;
import org.triple_brain.module.neo4j_graph_manipulator.graph.Relationships;
import org.triple_brain.module.neo4j_graph_manipulator.graph.graph.Neo4jGraphElementFactory;
import org.triple_brain.module.neo4j_graph_manipulator.graph.graph.Neo4jGraphElementOperator;

import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.triple_brain.module.neo4j_graph_manipulator.graph.Neo4jRestApiUtils.map;
import static org.triple_brain.module.neo4j_graph_manipulator.graph.Neo4jRestApiUtils.wrap;

public class Neo4jSchemaOperator implements SchemaOperator, Neo4jOperator {

    public static final String NEO4J_LABEL_NAME = "schema";
    public static final String NEO4J_PROPERTY_LABEL_NAME = "property";

    protected Neo4jFriendlyResource friendlyResourceOperator;
    protected Neo4jGraphElementFactory graphElementFactory;
    protected QueryEngine<Map<String, Object>> queryEngine;

    @AssistedInject
    protected Neo4jSchemaOperator(
            Neo4jFriendlyResourceFactory friendlyResourceFactory,
            QueryEngine queryEngine,
            Neo4jGraphElementFactory graphElementFactory,
            @Assisted URI uri
    ) {
        this.queryEngine = queryEngine;
        this.graphElementFactory = graphElementFactory;
        friendlyResourceOperator = friendlyResourceFactory.withUri(uri);
    }

    @AssistedInject
    protected Neo4jSchemaOperator(
            Neo4jFriendlyResourceFactory friendlyResourceFactory,
            QueryEngine queryEngine,
            Neo4jGraphElementFactory graphElementFactory,
            @Assisted String ownerUserName
    ) {
        this(
                friendlyResourceFactory,
                queryEngine,
                graphElementFactory,
                new UserUris(ownerUserName).generateSchemaUri()
        );
        create();
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
    public String queryPrefix() {
        return friendlyResourceOperator.queryPrefix();
    }

    @Override
    public Node getNode() {
        return friendlyResourceOperator.getNode();
    }

    @Override
    public Map<String, Object> addCreationProperties(Map<String, Object> map) {
        Map<String, Object> newMap = map(
                Neo4jFriendlyResource.props.type.name(), Neo4jFriendlyResource.type.schema.name()
        );
        newMap.putAll(
                map
        );
        newMap = friendlyResourceOperator.addCreationProperties(
                newMap
        );
        return newMap;
    }

    @Override
    public void comment(String comment) {
        friendlyResourceOperator.comment(comment);
    }

    @Override
    public void label(String label) {
        friendlyResourceOperator.label(label);
    }

    @Override
    public void addImages(Set<Image> images) {
        friendlyResourceOperator.addImages(images);
    }

    @Override
    public void create() {
        createUsingInitialValues(map());
    }

    @Override
    public void createUsingInitialValues(Map<String, Object> values) {
        Map<String, Object> props = addCreationProperties(
                values
        );
        queryEngine.query(
                "create (n:" + NEO4J_LABEL_NAME + " {props})", wrap(props)
        );
    }

    @Override
    public void remove() {
        //cannot remove schema for now
    }

    @Override
    public GraphElementOperator addProperty() {
        URI createdUri = UserUris.generateSchemaPropertyUri(uri());
        Neo4jGraphElementOperator property = graphElementFactory.withUri(createdUri);
        queryEngine.query(
                queryPrefix() +
                        "CREATE (p:" + NEO4J_PROPERTY_LABEL_NAME + " {props}) " +
                        "CREATE UNIQUE " +
                        "n-[:" + Relationships.HAS_PROPERTY + "]->p ",
                map(
                        "props",
                        property.addCreationProperties(map(
                                Neo4jFriendlyResource.props.type.name(), Neo4jFriendlyResource.type.property.name()
                        ))
                )
        );
        return property;
    }

    @Override
    public boolean equals(Object graphElementToCompareAsObject) {
        return friendlyResourceOperator.equals(graphElementToCompareAsObject);
    }

    @Override
    public int hashCode() {
        return friendlyResourceOperator.hashCode();
    }

    @Override
    public Map<URI, ? extends GraphElement> getProperties() {
        Map<URI, GraphElementOperator> properties = new HashMap<>();
        QueryResult<Map<String, Object>> result = queryEngine.query(
                queryPrefix() +
                        "MATCH n-[:" + Relationships.HAS_PROPERTY + "]->(property) " +
                        "RETURN property.uri as uri",
                map()
        );
        for (Map<String, Object> uriMap : result) {
            URI uri = URI.create(
                    uriMap.get(
                            "uri"
                    ).toString()
            );
            properties.put(
                    uri,
                    graphElementFactory.withUri(
                            uri
                    )
            );
        }
        return properties;
    }
}
