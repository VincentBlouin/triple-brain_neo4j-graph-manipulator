/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.graph.schema;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import guru.bubl.module.common_utils.NamedParameterStatement;
import guru.bubl.module.common_utils.NoExRun;
import guru.bubl.module.model.Image;
import guru.bubl.module.model.UserUris;
import guru.bubl.module.model.graph.*;
import guru.bubl.module.model.graph.schema.SchemaOperator;
import guru.bubl.module.neo4j_graph_manipulator.graph.Neo4jFriendlyResource;
import guru.bubl.module.neo4j_graph_manipulator.graph.Neo4jOperator;
import guru.bubl.module.neo4j_graph_manipulator.graph.Relationships;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.Neo4jGraphElementFactory;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.Neo4jGraphElementOperator;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.vertex.Neo4jVertexInSubGraphOperator;
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

import static guru.bubl.module.neo4j_graph_manipulator.graph.Neo4jRestApiUtils.map;

public class Neo4jSchemaOperator implements SchemaOperator, Neo4jOperator {

    protected Neo4jGraphElementOperator graphElementOperator;
    protected Neo4jGraphElementFactory graphElementFactory;
    protected Connection connection;

    @AssistedInject
    protected Neo4jSchemaOperator(
            Neo4jGraphElementFactory graphElementFactory,
            Connection connection,
            @Assisted URI uri
    ) {
        this.connection = connection;
        this.graphElementFactory = graphElementFactory;
        graphElementOperator = graphElementFactory.withUri(uri);
    }

    @AssistedInject
    protected Neo4jSchemaOperator(
            Neo4jGraphElementFactory graphElementFactory,
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
    public String getOwnerUsername() {
        return graphElementOperator.getOwnerUsername();
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
                Neo4jFriendlyResource.props.type.name(),
                GraphElementType.schema.name(),
                Neo4jVertexInSubGraphOperator.props.is_public.name(),
                true
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
                Neo4jFriendlyResource.props.type.name(),
                GraphElementType.schema.name()
        );
        statement.setObject(
                Neo4jVertexInSubGraphOperator.props.is_public.name(),
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
        NoExRun.wrap(() -> {
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
    public void removeIdentification(Identification type) {
        graphElementOperator.removeIdentification(type);
    }

    @Override
    public IdentificationPojo addType(Identification type) {
        return graphElementOperator.addType(type);
    }

    @Override
    public IdentificationPojo addSameAs(Identification friendlyResource) {
        return graphElementOperator.addSameAs(friendlyResource);
    }

    @Override
    public IdentificationPojo addGenericIdentification(Identification friendlyResource) {
        return graphElementOperator.addGenericIdentification(friendlyResource);
    }

    @Override
    public GraphElementOperator addProperty() {
        URI createdUri = UserUris.generateSchemaPropertyUri(uri());
        Neo4jGraphElementOperator property = graphElementFactory.withUri(createdUri);
        String query = queryPrefix() +
                "CREATE (p:" + GraphElementType.resource + " {1}) " +
                "CREATE UNIQUE " +
                "n-[:" + Relationships.HAS_PROPERTY + "]->p ";
        NoExRun.wrap(() -> {
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setObject(
                    1,
                    property.addCreationProperties(map(
                            Neo4jFriendlyResource.props.type.name(), GraphElementType.property.name(),
                            Neo4jVertexInSubGraphOperator.props.is_public.name(), true
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
        return NoExRun.wrap(() -> {
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
    public Map<URI, IdentificationPojo> getGenericIdentifications() {
        return graphElementOperator.getGenericIdentifications();
    }

    @Override
    public Map<URI, IdentificationPojo> getSameAs() {
        return graphElementOperator.getSameAs();
    }

    @Override
    public Map<URI, IdentificationPojo> getAdditionalTypes() {
        return graphElementOperator.getAdditionalTypes();
    }

    @Override
    public Map<URI, IdentificationPojo> getIdentifications() {
        return graphElementOperator.getIdentifications();
    }

    @Override
    public URI getExternalResourceUri() {
        return graphElementOperator.getExternalResourceUri();
    }
}
