package guru.bubl.module.neo4j_graph_manipulator.graph.graph.group_relation;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import guru.bubl.module.model.Image;
import guru.bubl.module.model.graph.ShareLevel;
import guru.bubl.module.model.graph.edge.EdgePojo;
import guru.bubl.module.model.graph.group_relation.GroupRelationOperator;
import guru.bubl.module.model.graph.tag.Tag;
import guru.bubl.module.model.graph.tag.TagFactory;
import guru.bubl.module.model.graph.tag.TagOperator;
import guru.bubl.module.model.graph.tag.TagPojo;
import guru.bubl.module.model.graph.fork.NbNeighbors;
import guru.bubl.module.model.graph.fork.ForkOperatorFactory;
import guru.bubl.module.neo4j_graph_manipulator.graph.OperatorNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.GraphElementFactoryNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.GraphElementOperatorNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.vertex.VertexOperatorNeo4j;

import java.net.URI;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import static guru.bubl.module.neo4j_graph_manipulator.graph.RestApiUtilsNeo4j.map;


public class GroupRelationOperatorNeo4j implements GroupRelationOperator, OperatorNeo4j {

    protected GraphElementOperatorNeo4j graphElementOperator;
    protected TagOperator tagOperator;

    @Inject
    protected ForkOperatorFactory forkOperatorFactory;

    @AssistedInject
    protected GroupRelationOperatorNeo4j(
            TagFactory tagFactory,
            GraphElementFactoryNeo4j graphElementFactory,
            @Assisted URI uri
    ) {
        this.graphElementOperator = graphElementFactory.withUri(
                uri
        );
    }

    @AssistedInject
    protected GroupRelationOperatorNeo4j(
            TagFactory tagFactory,
            GraphElementFactoryNeo4j graphElementFactory,
            @Assisted("self") URI uri,
            @Assisted("tag") URI tagUri
    ) {
        this.graphElementOperator = graphElementFactory.withUri(
                uri
        );
        this.tagOperator = tagFactory.withUri(tagUri);
    }

    @Override
    public void comment(String comment) {
        tagOperator.comment(comment);
    }

    @Override
    public void label(String label) {
        tagOperator.label(label);
    }

    @Override
    public void addImages(Set<Image> images) {
        tagOperator.addImages(images);
    }

    @Override
    public void create() {

    }

    @Override
    public void createUsingInitialValues(Map<String, Object> values) {

    }

    @Override
    public void remove() {
        graphElementOperator.remove();
    }

    @Override
    public void setColors(String colors) {
        tagOperator.setColors(colors);
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
        tagOperator.setFont(font);
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
        return tagOperator.getFont();
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
        return tagOperator.getShareLevel();
    }

    @Override
    public URI uri() {
        return graphElementOperator.uri();
    }

    @Override
    public boolean hasLabel() {
        return tagOperator.hasLabel();
    }

    @Override
    public String label() {
        return tagOperator.label();
    }

    @Override
    public Set<Image> images() {
        return tagOperator.images();
    }

    @Override
    public Boolean gotImages() {
        return tagOperator.gotImages();
    }

    @Override
    public String comment() {
        return tagOperator.comment();
    }

    @Override
    public Boolean gotComments() {
        return tagOperator.gotComments();
    }

    @Override
    public Date creationDate() {
        return tagOperator.creationDate();
    }

    @Override
    public Date lastModificationDate() {
        return tagOperator.lastModificationDate();
    }

    @Override
    public String getColors() {
        return tagOperator.getColors();
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
    public EdgePojo addVertexAndRelation() {
        return forkOperatorFactory.withUri(uri()).addVertexAndRelation();
    }

    @Override
    public EdgePojo addVertexAndRelationWithIds(String vertexId, String edgeId) {
        return forkOperatorFactory.withUri(uri()).addVertexAndRelationWithIds(
                vertexId,
                edgeId
        );
    }
}
