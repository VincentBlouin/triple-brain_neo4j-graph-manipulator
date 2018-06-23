/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.graph.schema;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import guru.bubl.module.common_utils.NamedParameterStatement;
import guru.bubl.module.common_utils.NoEx;
import guru.bubl.module.model.Image;
import guru.bubl.module.model.UserUris;
import guru.bubl.module.model.graph.*;
import guru.bubl.module.model.graph.identification.Identifier;
import guru.bubl.module.model.graph.identification.IdentifierPojo;
import guru.bubl.module.model.graph.schema.SchemaOperator;
import guru.bubl.module.neo4j_graph_manipulator.graph.FriendlyResourceNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.OperatorNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.Relationships;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.GraphElementFactoryNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.GraphElementOperatorNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.vertex.VertexInSubGraphOperatorNeo4j;
import org.neo4j.graphdb.Node;

import java.net.URI;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static guru.bubl.module.neo4j_graph_manipulator.graph.RestApiUtilsNeo4j.map;

public class SchemaOperatorNeo4j implements SchemaOperator, OperatorNeo4j {

    protected GraphElementOperatorNeo4j graphElementOperator;
    protected GraphElementFactoryNeo4j graphElementFactory;
    protected Connection connection;

    @AssistedInject
    protected SchemaOperatorNeo4j(
            GraphElementFactoryNeo4j graphElementFactory,
            Connection connection,
            @Assisted URI uri
    ) {
        this.connection = connection;
        this.graphElementFactory = graphElementFactory;
        graphElementOperator = graphElementFactory.withUri(uri);
    }

    @AssistedInject
    protected SchemaOperatorNeo4j(
            GraphElementFactoryNeo4j graphElementFactory,
            Connection connection,
            @Assisted String ownerUserName
    ) {
        this(
                graphElementFactory,
                connection,
                new UserUris(ownerUserName).generateSchemaUri()
        );
        create();
    }

    @Override
    public URI uri() {
        return graphElementOperator.uri();
    }

    @Override
    public boolean hasLabel() {
        return graphElementOperator.hasLabel();
    }

    @Override
    public String label() {
        return graphElementOperator.label();
    }

    @Override
    public Set<Image> images() {
        return graphElementOperator.images();
    }

    @Override
    public Boolean gotImages() {
        return graphElementOperator.gotImages();
    }

    @Override
    public String comment() {
        return graphElementOperator.comment();
    }

    @Override
    public Boolean gotComments() {
        return graphElementOperator.gotComments();
    }

    @Override
    public Date creationDate() {
        return graphElementOperator.creationDate();
    }

    @Override
    public Date lastModificationDate() {
        return graphElementOperator.lastModificationDate();
    }

    @Override
    public String queryPrefix() {
        return graphElementOperator.queryPrefix();
    }

    @Override
    public Node getNode() {
        return graphElementOperator.getNode();
    }

    @Override
    public Map<String, Object> addCreationProperties(Map<String, Object> map) {
        Map<String, Object> newMap = map(
                FriendlyResourceNeo4j.props.type.name(),
                GraphElementType.schema.name(),
                VertexInSubGraphOperatorNeo4j.props.shareLevel.name(),
                ShareLevel.PUBLIC.getConfidentialityIndex()
        );
        newMap.putAll(
                map
        );
        newMap = graphElementOperator.addCreationProperties(
                newMap
        );
        return newMap;
    }

    @Override
    public void setNamedCreationProperties(NamedParameterStatement statement) throws SQLException {
        statement.setString(
                FriendlyResourceNeo4j.props.type.name(),
                GraphElementType.schema.name()
        );
        statement.setObject(
                VertexInSubGraphOperatorNeo4j.props.is_public.name(),
                true
        );
        graphElementOperator.setNamedCreationProperties(
                statement
        );
    }

    @Override
    public void comment(String comment) {
        graphElementOperator.comment(comment);
    }

    @Override
    public void label(String label) {
        graphElementOperator.label(label);
    }

    @Override
    public void addImages(Set<Image> images) {
        graphElementOperator.addImages(images);
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
        String query = String.format(
                "create (n:%s{1})",
                GraphElementType.resource
        );
        NoEx.wrap(() -> {
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setObject(
                    1,
                    props
            );
            return statement.execute();
        }).get();
    }

    @Override
    public void remove() {
        //cannot remove schema for now
    }

    @Override
    public void removeIdentification(Identifier type) {
        graphElementOperator.removeIdentification(type);
    }

    @Override
    public Map<URI, IdentifierPojo> addMeta(Identifier friendlyResource) {
        return graphElementOperator.addMeta(friendlyResource);
    }

    @Override
    public void setSortDate(Date sortDate, Date moveDate) {
        graphElementOperator.setSortDate(
                sortDate,
                moveDate
        );
    }

    @Override
    public String getColors() {
        return graphElementOperator.getColors();
    }

    @Override
    public String getFont() {
        return graphElementOperator.getFont();
    }

    @Override
    public void setColors(String colors) {
        graphElementOperator.setColors(colors);
    }

    @Override
    public void setFont(String font) {
        graphElementOperator.setFont(font);
    }

    @Override
    public void setChildrenIndex(String childrenIndex) {
        graphElementOperator.setChildrenIndex(
                childrenIndex
        );
    }

    @Override
    public String getChildrenIndex() {
        return graphElementOperator.getChildrenIndex();
    }

    @Override
    public GraphElementOperator addProperty() {
        URI createdUri = UserUris.generateSchemaPropertyUri(uri());
        GraphElementOperatorNeo4j property = graphElementFactory.withUri(createdUri);
        String query = queryPrefix() +
                "CREATE (p:" + GraphElementType.resource + " {1}) " +
                "CREATE UNIQUE " +
                "n-[:" + Relationships.HAS_PROPERTY + "]->p ";
        NoEx.wrap(() -> {
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setObject(
                    1,
                    property.addCreationProperties(map(
                            FriendlyResourceNeo4j.props.type.name(), GraphElementType.property.name(),
                            VertexInSubGraphOperatorNeo4j.props.shareLevel.name(), ShareLevel.PUBLIC.getConfidentialityIndex()
                    ))
            );
            return statement.execute();
        }).get();
        return property;
    }

    @Override
    public boolean equals(Object graphElementToCompareAsObject) {
        return graphElementOperator.equals(graphElementToCompareAsObject);
    }

    @Override
    public int hashCode() {
        return graphElementOperator.hashCode();
    }

    @Override
    public Map<URI, ? extends GraphElement> getProperties() {
        Map<URI, GraphElementOperator> properties = new HashMap<>();
        String query = queryPrefix() +
                "MATCH n-[:" + Relationships.HAS_PROPERTY + "]->(property) " +
                "RETURN property.uri as uri";
        return NoEx.wrap(() -> {
            ResultSet rs = connection.createStatement().executeQuery(
                    query
            );
            while (rs.next()) {
                URI uri = URI.create(
                        rs.getString(
                                "uri"
                        )
                );
                properties.put(
                        uri,
                        graphElementFactory.withUri(
                                uri
                        )
                );
            }
            return properties;
        }).get();
    }

    @Override
    public Map<URI, IdentifierPojo> getIdentifications() {
        return graphElementOperator.getIdentifications();
    }

}
