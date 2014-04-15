package org.triple_brain.module.neo4j_graph_manipulator.graph;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.hp.hpl.jena.vocabulary.RDFS;
import org.apache.commons.lang.StringUtils;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.rest.graphdb.RestAPI;
import org.neo4j.rest.graphdb.batch.BatchCallback;
import org.neo4j.rest.graphdb.query.QueryEngine;
import org.neo4j.rest.graphdb.util.QueryResult;
import org.triple_brain.module.common_utils.Uris;
import org.triple_brain.module.model.FriendlyResource;
import org.triple_brain.module.model.Image;
import org.triple_brain.module.model.graph.FriendlyResourceOperator;
import org.triple_brain.module.model.graph.FriendlyResourcePojo;
import org.triple_brain.module.neo4j_graph_manipulator.graph.graph.Neo4jUserGraph;
import org.triple_brain.module.neo4j_graph_manipulator.graph.image.Neo4jImageFactory;
import org.triple_brain.module.neo4j_graph_manipulator.graph.image.Neo4jImages;
import org.triple_brain.module.neo4j_graph_manipulator.graph.suggestion.Neo4jSuggestionOriginOperator;

import javax.inject.Inject;
import java.net.URI;
import java.util.*;

import static org.triple_brain.module.neo4j_graph_manipulator.graph.Neo4jRestApiUtils.map;
import static org.triple_brain.module.neo4j_graph_manipulator.graph.Neo4jRestApiUtils.wrap;

/*
* Copyright Mozilla Public License 1.1
*/
public class Neo4jFriendlyResource implements FriendlyResourceOperator, Neo4jOperator {

    public enum props {
        label,
        creation_date,
        last_modification_date
    }

    public static final String LAST_MODIFICATION_QUERY_PART = " n." + props.last_modification_date + "= { " + props.last_modification_date + "} ";

    public static Map<String, Object> addUpdatedLastModificationDate(Map<String, Object> map) {
        map.put(
                props.last_modification_date.name(), new Date().getTime()
        );
        return map;
    }

    QueryEngine<Map<String, Object>> queryEngine;

    protected URI uri;

    protected Node node;

    protected RestAPI restApi;

    protected Neo4jImages images;

    @AssistedInject
    protected Neo4jFriendlyResource(
            RestAPI restApi,
            QueryEngine queryEngine,
            Neo4jImageFactory imageFactory,
            @Assisted Node node
    ) {
        this.queryEngine = queryEngine;
        this.restApi = restApi;
        this.images = imageFactory.forResource(this);
        this.node = node;
        this.uri = Uris.get(node.getProperty(
                Neo4jUserGraph.URI_PROPERTY_NAME
        ).toString());
    }

    @AssistedInject
    protected Neo4jFriendlyResource(
            RestAPI restApi,
            QueryEngine queryEngine,
            Neo4jImageFactory imageFactory,
            @Assisted URI uri
    ) {
        this.queryEngine = queryEngine;
        this.restApi = restApi;
        this.images = imageFactory.forResource(this);
        if (StringUtils.isEmpty(uri.toString())) {
            throw new RuntimeException("uri for friendly resource is mandatory");
        }
        this.uri = uri;
    }

    @AssistedInject
    protected Neo4jFriendlyResource(
            RestAPI restApi,
            QueryEngine queryEngine,
            Neo4jUtils neo4jUtils,
            Neo4jImageFactory imageFactory,
            @Assisted URI uri,
            @Assisted String label
    ) {
        this(
                restApi,
                queryEngine,
                imageFactory,
                neo4jUtils.getOrCreate(uri)
        );
        label(label);
    }

    @AssistedInject
    protected Neo4jFriendlyResource(
            RestAPI restApi,
            QueryEngine queryEngine,
            Neo4jImageFactory imageFactory,
            @Assisted FriendlyResourcePojo pojo
    ) {

        this.restApi = restApi;
        this.queryEngine = queryEngine;
        this.images = imageFactory.forResource(this);
        this.uri = pojo.uri();
        createUsingInitialValues(
                map(
                        RDFS.label.getURI().toString(), pojo.label() == null ? "" : pojo.label(),
                        RDFS.comment.getURI().toString(), pojo.comment() == null ? "" : pojo.comment()
                )
        );
    }

    @Override
    public URI uri() {
        return uri;
    }

    @Override
    public boolean hasLabel() {
        return !label().isEmpty();
    }

    @Override
    public String label() {
        QueryResult<Map<String, Object>> result = queryEngine.query(
                queryPrefix() +
                        "return n.`" + RDFS.label.getURI() + "` as label",
                map()
        );
        Object label = result.iterator().next().get("label");
        return label == null ?
                "" :
                label.toString();
    }

    @Override
    public void label(String label) {
        String query = queryPrefix() +
                " SET n.`" + RDFS.label.getURI() + "`= {label}, " +
                LAST_MODIFICATION_QUERY_PART;
        Map<String, Object> props = map(
                "label", label
        );
        addUpdatedLastModificationDate(props);
        queryEngine.query(
                query,
                props
        );
    }

    @Override
    public Set<Image> images() {
        return images.get();
    }

    @Override
    public Boolean gotImages() {
        return images().size() > 0;
    }

    @Override
    public String comment() {
        String query = queryPrefix() + "return n.`"
                + RDFS.comment.getURI() + "` as comment";
        QueryResult<Map<String, Object>> result = queryEngine.query(
                query,
                map()
        );
        Iterator<Map<String, Object>> it = result.iterator();
        Object comment = it.next().get("comment");
        return null == comment ?
                "" :
                comment.toString();
    }

    @Override
    public void comment(String comment) {
        String query = queryPrefix() + "SET n.`"
                + RDFS.comment.getURI() +
                "`= {comment}, " + LAST_MODIFICATION_QUERY_PART;
        queryEngine.query(
                query,
                addUpdatedLastModificationDate(map(
                        "comment", comment
                ))
        );
    }

    @Override
    public Boolean gotComments() {
        return !StringUtils.isEmpty(
                comment()
        );
    }

    @Override
    public void addImages(Set<Image> images) {
        this.images.addAll(images);
    }

    @Override
    public void create() {
        createUsingInitialValues(
                map()
        );
    }

    @Override
    public void createUsingInitialValues(Map<String, Object> values) {
        Map<String, Object> props = addCreationProperties(
                values
        );
        queryEngine.query(
                "create (n {props})", wrap(props)
        );
    }

    @Override
    public void remove() {
        for (Relationship relationship : getNode().getRelationships()) {
            //removing explicitly so node index gets reindexed
            relationship.removeProperty(
                    Neo4jUserGraph.URI_PROPERTY_NAME
            );
            relationship.delete();
        }
        //removing explicitly so node index gets reindexed
        getNode().removeProperty(Neo4jUserGraph.URI_PROPERTY_NAME);
        getNode().delete();
    }

    @Override
    public Date creationDate() {
        return new Date((Long) getNode().getProperty(
                props.creation_date.name()
        ));
    }

    @Override
    public Date lastModificationDate() {
        return new Date((Long) getNode().getProperty(
                props.last_modification_date.name()
        ));
    }

    public void updateLastModificationDate() {
        String query = queryPrefix() +
                " SET " +
                LAST_MODIFICATION_QUERY_PART;
        queryEngine.query(
                query,
                addUpdatedLastModificationDate(map())
        );
    }

    @Override
    public Node getNode() {
        if (null == node) {
            QueryResult<Map<String, Object>> result = queryEngine.query(
                    queryPrefix() + "return n",
                    map()
            );
            node = (Node) result.iterator().next().get("n");
        }
        return node;
    }

    @Override
    public Map<String, Object> addCreationProperties(Map<String, Object> map) {
        Long now = new Date().getTime();
        Map<String, Object> newMap = map(
                Neo4jUserGraph.URI_PROPERTY_NAME, uri().toString(),
                props.creation_date.name(), now,
                props.last_modification_date.name(), now
        );
        newMap.putAll(
                map
        );
        return newMap;
    }

    @Override
    public boolean equals(Object friendlyResourceToCompareAsObject) {
        FriendlyResource friendlyResourceToCompare = (FriendlyResource) friendlyResourceToCompareAsObject;
        return uri().equals(friendlyResourceToCompare.uri());
    }

    @Override
    public int hashCode() {
        return uri().hashCode();
    }

    private boolean hasCreationDate() {
        return node.hasProperty(
                props.creation_date.name()
        );
    }

    @Override
    public String queryPrefix() {
        return "START " + addToSelectUsingVariableName(
                "n"
        ) + " ";
    }

    public String addToSelectUsingVariableName(String variableName) {
        return variableName + "=node:node_auto_index(uri='" + uri + "') ";
    }

}
