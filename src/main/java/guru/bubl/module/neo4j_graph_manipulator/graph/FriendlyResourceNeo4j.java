/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import guru.bubl.module.common_utils.NamedParameterStatement;
import guru.bubl.module.common_utils.NoEx;
import guru.bubl.module.common_utils.Uris;
import guru.bubl.module.model.FriendlyResource;
import guru.bubl.module.model.Image;
import guru.bubl.module.model.UserUris;
import guru.bubl.module.model.graph.FriendlyResourceOperator;
import guru.bubl.module.model.graph.FriendlyResourcePojo;
import guru.bubl.module.model.graph.GraphElementType;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.UserGraphNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.image.ImageFactoryNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.image.ImagesNeo4j;
import org.apache.commons.lang.StringUtils;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.net.URI;
import java.sql.*;
import java.util.Date;
import java.util.Map;
import java.util.Set;

public class FriendlyResourceNeo4j implements FriendlyResourceOperator, OperatorNeo4j {

    public enum props {
        uri,
        label,
        comment,
        creation_date,
        last_modification_date,
        owner,
        type
    }

    public static final String LAST_MODIFICATION_QUERY_PART = String.format(
            " n.%s=@%s ",
            props.last_modification_date,
            props.last_modification_date
    );

    public static Map<String, Object> addUpdatedLastModificationDate(Map<String, Object> map) {
        map.put(
                props.last_modification_date.name(), new Date().getTime()
        );
        return map;
    }

    protected URI uri;

    protected Node node;

    protected ImagesNeo4j images;

    protected Connection connection;

    @AssistedInject
    protected FriendlyResourceNeo4j(
            ImageFactoryNeo4j imageFactory,
            Connection connection,
            @Assisted Node node
    ) {
        this.images = imageFactory.forResource(this);
        this.connection = connection;
        this.node = node;
        this.uri = Uris.get(node.getProperty(
                UserGraphNeo4j.URI_PROPERTY_NAME
        ).toString());
    }

    @AssistedInject
    protected FriendlyResourceNeo4j(
            ImageFactoryNeo4j imageFactory,
            Connection connection,
            @Assisted URI uri
    ) {
        this.images = imageFactory.forResource(this);
        this.connection = connection;
        if (StringUtils.isEmpty(uri.toString())) {
            throw new RuntimeException("uri for friendly resource is mandatory");
        }
        this.uri = uri;
    }

    @AssistedInject
    protected FriendlyResourceNeo4j(
            ImageFactoryNeo4j imageFactory,
            Connection connection,
            @Assisted FriendlyResourcePojo pojo
    ) {

        this.images = imageFactory.forResource(this);
        this.connection = connection;
        this.uri = pojo.uri();
        createUsingInitialValues(
                RestApiUtilsNeo4j.map(
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
        return NoEx.wrap(() -> {
            String query = String.format(
                    "%s return n.%s as label",
                    queryPrefix(),
                    props.label.toString()
            );
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery(query);
            return rs.next() && rs.getString("label") != null ?
                    rs.getString("label") :
                    "";
        }).get();
    }

    @Override
    public void label(String label) {
        String query = String.format(
                "%s SET n.%s=@label, %s",
                queryPrefix(),
                FriendlyResourceNeo4j.props.label,
                LAST_MODIFICATION_QUERY_PART
        );
        Map<String, Object> props = RestApiUtilsNeo4j.map(
                "label", label
        );
        addUpdatedLastModificationDate(props);
        NoEx.wrap(() -> {
            NamedParameterStatement statement = new NamedParameterStatement(connection, query);
            statement.setString("label", label);
            setLastUpdatedDateInStatement(statement);
            return statement.execute();
        }).get();
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
        String query = String.format(
                "%sreturn n.%s as comment",
                queryPrefix(),
                props.comment
        );
        return NoEx.wrap(() -> {
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery(query);
            return rs.next() && rs.getString("comment") != null ?
                    rs.getString("comment") :
                    "";
        }).get();
    }

    @Override
    public void comment(String comment) {
        String query = String.format(
                "%s SET n.%s=@comment, %s",
                queryPrefix(),
                props.comment,
                LAST_MODIFICATION_QUERY_PART
        );
        Map<String, Object> props = RestApiUtilsNeo4j.map(
                "comment", comment
        );
        addUpdatedLastModificationDate(props);
        NoEx.wrap(() -> {
            NamedParameterStatement statement = new NamedParameterStatement(connection, query);
            statement.setString("comment", comment);
            setLastUpdatedDateInStatement(statement);
            return statement.execute();
        }).get();
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
                RestApiUtilsNeo4j.map()
        );
    }

    @Override
    public void createUsingInitialValues(Map<String, Object> values) {
        Map<String, Object> creationProps = addCreationProperties(
                values
        );
        String query = String.format(
                "create (n:%s {1})",
                GraphElementType.resource
        );
        NoEx.wrap(() -> {
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setObject(
                    1,
                    creationProps
            );
            return statement.execute();
        }).get();
    }

    public FriendlyResourcePojo pojoFromCreationProperties(Map<String, Object> creationProperties){
        FriendlyResourcePojo friendlyResourcePojo = new FriendlyResourcePojo(
                uri
        );
        friendlyResourcePojo.setCreationDate(
                new Long(
                        creationProperties.get(
                                props.creation_date.name()
                        ).toString()
                )
        );
        friendlyResourcePojo.setLastModificationDate(
                new Long(
                        creationProperties.get(
                                props.last_modification_date.name()
                        ).toString()
                )
        );
        return friendlyResourcePojo;
    }

    @Override
    public void remove() {
        for (Relationship relationship : getNode().getRelationships()) {
            //removing explicitly so node index gets reindexed
            relationship.removeProperty(
                    UserGraphNeo4j.URI_PROPERTY_NAME
            );
            relationship.delete();
        }
        //removing explicitly so node index gets reindexed
        getNode().removeProperty(UserGraphNeo4j.URI_PROPERTY_NAME);
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
        NoEx.wrap(() -> {
            NamedParameterStatement statement = new NamedParameterStatement(
                    connection,
                    query
            );
            setLastUpdatedDateInStatement(statement);
            return statement.execute();
        });
    }

    @Override
    public Node getNode() {
        if (null == node) {
            node = NoEx.wrap(() -> {
                ResultSet rs = connection.createStatement().executeQuery(
                        queryPrefix() + "return n"
                );
                rs.next();
                return (Node) rs.getObject("n");
            }).get();
        }
        return node;
    }

    @Override
    public void setNamedCreationProperties(NamedParameterStatement statement) throws SQLException {
        statement.setString(
                UserGraphNeo4j.URI_PROPERTY_NAME,
                uri().toString()
        );
        statement.setString(
                props.owner.name(),
                UserUris.ownerUserNameFromUri(uri())
        );
        /*
        *  not setting creation date and last modification date because it
        *  can be easily done in neo4j using timestamp()
        */
    }

    @Override
    public Map<String, Object> addCreationProperties(Map<String, Object> map) {
        Long now = new Date().getTime();
        Map<String, Object> newMap = RestApiUtilsNeo4j.map(
                UserGraphNeo4j.URI_PROPERTY_NAME, uri().toString(),
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
        return String.format(
                "START %s ",
                addToSelectUsingVariableName(
                        "n"
                )
        );
    }

    public void setLastUpdatedDateInStatement(NamedParameterStatement statement) throws SQLException {
        statement.setLong(
                props.last_modification_date.name(),
                new Date().getTime()
        );
    }

    public String addToSelectUsingVariableName(String variableName) {
        return String.format(
                "%s=node:node_auto_index('uri:%s') ",
                variableName,
                uri
        );
    }
}
