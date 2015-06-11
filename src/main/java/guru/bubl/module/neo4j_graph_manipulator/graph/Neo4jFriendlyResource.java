/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.Neo4jUserGraph;
import guru.bubl.module.neo4j_graph_manipulator.graph.image.Neo4jImageFactory;
import guru.bubl.module.neo4j_graph_manipulator.graph.image.Neo4jImages;
import org.apache.commons.lang.StringUtils;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.rest.graphdb.RestAPI;
import org.neo4j.rest.graphdb.query.QueryEngine;
import org.neo4j.rest.graphdb.util.QueryResult;
import guru.bubl.module.common_utils.Uris;
import guru.bubl.module.model.FriendlyResource;
import guru.bubl.module.model.Image;
import guru.bubl.module.model.UserUris;
import guru.bubl.module.model.graph.FriendlyResourceOperator;
import guru.bubl.module.model.graph.FriendlyResourcePojo;
import guru.bubl.module.model.graph.GraphElementType;

import java.net.URI;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import static guru.bubl.module.neo4j_graph_manipulator.graph.Neo4jRestApiUtils.map;

public class Neo4jFriendlyResource implements FriendlyResourceOperator, Neo4jOperator {

    public enum props {
        uri,
        label,
        comment,
        creation_date,
        last_modification_date,
        owner,
        type
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
            Neo4jImageFactory imageFactory,
            @Assisted FriendlyResourcePojo pojo
    ) {

        this.restApi = restApi;
        this.queryEngine = queryEngine;
        this.images = imageFactory.forResource(this);
        this.uri = pojo.uri();
        createUsingInitialValues(
                Neo4jRestApiUtils.map(
                        props.label.toString(), pojo.label() == null ? "" : pojo.label(),
                        props.comment.toString(), pojo.comment() == null ? "" : pojo.comment()
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
                        "return n." + props.label.toString() + " as label",
                Neo4jRestApiUtils.map()
        );
        Object label = result.iterator().next().get("label");
        return label == null ?
                "" :
                label.toString();
    }

    @Override
    public void label(String label) {
        String query = queryPrefix() +
                " SET n." + props.label + "= {label}, " +
                LAST_MODIFICATION_QUERY_PART;
        Map<String, Object> props = Neo4jRestApiUtils.map(
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
        String query = queryPrefix() + "return n."
                + props.comment + " as comment";
        QueryResult<Map<String, Object>> result = queryEngine.query(
                query,
                Neo4jRestApiUtils.map()
        );
        Iterator<Map<String, Object>> it = result.iterator();
        Object comment = it.next().get("comment");
        return null == comment ?
                "" :
                comment.toString();
    }

    @Override
    public void comment(String comment) {
        String query = queryPrefix() + "SET n."
                + props.comment +
                "= {comment}, " + LAST_MODIFICATION_QUERY_PART;
        queryEngine.query(
                query,
                addUpdatedLastModificationDate(Neo4jRestApiUtils.map(
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
                Neo4jRestApiUtils.map()
        );
    }

    @Override
    public void createUsingInitialValues(Map<String, Object> values) {
        Map<String, Object> props = addCreationProperties(
                values
        );
        queryEngine.query(
                "create (n:"+GraphElementType.resource+" {props})", Neo4jRestApiUtils.wrap(props)
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

    @Override
    public String getOwnerUsername() {
        return UserUris.ownerUserNameFromUri(uri);
    }

    public void updateLastModificationDate() {
        String query = queryPrefix() +
                " SET " +
                LAST_MODIFICATION_QUERY_PART;
        queryEngine.query(
                query,
                addUpdatedLastModificationDate(Neo4jRestApiUtils.map())
        );
    }

    @Override
    public Node getNode() {
        if (null == node) {
            QueryResult<Map<String, Object>> result = queryEngine.query(
                    queryPrefix() + "return n",
                    Neo4jRestApiUtils.map()
            );
            node = (Node) result.iterator().next().get("n");
        }
        return node;
    }

    @Override
    public Map<String, Object> addCreationProperties(Map<String, Object> map) {
        Long now = new Date().getTime();
        Map<String, Object> newMap = Neo4jRestApiUtils.map(
                Neo4jUserGraph.URI_PROPERTY_NAME, uri().toString(),
                props.owner.name(), UserUris.ownerUserNameFromUri(uri()),
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

    @Override
    public String queryPrefix() {
        return "START " + addToSelectUsingVariableName(
                "n"
        ) + " ";
    }

    public String addToSelectUsingVariableName(String variableName) {
        return variableName + "=node:node_auto_index('uri:" + uri + "') ";
    }

}
