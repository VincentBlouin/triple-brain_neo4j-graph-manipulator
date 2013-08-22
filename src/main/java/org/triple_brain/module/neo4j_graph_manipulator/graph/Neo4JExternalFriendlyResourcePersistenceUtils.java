package org.triple_brain.module.neo4j_graph_manipulator.graph;

import com.hp.hpl.jena.vocabulary.RDFS;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.ReadableIndex;
import org.triple_brain.module.common_utils.Uris;
import org.triple_brain.module.model.ExternalFriendlyResource;
import org.triple_brain.module.model.ExternalFriendlyResourcePersistenceUtils;
import org.triple_brain.module.model.Image;

import javax.inject.Inject;
import java.net.URI;
import java.util.Set;

/*
* Copyright Mozilla Public License 1.1
*/
public class Neo4JExternalFriendlyResourcePersistenceUtils implements ExternalFriendlyResourcePersistenceUtils {

    @Inject
    Neo4JExternalResourceUtils externalResourceUtils;

    @Inject
    Neo4JUtils utils;

    @Inject
    private Neo4JImageUtils imageUtils;

    @Inject
    ReadableIndex<Node> nodeIndex;

    public ExternalFriendlyResource loadFromNode(Node node) {
        ExternalFriendlyResource friendlyResource = ExternalFriendlyResource.withUriAndLabel(
                Uris.get(
                        node.getProperty(
                                Neo4JUserGraph.URI_PROPERTY_NAME).toString()
                ),
                node.getProperty(
                        RDFS.label.getURI()
                ).toString()
        );
        friendlyResource.images(
                imageUtils.getImages(node)
        );

        friendlyResource.description(
                node.hasProperty(RDFS.comment.getURI()) ?
                        node.getProperty(
                                RDFS.comment.getURI()
                        ).toString() :
                        ""
        );
        return friendlyResource;
    }

    public ExternalFriendlyResource loadFromUri(URI uri) {
        return loadFromNode(
                nodeIndex.get(
                        Neo4JUserGraph.URI_PROPERTY_NAME,
                        uri.toString()
                ).getSingle()
        );
    }

    @Override
    public void setDescription(ExternalFriendlyResource externalFriendlyResource, String description) {
        Node node = externalResourceUtils.getFromUri(
                externalFriendlyResource.uri()
        );
        node.setProperty(
                RDFS.comment.getURI(),
                description
        );
    }

    @Override
    public void addImages(ExternalFriendlyResource externalFriendlyResource, Set<Image> images) {
        Node node = externalResourceUtils.getFromUri(
                externalFriendlyResource.uri()
        );
        imageUtils.addImages(
                node,
                images
        );
    }

    @Override
    public ExternalFriendlyResource getUpdated(ExternalFriendlyResource externalFriendlyResource) {
        return getFromUri(
                externalFriendlyResource.uri()
        );
    }

    public ExternalFriendlyResource getFromUri(URI uri) {
        Node node = externalResourceUtils.getFromUri(uri);
        return loadFromNode(node);
    }


    public Node getOrCreate(ExternalFriendlyResource friendlyResource) {
        if (externalResourceUtils.alreadyExists(friendlyResource.uri())) {
            Node node = externalResourceUtils.getFromUri(
                    friendlyResource.uri()
            );
            utils.addPropertyIfMissing(
                    node,
                    RDFS.label.getURI().toString(),
                    friendlyResource.label()
            );
            imageUtils.getImages(node);
            return node;
        } else {
            return addInGraph(friendlyResource);
        }
    }

    private Node addInGraph(ExternalFriendlyResource friendlyResource) {
        Node friendlyResourceAsNode = externalResourceUtils.create(
                friendlyResource.uri()
        );
        friendlyResourceAsNode.setProperty(
                RDFS.label.getURI(),
                friendlyResource.label()
        );
        addImages(
                friendlyResource,
                friendlyResource.images()
        );
        setDescription(
                friendlyResource,
                friendlyResource.description()
        );
        return friendlyResourceAsNode;
    }
}
