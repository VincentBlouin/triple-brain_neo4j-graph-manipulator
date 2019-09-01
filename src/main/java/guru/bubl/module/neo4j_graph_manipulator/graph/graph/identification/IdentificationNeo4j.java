/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.graph.identification;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import guru.bubl.module.model.Image;
import guru.bubl.module.model.graph.FriendlyResourcePojo;
import guru.bubl.module.model.graph.GraphElementOperatorFactory;
import guru.bubl.module.model.graph.identification.IdentificationFactory;
import guru.bubl.module.model.graph.identification.IdentificationOperator;
import guru.bubl.module.model.graph.identification.Identifier;
import guru.bubl.module.model.graph.identification.IdentifierPojo;
import guru.bubl.module.neo4j_graph_manipulator.graph.FriendlyResourceFactoryNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.FriendlyResourceNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.OperatorNeo4j;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.graphdb.Node;

import java.net.URI;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import static org.neo4j.driver.v1.Values.parameters;

public class IdentificationNeo4j implements IdentificationOperator, OperatorNeo4j {

    public enum props {
        external_uri,
        identification_type,
        nb_references,
        relation_external_uri
    }

    private FriendlyResourceNeo4j friendlyResourceOperator;
    private Session session;
    private GraphElementOperatorFactory graphElementOperatorFactory;
    private IdentificationFactory identificationFactory;

    @AssistedInject
    protected IdentificationNeo4j(
            FriendlyResourceFactoryNeo4j friendlyResourceFactory,
            Session session,
            GraphElementOperatorFactory graphElementOperatorFactory,
            IdentificationFactory identificationFactory,
            @Assisted Node node
    ) {
        this.friendlyResourceOperator = friendlyResourceFactory.withNode(
                node
        );
        this.session = session;
        this.graphElementOperatorFactory = graphElementOperatorFactory;
        this.identificationFactory = identificationFactory;
    }

    @AssistedInject
    protected IdentificationNeo4j(
            FriendlyResourceFactoryNeo4j friendlyResourceFactory,
            Session session,
            GraphElementOperatorFactory graphElementOperatorFactory,
            IdentificationFactory identificationFactory,
            @Assisted URI uri
    ) {
        this.friendlyResourceOperator = friendlyResourceFactory.withUri(
                uri
        );
        this.session = session;
        this.graphElementOperatorFactory = graphElementOperatorFactory;
        this.identificationFactory = identificationFactory;
    }

    @Override
    public URI getRelationExternalResourceUri() {
        String query = String.format(
                "%sRETURN n.%s as relationExternalUri",
                queryPrefix(),
                props.relation_external_uri
        );
        Record record = session.run(
                query,
                parameters(
                        "uri",
                        uri().toString()
                )
        ).single();
        return URI.create(
                record.get("relationExternalUri").asString()
        );
    }

    @Override
    public URI getExternalResourceUri() {
        Record record = session.run(
                queryPrefix() + "RETURN n.external_uri as externalUri",
                parameters(
                        "uri",
                        this.uri().toString()
                )
        ).single();
        return URI.create(
                record.get("externalUri").asString()
        );
    }

    @Override
    public void setExternalResourceUri(URI uri) {
        session.run(
                queryPrefix() + "SET n.external_uri=$external_uri",
                parameters(
                        "uri", this.uri().toString(),
                        "external_uri", uri.toString()
                )
        );
    }

    @Override
    public Integer getNbReferences() {
        Record record = session.run(
                queryPrefix() + "RETURN n.nb_references as nbReferences",
                parameters(
                        "uri", this.uri().toString()
                )
        ).single();
        return record.get("nbReferences").asInt();
    }

    @Override
    public void setNbReferences(Integer nb) {
        session.run(
                queryPrefix() + "SET n.nb_references=$nbReferences",
                parameters(
                        "uri", uri().toString(),
                        "nbReferences", nb
                )
        );
    }

    @Override
    public IdentifierPojo buildPojo() {
        Record record = session.run(
                queryPrefix() + "RETURN n.uri as uri, n.label as label, n.comment as comment, n.external_uri as externalUri, n.nb_references as nbReferences",
                parameters(
                        "uri", this.uri().toString()
                )
        ).single();
        FriendlyResourcePojo friendlyResourcePojo = new FriendlyResourcePojo(
                URI.create(record.get("uri").asString()),
                record.get("label").asString()
        );
        friendlyResourcePojo.setComment(
                record.get("comment").asString()
        );
        return new IdentifierPojo(
                URI.create(record.get("externalUri").asString()),
                record.get("nbReferences").asInt(),
                friendlyResourcePojo
        );
    }

    @Override
    public void mergeTo(Identifier identifier) {
        StatementResult sr = session.run(
                queryPrefix() + "MATCH (n)<-[:IDENTIFIED_TO]-(tagged) RETURN tagged.uri",
                parameters(
                        "uri", this.uri().toString()
                )
        );
        IdentifierPojo mergeWithPojo = identificationFactory.withUri(
                identifier.uri()
        ).buildPojo();
        while (sr.hasNext()) {
            graphElementOperatorFactory.withUri(
                    URI.create(sr.next().get("tagged.uri").asString())
            ).addMeta(
                    mergeWithPojo
            );
        }
        this.remove();
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
    public Map<String, Object> addCreationProperties(Map<String, Object> map) {
        return friendlyResourceOperator.addCreationProperties(map);
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
