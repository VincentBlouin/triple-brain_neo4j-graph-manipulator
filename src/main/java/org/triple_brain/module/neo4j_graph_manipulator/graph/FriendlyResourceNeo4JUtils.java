package org.triple_brain.module.neo4j_graph_manipulator.graph;

import com.hp.hpl.jena.vocabulary.RDFS;
import org.neo4j.graphdb.Node;
import org.triple_brain.module.common_utils.Uris;
import org.triple_brain.module.model.ExternalFriendlyResource;

import javax.inject.Inject;
import java.net.URI;

/*
* Copyright Mozilla Public License 1.1
*/
public class FriendlyResourceNeo4JUtils {

    @Inject
    Neo4JExternalResourceUtils externalResourceUtils;

    public ExternalFriendlyResource loadFromNode(Node node) {
        return ExternalFriendlyResource.withUriAndLabel(
                Uris.get(node.getProperty(Neo4JUserGraph.URI_PROPERTY_NAME).toString()),
                node.getProperty(RDFS.label.getURI()).toString()
        );
    }

    public ExternalFriendlyResource getFromUri(URI uri) {
        Node node = externalResourceUtils.getFromUri(uri);
        return ExternalFriendlyResource.withUriAndLabel(
                Uris.get(node.getProperty(Neo4JUserGraph.URI_PROPERTY_NAME).toString()),
                node.getProperty(RDFS.label.getURI()).toString()
        );
    }

    public Node getOrCreate(ExternalFriendlyResource friendlyResource) {
        if (externalResourceUtils.alreadyExists(friendlyResource.uri())) {
            Node node = externalResourceUtils.getFromUri(
                    friendlyResource.uri()
            );
            if(!node.hasProperty(RDFS.label.getURI().toString())){
                node.setProperty(
                        RDFS.label.getURI().toString(),
                        friendlyResource.label()
                );
            }
            return node;
        } else {
            return addInGraph(friendlyResource);
        }
    }

    public Node addInGraph(ExternalFriendlyResource friendlyResource) {
        Node friendlyResourceAsNode = externalResourceUtils.create(
                friendlyResource.uri()
        );
        friendlyResourceAsNode.setProperty(
                RDFS.label.getURI(),
                friendlyResource.label()
        );
        return friendlyResourceAsNode;
    }

}

