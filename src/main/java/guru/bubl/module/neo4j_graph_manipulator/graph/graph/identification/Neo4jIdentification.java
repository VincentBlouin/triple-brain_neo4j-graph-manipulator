/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.graph.identification;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import guru.bubl.module.common_utils.NamedParameterStatement;
import guru.bubl.module.common_utils.NoEx;
import guru.bubl.module.model.Image;
import guru.bubl.module.model.graph.FriendlyResourcePojo;
import guru.bubl.module.model.graph.GraphElementOperatorFactory;
import guru.bubl.module.model.graph.identification.IdentificationFactory;
import guru.bubl.module.model.graph.identification.IdentificationOperator;
import guru.bubl.module.model.graph.identification.Identifier;
import guru.bubl.module.model.graph.identification.IdentifierPojo;
import guru.bubl.module.neo4j_graph_manipulator.graph.Neo4jFriendlyResource;
import guru.bubl.module.neo4j_graph_manipulator.graph.Neo4jFriendlyResourceFactory;
import guru.bubl.module.neo4j_graph_manipulator.graph.Neo4jOperator;
import guru.bubl.module.neo4j_graph_manipulator.graph.Relationships;
import org.neo4j.graphdb.Node;

import java.net.URI;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.Map;
import java.util.Set;

public class Neo4jIdentification implements IdentificationOperator, Neo4jOperator {

    public enum props {
        external_uri,
        identification_type,
        nb_references,
        relation_external_uri
    }

    private Neo4jFriendlyResource friendlyResourceOperator;
    private Connection connection;
    private GraphElementOperatorFactory graphElementOperatorFactory;
    private IdentificationFactory identificationFactory;

    @AssistedInject
    protected Neo4jIdentification(
            Neo4jFriendlyResourceFactory friendlyResourceFactory,
            Connection connection,
            GraphElementOperatorFactory graphElementOperatorFactory,
            IdentificationFactory identificationFactory,
            @Assisted Node node
    ) {
        this.friendlyResourceOperator = friendlyResourceFactory.withNode(
                node
        );
        this.connection = connection;
        this.graphElementOperatorFactory = graphElementOperatorFactory;
        this.identificationFactory = identificationFactory;
    }

    @AssistedInject
    protected Neo4jIdentification(
            Neo4jFriendlyResourceFactory friendlyResourceFactory,
            Connection connection,
            GraphElementOperatorFactory graphElementOperatorFactory,
            IdentificationFactory identificationFactory,
            @Assisted URI uri
    ) {
        this.friendlyResourceOperator = friendlyResourceFactory.withUri(
                uri
        );
        this.connection = connection;
        this.graphElementOperatorFactory = graphElementOperatorFactory;
        this.identificationFactory = identificationFactory;
    }

    @Override
    public URI getRelationExternalResourceUri() {
        String query = String.format(
                "%s RETURN n.%s as relationExternalUri",
                queryPrefix(),
                props.relation_external_uri
        );
        return NoEx.wrap(() -> {
            ResultSet rs = connection.createStatement().executeQuery(query);
            rs.next();
            return URI.create(
                    rs.getString("relationExternalUri")
            );
        }).get();
    }

    @Override
    public URI getExternalResourceUri() {
        String query = String.format(
                "%s RETURN n.%s as externalUri",
                queryPrefix(),
                props.external_uri
        );
        return NoEx.wrap(() -> {
            ResultSet rs = connection.createStatement().executeQuery(query);
            rs.next();
            return URI.create(
                    rs.getString("externalUri")
            );
        }).get();
    }

    @Override
    public void setExternalResourceUri(URI uri) {
        String query = String.format(
                "%s SET n.%s=@external_uri",
                queryPrefix(),
                props.external_uri
        );
        NoEx.wrap(() -> {
            NamedParameterStatement statement = new NamedParameterStatement(
                    connection,
                    query
            );
            statement.setString(
                    "external_uri",
                    uri.toString()
            );
            return statement.executeQuery();
        }).get();
    }

    @Override
    public Integer getNbReferences() {
        String query = String.format(
                "%s RETURN n.%s as nbReferences",
                queryPrefix(),
                Neo4jIdentification.props.nb_references
        );
        return NoEx.wrap(() -> {
            ResultSet rs = connection.createStatement().executeQuery(query);
            rs.next();
            return new Integer(
                    rs.getString("nbReferences")
            );
        }).get();
    }

    @Override
    public void setNbReferences(Integer nb) {
        String query = String.format(
                "%s SET n.%s=@nbReferences",
                queryPrefix(),
                Neo4jIdentification.props.nb_references
        );
        NoEx.wrap(() -> {
            NamedParameterStatement namedParameterStatement = new NamedParameterStatement(
                    connection,
                    query
            );
            namedParameterStatement.setInt(
                    "nbReferences",
                    nb
            );
            return namedParameterStatement.execute();
        }).get();
    }

    @Override
    public IdentifierPojo buildPojo() {
        String query = queryPrefix() +
                String.format(
                        "RETURN n.%s as uri," +
                                "n.%s as label," +
                                "n.%s as comment," +
                                "n.%s as externalUri," +
                                "n.%s as nbReferences",
                        Neo4jFriendlyResource.props.uri,
                        Neo4jFriendlyResource.props.label,
                        Neo4jFriendlyResource.props.comment,
                        props.external_uri,
                        Neo4jIdentification.props.nb_references
                );
        return NoEx.wrap(() -> {
            ResultSet rs = connection.createStatement().executeQuery(query);
            rs.next();
            FriendlyResourcePojo friendlyResourcePojo = new FriendlyResourcePojo(
                    URI.create(rs.getString("uri")),
                    rs.getString("label")
            );
            friendlyResourcePojo.setComment(
                    rs.getString("comment")
            );
            return new IdentifierPojo(
                    URI.create(rs.getString("externalUri")),
                    new Integer(rs.getString("nbReferences")),
                    friendlyResourcePojo
            );
        }).get();
    }

    @Override
    public void mergeTo(Identifier identifier) {
        String query = queryPrefix() +
                "MATCH n<-[:" +
                Relationships.IDENTIFIED_TO +
                "]-tagged " +
                "RETURN tagged.uri";
        NoEx.wrap(() -> {
            ResultSet rs = connection.createStatement().executeQuery(query);
            IdentifierPojo mergeWithPojo = identificationFactory.withUri(
                    identifier.uri()
            ).buildPojo();
            while (rs.next()) {
                graphElementOperatorFactory.withUri(
                        URI.create(rs.getString("tagged.uri"))
                ).addMeta(
                        mergeWithPojo
                );
            }
            this.remove();
            return rs;
        }).get();

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

    @Override
    public void setNamedCreationProperties(NamedParameterStatement statement) throws SQLException {
        friendlyResourceOperator.setNamedCreationProperties(statement);
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
