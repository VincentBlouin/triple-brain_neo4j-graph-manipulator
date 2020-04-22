/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.graph.relation;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import guru.bubl.module.model.Image;
import guru.bubl.module.model.UserUris;
import guru.bubl.module.model.graph.graph_element.GraphElement;
import guru.bubl.module.model.graph.ShareLevel;
import guru.bubl.module.model.graph.edge.EdgeOperatorFactory;
import guru.bubl.module.model.graph.group_relation.GroupRelationPojo;
import guru.bubl.module.model.graph.relation.RelationOperator;
import guru.bubl.module.model.graph.relation.RelationPojo;
import guru.bubl.module.model.graph.tag.Tag;
import guru.bubl.module.model.graph.tag.TagPojo;
import guru.bubl.module.model.graph.vertex.VertexPojo;
import guru.bubl.module.neo4j_graph_manipulator.graph.FriendlyResourceNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.OperatorNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.RestApiUtilsNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.graph_element.GraphElementFactoryNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.graph_element.GraphElementOperatorNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.group_relation.GroupRelationFactoryNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.group_relation.GroupRelationOperatorNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.vertex.VertexFactoryNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.vertex.VertexOperatorNeo4j;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Session;

import javax.inject.Inject;
import java.net.URI;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import static guru.bubl.module.neo4j_graph_manipulator.graph.RestApiUtilsNeo4j.map;
import static org.neo4j.driver.v1.Values.parameters;

public class RelationOperatorNeo4J implements RelationOperator, OperatorNeo4j {

    @Inject
    private EdgeOperatorFactory edgeOperatorFactory;

    protected GraphElementOperatorNeo4j graphElementOperator;
    protected VertexFactoryNeo4j vertexFactory;
    protected RelationFactoryNeo4j edgeFactory;

    protected URI sourceUri;
    protected URI destinationUri;

    private static final Map<String, Object> edgeCreateProperties = map(
            VertexOperatorNeo4j.props.shareLevel.name(), ShareLevel.PRIVATE.getIndex()
    );

    @Inject
    protected
    Driver driver;

    @Inject
    protected GroupRelationFactoryNeo4j groupRelationFactoryNeo4j;

    @AssistedInject
    protected RelationOperatorNeo4J(
            VertexFactoryNeo4j vertexFactory,
            RelationFactoryNeo4j edgeFactory,
            GraphElementFactoryNeo4j graphElementFactory,
            @Assisted URI uri
    ) {
        this.vertexFactory = vertexFactory;
        this.edgeFactory = edgeFactory;
        this.graphElementOperator = graphElementFactory.withUri(
                uri
        );
    }

    @AssistedInject
    protected RelationOperatorNeo4J(
            VertexFactoryNeo4j vertexFactory,
            RelationFactoryNeo4j edgeFactory,
            GraphElementFactoryNeo4j graphElementFactory,
            @Assisted("source") URI sourceUri,
            @Assisted("destination") URI destinationUri
    ) {
        this.vertexFactory = vertexFactory;
        this.edgeFactory = edgeFactory;
        UserUris userUris = new UserUris(
                UserUris.ownerUserNameFromUri(sourceUri)
        );
        URI newEdgeUri = userUris.generateEdgeUri();
        this.graphElementOperator = graphElementFactory.withUri(
                newEdgeUri
        );
        this.sourceUri = sourceUri;
        this.destinationUri = destinationUri;
    }

    @AssistedInject
    protected RelationOperatorNeo4J(
            VertexFactoryNeo4j vertexFactory,
            RelationFactoryNeo4j edgeFactory,
            GraphElementFactoryNeo4j graphElementFactory,
            @Assisted URI uri,
            @Assisted("source") URI sourceUri,
            @Assisted("destination") URI destinationUri
    ) {
        this.vertexFactory = vertexFactory;
        this.edgeFactory = edgeFactory;
        this.graphElementOperator = graphElementFactory.withUri(
                uri
        );
        this.sourceUri = sourceUri;
        this.destinationUri = destinationUri;
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

    @Override
    public void inverse() {
        try (Session session = driver.session()) {
            session.run(
                    String.format(
                            "%sMATCH (n)-[source_rel:SOURCE]->(source_vertex), " +
                                    "(n)-[destination_rel:DESTINATION]->(destination_vertex) " +
                                    "MERGE (n)-[:DESTINATION]->(source_vertex) " +
                                    "MERGE (n)-[:SOURCE]->(destination_vertex) " +
                                    "DELETE source_rel, destination_rel " +
                                    "SET %s",
                            queryPrefix(),
                            FriendlyResourceNeo4j.LAST_MODIFICATION_QUERY_PART
                    ),
                    parameters(
                            "uri",
                            this.uri().toString(),
                            "last_modification_date",
                            new Date().getTime()
                    )
            );
        }
    }

    @Override
    public void remove() {
        this.graphElementOperator.remove();
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
    public URI uri() {
        return graphElementOperator.uri();
    }

    @Override
    public String label() {
        return graphElementOperator.label();
    }

    @Override
    public void label(String label) {
        graphElementOperator.label(label);
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
    public void comment(String comment) {
        graphElementOperator.comment(
                comment
        );
    }

    @Override
    public Boolean gotComments() {
        return graphElementOperator.gotComments();
    }

    @Override
    public void addImages(Set<Image> images) {
        graphElementOperator.addImages(images);
    }

    @Override
    public boolean hasLabel() {
        return graphElementOperator.hasLabel();
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
    public Boolean isUnderPattern() {
        return graphElementOperator.isUnderPattern();
    }

    @Override
    public Boolean isPatternOrUnderPattern() {
        return graphElementOperator.isUnderPattern();
    }

    @Override
    public String getPrivateContext() {
        return graphElementOperator.getPrivateContext();
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
    public Boolean isPublic() {
        return graphElementOperator.isPublic();
    }

    @Override
    public void create() {
        createEdge();
    }

    @Override
    public RelationPojo createEdge() {
        return createEdgeUsingInitialValues(
                edgeCreateProperties
        );
    }

    @Override
    public RelationPojo createEdgeWithAdditionalProperties(Map<String, Object> props) {
        props.putAll(
                edgeCreateProperties
        );
        return createEdgeUsingInitialValues(
                props
        );
    }

    @Override
    public RelationPojo createWithShareLevel(ShareLevel shareLevel) {
        return createEdgeUsingInitialValues(
                map(
                        VertexOperatorNeo4j.props.shareLevel.name(),
                        shareLevel.getIndex()
                )
        );
    }

    @Override
    public GroupRelationPojo convertToGroupRelation(String newGroupRelationId, ShareLevel initialShareLevel, String label, String note) {
        UserUris userUris = new UserUris(graphElementOperator.getOwnerUsername());
        URI newGroupRelationUri = userUris.groupRelationUriFromShortId(newGroupRelationId);
        GroupRelationOperatorNeo4j groupRelationOperator = groupRelationFactoryNeo4j.withUri(newGroupRelationUri);
        try (Session session = driver.session()) {
            session.run(
                    queryPrefix() +
                            "CREATE(gr:Resource:GraphElement:GroupRelation $groupRelation) " +
                            "WITH n, gr " +
                            "MATCH (n)-[r:SOURCE]->(s) " +
                            "MERGE (gr)-[:SOURCE]->(s) " +
                            "MERGE (n)-[:SOURCE]->(gr) " +
                            "DELETE r " +
                            "WITH n,gr " +
                            "MATCH (n)-[r:IDENTIFIED_TO]->(t) " +
                            "MERGE (gr)-[:IDENTIFIED_TO]->(t) " +
                            "DELETE r",
                    parameters(
                            "uri", uri().toString(),
                            "groupRelation",
                            groupRelationOperator.addCreationProperties(
                                    RestApiUtilsNeo4j.map(
                                            "shareLevel", initialShareLevel.getIndex(),
                                            initialShareLevel.getNbNeighborsPropertyName(), 2,
                                            "label", label,
                                            "comment", note
                                    )
                            )
                    )
            );
        }
        return new GroupRelationPojo(
                newGroupRelationUri
        );
    }

    @Override
    public RelationPojo createEdgeUsingInitialValues(Map<String, Object> values) {
        Map<String, Object> creationProperties = addCreationProperties(values);
        try (Session session = driver.session()) {
            session.run(
                    "MATCH (source_node:Resource{uri:$sourceUri}), " +
                            "(destination_node:Resource{uri:$destinationUri}) " +
                            "CREATE (n:Resource:GraphElement:Edge $edge) MERGE (n)-[:SOURCE]->(source_node) MERGE (n)-[:DESTINATION]->(destination_node)",
                    parameters(
                            "sourceUri",
                            sourceUri.toString(),
                            "destinationUri",
                            destinationUri.toString(),
                            "edge",
                            creationProperties
                    )
            );
            RelationPojo edge = new RelationPojo(
                    graphElementOperator.pojoFromCreationProperties(
                            creationProperties
                    )
            );
            edge.setSourceVertex(new VertexPojo(
                    sourceUri
            ));
            edge.setDestinationVertex(new VertexPojo(
                    destinationUri
            ));
            return edge;
        }
    }

    @Override
    public void createUsingInitialValues(Map<String, Object> values) {
        createEdgeUsingInitialValues(values);
    }

    @Override
    public void removeTag(Tag tag, ShareLevel sourceShareLevel) {
        graphElementOperator.removeTag(tag, sourceShareLevel);
    }

    @Override
    public Map<URI, TagPojo> addTag(Tag friendlyResource, ShareLevel sourceShareLevel) {
        return graphElementOperator.addTag(friendlyResource, sourceShareLevel);
    }

    @Override
    public Map<URI, TagPojo> getTags() {
        return graphElementOperator.getTags();
    }

    @Override
    public boolean equals(Object edgeToCompareAsObject) {
        return graphElementOperator.equals(edgeToCompareAsObject);
    }

    @Override
    public int hashCode() {
        return graphElementOperator.hashCode();
    }

    @Override
    public String queryPrefix() {
        return graphElementOperator.queryPrefix();
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
}
