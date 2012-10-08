package org.triple_brain.module.neo4j_graph_manipulator.graph;

import com.hp.hpl.jena.vocabulary.RDFS;
import org.neo4j.graphdb.Node;
import org.triple_brain.module.common_utils.Uris;
import org.triple_brain.module.common_utils.Urls;
import org.triple_brain.module.model.ExternalFriendlyResource;

import javax.inject.Inject;
import java.net.URI;

/*
* Copyright Mozilla Public License 1.1
*/
public class FriendlyResourceNeo4JUtils {

    private static final String IMAGE_URL_PROPERTY_KEY = "image_url";

    @Inject
    Neo4JExternalResourceUtils externalResourceUtils;

    @Inject
    Neo4JUtils utils;

    public ExternalFriendlyResource loadFromNode(Node node) {
        ExternalFriendlyResource friendlyResource = ExternalFriendlyResource.withUriAndLabel(
                Uris.get(node.getProperty(Neo4JUserGraph.URI_PROPERTY_NAME).toString()),
                node.getProperty(RDFS.label.getURI()).toString()
        );
        if(node.hasProperty(IMAGE_URL_PROPERTY_KEY)){
            friendlyResource.imageUrl(
                    Urls.get(
                            node.getProperty(IMAGE_URL_PROPERTY_KEY).toString()
                    )
            );
        }
        return friendlyResource;
    }

    public ExternalFriendlyResource getFromUri(URI uri) {
        Node node = externalResourceUtils.getFromUri(uri);
        ExternalFriendlyResource friendlyResource = ExternalFriendlyResource.withUriAndLabel(
                Uris.get(node.getProperty(Neo4JUserGraph.URI_PROPERTY_NAME).toString()),
                node.getProperty(RDFS.label.getURI()).toString()
                );
        if(node.hasProperty(IMAGE_URL_PROPERTY_KEY)){
            friendlyResource.imageUrl(
                    Urls.get(
                            node.getProperty(IMAGE_URL_PROPERTY_KEY).toString()
                    )
            );
        }
        return friendlyResource;
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
            if (friendlyResource.hasImageUrl()) {
                utils.addPropertyIfMissing(
                        node,
                        IMAGE_URL_PROPERTY_KEY,
                        friendlyResource.imageUrl().toString()
                );
            }
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
        if (friendlyResource.hasImageUrl()) {
            friendlyResourceAsNode.setProperty(
                    IMAGE_URL_PROPERTY_KEY,
                    friendlyResource.imageUrl().toString()
            );
        }
        return friendlyResourceAsNode;
    }

}

