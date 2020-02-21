/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.graph.tag;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import guru.bubl.module.model.Image;
import guru.bubl.module.model.graph.FriendlyResourcePojo;
import guru.bubl.module.model.graph.GraphElementPojo;
import guru.bubl.module.model.graph.ShareLevel;
import guru.bubl.module.model.graph.tag.TagFactory;
import guru.bubl.module.model.graph.tag.TagOperator;
import guru.bubl.module.model.graph.tag.Tag;
import guru.bubl.module.model.graph.tag.TagPojo;
import guru.bubl.module.neo4j_graph_manipulator.graph.FriendlyResourceFactoryNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.OperatorNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.GraphElementFactoryNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.GraphElementOperatorNeo4j;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;

import java.net.URI;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import static org.neo4j.driver.v1.Values.parameters;

public class TagNeo4J implements TagOperator, OperatorNeo4j {

    @Override
    public Map<URI, TagPojo> getIdentifications() {
        return null;
    }

    @Override
    public String getColors() {
        return null;
    }

    @Override
    public String getFont() {
        return null;
    }

    @Override
    public String getChildrenIndex() {
        return null;
    }

    @Override
    public URI getPatternUri() {
        return null;
    }

    @Override
    public Boolean isPublic() {
        return this.getShareLevel().isPublic();
    }

    @Override
    public ShareLevel getShareLevel() {
        return graphElementOperator.getShareLevel();
    }

    public enum props {
        external_uri,
        identification_type,
        nb_references,
        relation_external_uri
    }

    private GraphElementOperatorNeo4j graphElementOperator;
    private Driver driver;
    private GraphElementFactoryNeo4j graphElementOperatorFactory;
    private TagFactory tagFactory;

    @AssistedInject
    protected TagNeo4J(
            FriendlyResourceFactoryNeo4j friendlyResourceFactory,
            Driver driver,
            GraphElementFactoryNeo4j graphElementOperatorFactory,
            TagFactory tagFactory,
            @Assisted URI uri
    ) {
        this.graphElementOperator = graphElementOperatorFactory.withUri(
                uri
        );
        this.driver = driver;
        this.graphElementOperatorFactory = graphElementOperatorFactory;
        this.tagFactory = tagFactory;
    }

    @Override
    public URI getRelationExternalResourceUri() {
        String query = String.format(
                "%sRETURN n.%s as relationExternalUri",
                queryPrefix(),
                props.relation_external_uri
        );
        try (Session session = driver.session()) {
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
    }

    @Override
    public URI getExternalResourceUri() {
        try (Session session = driver.session()) {
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
    }

    @Override
    public void setExternalResourceUri(URI uri) {
        try (Session session = driver.session()) {
            session.run(
                    queryPrefix() + "SET n.external_uri=$external_uri",
                    parameters(
                            "uri", this.uri().toString(),
                            "external_uri", uri.toString()
                    )
            );
        }
    }

    @Override
    public Integer getNbReferences() {
        try (Session session = driver.session()) {
            Record record = session.run(
                    queryPrefix() + "RETURN n.nb_references as nbReferences",
                    parameters(
                            "uri", this.uri().toString()
                    )
            ).single();
            return record.get("nbReferences").asInt();
        }
    }

    @Override
    public void setNbReferences(Integer nb) {
        try (Session session = driver.session()) {
            session.run(
                    queryPrefix() + "SET n.nb_references=$nbReferences",
                    parameters(
                            "uri", uri().toString(),
                            "nbReferences", nb
                    )
            );
        }
    }

    @Override
    public TagPojo buildPojo() {
        try (Session session = driver.session()) {
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
            return new TagPojo(
                    URI.create(record.get("externalUri").asString()),
                    record.get("nbReferences").asInt(),
                    new GraphElementPojo(
                            friendlyResourcePojo
                    )
            );
        }
    }

    @Override
    public void mergeTo(Tag tag) {
        try (Session session = driver.session()) {
            StatementResult sr = session.run(
                    queryPrefix() + "MATCH (n)<-[:IDENTIFIED_TO]-(tagged) RETURN tagged.uri",
                    parameters(
                            "uri", this.uri().toString()
                    )
            );
            TagPojo mergeWithPojo = tagFactory.withUri(
                    tag.uri()
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
    }

    @Override
    public void setShareLevel(ShareLevel shareLevel) {
        try (Session session = driver.session()) {
            session.run(
                    queryPrefix() + "SET n.shareLevel=$shareLevel ",
                    parameters(
                            "uri", uri().toString(),
                            "shareLevel", shareLevel.getIndex()
                    )
            );
        }
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
    public void comment(String comment) {
        graphElementOperator.comment(
                comment
        );
    }

    @Override
    public void label(String label) {
        graphElementOperator.label(
                label
        );
    }

    @Override
    public void addImages(Set<Image> images) {
        graphElementOperator.addImages(
                images
        );
    }

    @Override
    public void create() {
        graphElementOperator.create();
    }

    @Override
    public void createUsingInitialValues(Map<String, Object> values) {
        graphElementOperator.createUsingInitialValues(
                values
        );
    }

    @Override
    public void remove() {
        graphElementOperator.remove();
    }

    @Override
    public void removeIdentification(Tag type) {

    }

    @Override
    public Map<URI, TagPojo> addMeta(Tag friendlyResource) {
        return null;
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
        graphElementOperator.setChildrenIndex(childrenIndex);
    }

    @Override
    public Boolean isUnderPattern() {
        return graphElementOperator.isUnderPattern();
    }

    @Override
    public Boolean isPatternOrUnderPattern() {
        return this.isUnderPattern();
    }

    @Override
    public String queryPrefix() {
        return graphElementOperator.queryPrefix();
    }

    @Override
    public Map<String, Object> addCreationProperties(Map<String, Object> map) {
        return graphElementOperator.addCreationProperties(map);
    }

    @Override
    public boolean equals(Object toCompare) {
        return graphElementOperator.equals(toCompare);
    }

    @Override
    public int hashCode() {
        return graphElementOperator.hashCode();
    }
}
