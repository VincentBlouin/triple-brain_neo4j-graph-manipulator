/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.graph.edge;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import guru.bubl.module.model.Image;
import guru.bubl.module.model.UserUris;
import guru.bubl.module.model.graph.GraphElement;
import guru.bubl.module.model.graph.GraphElementOperator;
import guru.bubl.module.model.graph.ShareLevel;
import guru.bubl.module.model.graph.edge.EdgeOperator;
import guru.bubl.module.model.graph.edge.EdgePojo;
import guru.bubl.module.model.graph.group_relation.GroupRelationPojo;
import guru.bubl.module.model.graph.tag.Tag;
import guru.bubl.module.model.graph.tag.TagPojo;
import guru.bubl.module.model.graph.vertex.VertexPojo;
import guru.bubl.module.model.graph.vertex.VertexOperator;
import guru.bubl.module.neo4j_graph_manipulator.graph.FriendlyResourceNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.OperatorNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.Relationships;
import guru.bubl.module.neo4j_graph_manipulator.graph.RestApiUtilsNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.GraphElementFactoryNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.GraphElementOperatorNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.GraphElementSpecialOperatorFactory;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.group_relation.GroupRelationFactoryNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.group_relation.GroupRelationOperatorNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.vertex.VertexFactoryNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.vertex.VertexOperatorNeo4j;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;

import javax.inject.Inject;
import java.net.URI;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import static guru.bubl.module.neo4j_graph_manipulator.graph.RestApiUtilsNeo4j.map;
import static guru.bubl.module.neo4j_graph_manipulator.graph.graph.GraphElementOperatorNeo4j.decrementNbNeighborsQueryPart;
import static guru.bubl.module.neo4j_graph_manipulator.graph.graph.GraphElementOperatorNeo4j.incrementNbNeighborsQueryPart;
import static org.neo4j.driver.v1.Values.parameters;

public class EdgeOperatorNeo4j implements EdgeOperator, OperatorNeo4j {

    protected GraphElementOperatorNeo4j graphElementOperator;
    protected VertexFactoryNeo4j vertexFactory;
    protected EdgeFactoryNeo4j edgeFactory;

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

    @Inject
    protected GraphElementSpecialOperatorFactory graphElementSpecialOperatorFactory;

    @AssistedInject
    protected EdgeOperatorNeo4j(
            VertexFactoryNeo4j vertexFactory,
            EdgeFactoryNeo4j edgeFactory,
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
    protected EdgeOperatorNeo4j(
            VertexFactoryNeo4j vertexFactory,
            EdgeFactoryNeo4j edgeFactory,
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
    protected EdgeOperatorNeo4j(
            VertexFactoryNeo4j vertexFactory,
            EdgeFactoryNeo4j edgeFactory,
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
        try (Session session = driver.session()) {
            Record record = session.run(
                    queryPrefix() +
                            "MATCH (n)-[:SOURCE]->(v) " +
                            "RETURN v.uri as uri",
                    parameters(
                            "uri",
                            uri().toString()
                    )
            ).single();
            return URI.create(record.get("uri").asString());
        }
    }

    @Override
    public URI destinationUri() {
        try (Session session = driver.session()) {
            Record record = session.run(
                    queryPrefix() +
                            "MATCH (n)-[:DESTINATION]->(v) " +
                            "RETURN v.uri as uri",
                    parameters(
                            "uri",
                            uri().toString()
                    )
            ).single();
            return URI.create(record.get("uri").asString());
        }
    }

    @Override
    public GraphElement sourceFork() {
        return graphElementSpecialOperatorFactory.getFromUri(sourceUri());
    }

    @Override
    public GraphElement destinationFork() {
        return graphElementSpecialOperatorFactory.getFromUri(destinationUri());
    }

    @Override
    public void changeSource(
            URI newSourceUri,
            ShareLevel oldEndShareLevel,
            ShareLevel keptEndShareLevel,
            ShareLevel newEndShareLevel
    ) {
        changeEndVertex(
                newSourceUri,
                Relationships.SOURCE,
                oldEndShareLevel,
                keptEndShareLevel,
                newEndShareLevel
        );
    }

    @Override
    public void changeDestination(
            URI newDestinationUri,
            ShareLevel oldEndShareLevel,
            ShareLevel keptEndShareLevel,
            ShareLevel newEndShareLevel
    ) {
        changeEndVertex(
                newDestinationUri,
                Relationships.DESTINATION,
                oldEndShareLevel,
                keptEndShareLevel,
                newEndShareLevel
        );
    }

    private void changeEndVertex(
            URI newEndUri,
            Relationships relationshipToChange,
            ShareLevel oldEndShareLevel,
            ShareLevel keptEndShareLevel,
            ShareLevel newEndShareLevel
    ) {
        Relationships relationshipToKeep = Relationships.SOURCE == relationshipToChange ?
                Relationships.DESTINATION : Relationships.SOURCE;
        GraphElementOperator sourceVertex = graphElementSpecialOperatorFactory.getFromUri(sourceUri());
        GraphElementOperator destinationVertex = graphElementSpecialOperatorFactory.getFromUri(destinationUri());
        String decrementPreviousVertexQueryPart = decrementNbNeighborsQueryPart(
                keptEndShareLevel,
                "prev_v",
                "SET "
        );
        String incrementKeptVertexQueryPart = "";
        String decrementKeptVertexQueryPart = "";
        if (oldEndShareLevel.isSame(keptEndShareLevel)) {
            if (!newEndShareLevel.isSame(keptEndShareLevel)) {
                incrementKeptVertexQueryPart = incrementNbNeighborsQueryPart(
                        newEndShareLevel,
                        "kept_v",
                        ", "
                );
            }
        } else {
            decrementKeptVertexQueryPart = decrementNbNeighborsQueryPart(
                    oldEndShareLevel,
                    "kept_v",
                    ", "
            );
        }

        String incrementNewEndVertexQueryPart = incrementNbNeighborsQueryPart(
                keptEndShareLevel,
                "new_v",
                ", "
        );
        String query = String.format(
                "%s, (new_v:Resource{uri:$endVertexUri}), " +
                        "(n)-[prev_rel:%s]->(prev_v), " +
                        "(n)-[:%s]->(kept_v) " +
                        "MERGE (n)-[:%s]->(new_v) " +
                        "DELETE prev_rel " +
                        decrementPreviousVertexQueryPart +
                        decrementKeptVertexQueryPart +
                        incrementKeptVertexQueryPart +
                        incrementNewEndVertexQueryPart + ",%s",
                queryPrefix(),
                relationshipToChange,
                relationshipToKeep,
                relationshipToChange,
                FriendlyResourceNeo4j.LAST_MODIFICATION_QUERY_PART
        );
        try (Session session = driver.session()) {
            session.run(
                    query,
                    parameters(
                            "uri",
                            this.uri().toString(),
                            "endVertexUri",
                            newEndUri.toString(),
                            "last_modification_date",
                            new Date().getTime()
                    )
            );
        }
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
    public EdgePojo createEdge() {
        return createEdgeUsingInitialValues(
                edgeCreateProperties
        );
    }

    @Override
    public EdgePojo createEdgeWithAdditionalProperties(Map<String, Object> props) {
        props.putAll(
                edgeCreateProperties
        );
        return createEdgeUsingInitialValues(
                props
        );
    }

    @Override
    public EdgePojo createWithShareLevel(ShareLevel shareLevel) {
        return createEdgeUsingInitialValues(
                map(
                        VertexOperatorNeo4j.props.shareLevel.name(),
                        shareLevel.getIndex()
                )
        );
    }

    @Override
    public GroupRelationPojo convertToGroupRelation(TagPojo tag, Boolean isNewTag, ShareLevel initialShareLevel) {
        String existingTagQueryPart = isNewTag ? "" : "WITH n, gr " +
                "MATCH(tag:Resource{uri:$tagUri}), " +
                "(n)-[t:IDENTIFIED_TO]->(tag) " +
                "MERGE (gr)-[:IDENTIFIED_TO]->(tag) " +
                "DELETE t";
        URI newGroupRelationUri = new UserUris(graphElementOperator.getOwnerUsername()).generateGroupRelationUri();
        GroupRelationOperatorNeo4j groupRelationOperator = groupRelationFactoryNeo4j.withUri(newGroupRelationUri);
        try (Session session = driver.session()) {
            session.run(
                    queryPrefix() +
                            "CREATE(gr:Resource:GraphElement:GroupRelation $groupRelation) " +
                            "WITH n, gr " +
                            "MATCH (n)-[r:SOURCE]->(s) " +
                            "MERGE (gr)-[:SOURCE]->(s) " +
                            "MERGE (gr)-[:DESTINATION]->(n) " +
                            "MERGE (n)-[:SOURCE]->(gr) " +
                            "DELETE r " +
                            existingTagQueryPart,
                    parameters(
                            "uri", uri().toString(),
                            "groupRelation",
                            groupRelationOperator.addCreationProperties(
                                    RestApiUtilsNeo4j.map(
                                            "shareLevel", initialShareLevel.getIndex()
                                    )
                            ),
                            "tagUri", tag.hasUri() ? tag.uri().toString() : null
                    )
            );
        }
        if (isNewTag) {
            groupRelationOperator.addTag(
                    tag,
                    initialShareLevel
            );
        }
        return new GroupRelationPojo(
                newGroupRelationUri,
                tag
        );
    }

    @Override
    public EdgePojo createEdgeUsingInitialValues(Map<String, Object> values) {
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
            EdgePojo edge = new EdgePojo(
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
}
