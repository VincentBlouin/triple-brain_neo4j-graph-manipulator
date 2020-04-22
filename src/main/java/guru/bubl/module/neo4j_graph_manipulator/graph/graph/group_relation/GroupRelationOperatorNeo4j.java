package guru.bubl.module.neo4j_graph_manipulator.graph.graph.group_relation;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import guru.bubl.module.model.Image;
import guru.bubl.module.model.graph.graph_element.GraphElement;
import guru.bubl.module.model.graph.ShareLevel;
import guru.bubl.module.model.graph.edge.EdgeOperatorFactory;
import guru.bubl.module.model.graph.fork.ForkOperator;
import guru.bubl.module.model.graph.relation.RelationOperator;
import guru.bubl.module.model.graph.relation.RelationPojo;
import guru.bubl.module.model.graph.fork.ForkOperatorFactory;
import guru.bubl.module.model.graph.fork.NbNeighbors;
import guru.bubl.module.model.graph.group_relation.GroupRelationOperator;
import guru.bubl.module.model.graph.tag.Tag;
import guru.bubl.module.model.graph.tag.TagPojo;
import guru.bubl.module.neo4j_graph_manipulator.graph.OperatorNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.graph_element.GraphElementFactoryNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.graph_element.GraphElementOperatorNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.vertex.VertexOperatorNeo4j;

import java.net.URI;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import static guru.bubl.module.neo4j_graph_manipulator.graph.RestApiUtilsNeo4j.map;


public class GroupRelationOperatorNeo4j implements GroupRelationOperator, OperatorNeo4j {

    protected GraphElementOperatorNeo4j graphElementOperator;

    @Inject
    protected ForkOperatorFactory forkOperatorFactory;

    @Inject
    protected EdgeOperatorFactory edgeOperatorFactory;

    @AssistedInject
    protected GroupRelationOperatorNeo4j(
            GraphElementFactoryNeo4j graphElementFactory,
            @Assisted URI uri
    ) {
        this.graphElementOperator = graphElementFactory.withUri(
                uri
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

    }

    @Override
    public void createUsingInitialValues(Map<String, Object> values) {

    }

    @Override
    public void remove() {
        forkOperatorFactory.withUri(uri()).remove();
    }

    @Override
    public void setColors(String colors) {
        graphElementOperator.setColors(colors);
    }

    @Override
    public void removeTag(Tag tag, ShareLevel sourceShareLevel) {

    }

    @Override
    public Map<URI, TagPojo> addTag(Tag friendlyResource, ShareLevel sourceShareLevel) {
        return graphElementOperator.addTag(friendlyResource, sourceShareLevel);
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
        return graphElementOperator.isPatternOrUnderPattern();
    }

    @Override
    public String getPrivateContext() {
        return graphElementOperator.getPrivateContext();
    }

    @Override
    public Map<URI, TagPojo> getTags() {
        return graphElementOperator.getTags();
    }

    @Override
    public String getFont() {
        return graphElementOperator.getFont();
    }

    @Override
    public String getChildrenIndex() {
        return graphElementOperator.getChildrenIndex();
    }

    @Override
    public URI getPatternUri() {
        return graphElementOperator.getPatternUri();
    }

    @Override
    public ShareLevel getShareLevel() {
        return graphElementOperator.getShareLevel();
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
    public String getColors() {
        return graphElementOperator.getColors();
    }

    @Override
    public Map<String, Object> addCreationProperties(Map<String, Object> map) {
        Map<String, Object> newMap = map(
                VertexOperatorNeo4j.props.shareLevel.name(), ShareLevel.PRIVATE.getIndex()
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
    public NbNeighbors getNbNeighbors() {
        return forkOperatorFactory.withUri(uri()).getNbNeighbors();
    }


    @Override
    public void setShareLevel(ShareLevel shareLevel) {
        setShareLevel(shareLevel, getShareLevel());
    }

    @Override
    public void setShareLevel(ShareLevel shareLevel, ShareLevel previousShareLevel) {
        forkOperatorFactory.withUri(uri()).setShareLevel(shareLevel, previousShareLevel);
    }

    @Override
    public RelationPojo addVertexAndRelation() {
        return forkOperatorFactory.withUri(uri()).addVertexAndRelation();
    }

    @Override
    public RelationPojo addVertexAndRelationWithIds(String vertexId, String edgeId) {
        return forkOperatorFactory.withUri(uri()).addVertexAndRelationWithIds(
                vertexId,
                edgeId
        );
    }

    @Override
    public RelationOperator addRelationToFork(URI destinationUri, ShareLevel sourceShareLevel, ShareLevel destinationShareLevel) {
        return forkOperatorFactory.withUri(
                uri()
        ).addRelationToFork(
                destinationUri, sourceShareLevel, destinationShareLevel
        );
    }

    @Override
    public void changeSource(URI newSourceUri, ShareLevel oldEndShareLevel, ShareLevel keptEndShareLevel, ShareLevel newEndShareLevel) {
        edgeOperatorFactory.withUri(uri()).changeSource(
                newSourceUri,
                oldEndShareLevel,
                keptEndShareLevel,
                newEndShareLevel
        );
    }

    @Override
    public void changeDestination(URI newDestinationUri, ShareLevel oldEndShareLevel, ShareLevel keptEndShareLevel, ShareLevel newEndShareLevel) {
        edgeOperatorFactory.withUri(uri()).changeDestination(
                newDestinationUri,
                oldEndShareLevel,
                keptEndShareLevel,
                newEndShareLevel
        );
    }

    @Override
    public URI sourceUri() {
        return edgeOperatorFactory.withUri(uri()).sourceUri();
    }

    @Override
    public URI destinationUri() {
        return edgeOperatorFactory.withUri(uri()).destinationUri();
    }

    @Override
    public GraphElement source() {
        return edgeOperatorFactory.withUri(uri()).source();
    }

    @Override
    public GraphElement destination() {
        return edgeOperatorFactory.withUri(uri()).destination();
    }
}
