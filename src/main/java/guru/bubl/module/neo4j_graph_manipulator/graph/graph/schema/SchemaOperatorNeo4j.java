/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.graph.schema;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import guru.bubl.module.model.Image;
import guru.bubl.module.model.UserUris;
import guru.bubl.module.model.graph.GraphElement;
import guru.bubl.module.model.graph.GraphElementOperator;
import guru.bubl.module.model.graph.GraphElementType;
import guru.bubl.module.model.graph.ShareLevel;
import guru.bubl.module.model.graph.identification.Identifier;
import guru.bubl.module.model.graph.identification.IdentifierPojo;
import guru.bubl.module.model.graph.schema.SchemaOperator;
import guru.bubl.module.neo4j_graph_manipulator.graph.FriendlyResourceNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.OperatorNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.Relationships;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.GraphElementFactoryNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.GraphElementOperatorNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.vertex.VertexInSubGraphOperatorNeo4j;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;

import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static guru.bubl.module.neo4j_graph_manipulator.graph.RestApiUtilsNeo4j.map;
import static org.neo4j.driver.v1.Values.parameters;

public class SchemaOperatorNeo4j implements SchemaOperator, OperatorNeo4j {

    protected GraphElementOperatorNeo4j graphElementOperator;
    protected GraphElementFactoryNeo4j graphElementFactory;
    protected Session session;

    @AssistedInject
    protected SchemaOperatorNeo4j(
            GraphElementFactoryNeo4j graphElementFactory,
            Session session,
            @Assisted URI uri
    ) {
        this.session = session;
        this.graphElementFactory = graphElementFactory;
        graphElementOperator = graphElementFactory.withUri(uri);
    }

    @AssistedInject
    protected SchemaOperatorNeo4j(
            GraphElementFactoryNeo4j graphElementFactory,
            Session session,
            @Assisted String ownerUserName
    ) {
        this(
                graphElementFactory,
                session,
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
    public Map<String, Object> addCreationProperties(Map<String, Object> map) {
        Map<String, Object> newMap = map(
                FriendlyResourceNeo4j.props.type.name(),
                GraphElementType.Schema.name(),
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

        session.run(
                "CREATE (n:Resource $props)",
                parameters(
                        "props",
                        props
                )
        );
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
                "CREATE (p:Resource $values) " +
                "CREATE UNIQUE " +
                "(n)-[:" + Relationships.HAS_PROPERTY + "]->(p) ";

        session.run(
                query,
                parameters(
                        "uri", this.uri().toString(),
                        "values", property.addCreationProperties(map(
                                FriendlyResourceNeo4j.props.type.name(), GraphElementType.Property.name(),
                                VertexInSubGraphOperatorNeo4j.props.shareLevel.name(), ShareLevel.PUBLIC.getConfidentialityIndex()
                        ))
                )
        );
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
        String query = queryPrefix() + "MATCH (n)-[:HAS_PROPERTY]->(property) RETURN property.uri as uri";
        StatementResult sr = session.run(
                query,
                parameters(
                        "uri", this.uri().toString()
                )
        );
        while (sr.hasNext()) {
            Record record = sr.next();
            URI uri = URI.create(
                    record.get(
                            "uri"
                    ).asString()
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

    @Override
    public Map<URI, IdentifierPojo> getIdentifications() {
        return graphElementOperator.getIdentifications();
    }

}
