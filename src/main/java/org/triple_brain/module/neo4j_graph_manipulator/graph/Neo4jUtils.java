/*
 * Copyright Vincent Blouin under the Mozilla Public License 1.1
 */

package org.triple_brain.module.neo4j_graph_manipulator.graph;

import org.apache.commons.lang.StringUtils;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.index.ReadableIndex;
import org.neo4j.rest.graphdb.RestAPI;
import org.triple_brain.module.common_utils.Uris;
import org.triple_brain.module.model.graph.vertex.Vertex;
import org.triple_brain.module.neo4j_graph_manipulator.graph.graph.Neo4jUserGraph;

import javax.inject.Inject;
import java.net.URI;
import java.util.Collections;

public class Neo4jUtils {

    @Inject
    RestAPI restAPI;

    @Inject
    private ReadableIndex<Node> nodeIndex;

    public void removeAllProperties(PropertyContainer propertyContainer) {
        for (String propertyName : propertyContainer.getPropertyKeys()) {
            propertyContainer.removeProperty(propertyName);
        }
    }

    public void removeAllRelationships(Node node) {
        for (Relationship relationship : node.getRelationships()) {
            removeAllProperties(relationship);
            relationship.delete();
        }
    }

    public void removeOutgoingNodesRecursively(Node node) {
        for (Relationship relationship : node.getRelationships(Direction.OUTGOING)) {
            Node endNode = relationship.getEndNode();
            removeAllProperties(endNode);
            removeAllProperties(relationship);
            relationship.delete();
            removeOutgoingNodesRecursively(endNode);
        }
    }

    public Node nodeOfVertex(Vertex vertex) {
        return nodeOfUri(
                vertex.uri()
        );
    }

    public Node nodeOfUri(URI uri) {
        return nodeIndex.get(
                Neo4jUserGraph.URI_PROPERTY_NAME,
                uri.toString()
        ).getSingle();
    }

    public URI uriOfNode(Node node) {
        return Uris.get(node.getProperty(
                Neo4jUserGraph.URI_PROPERTY_NAME
        ).toString());
    }

    public URI getUriOfEndNodeUsingRelationship(Node node, RelationshipType relationshipType) {
        return Uris.get(node
                .getSingleRelationship(relationshipType, Direction.OUTGOING)
                .getEndNode()
                .getProperty(
                        Neo4jUserGraph.URI_PROPERTY_NAME
                ).toString()
        );
    }

    public void addPropertyIfMissing(Node node, String key, Object value) {
        if (!node.hasProperty(key)) {
            node.setProperty(
                    key,
                    value
            );
        }
    }

    public Node getOrCreate(URI uri) {
        return alreadyExists(uri) ?
                getFromUri(uri) :
                create(uri);
    }

    public Node create(URI uri) {
        if(StringUtils.isEmpty(uri.toString())){
            throw new IllegalArgumentException(
                    "uri cannot be empty"
            );
        }
        Node externalResourceAsNode = restAPI.createNode(
                Collections.EMPTY_MAP
        );
        externalResourceAsNode.setProperty(
                Neo4jUserGraph.URI_PROPERTY_NAME,
                uri.toString()
        );
        return externalResourceAsNode;
    }

    public Node getFromUri(URI uri) {
        return nodeIndex.get(
                Neo4jUserGraph.URI_PROPERTY_NAME,
                uri.toString()
        ).getSingle();
    }

    public Boolean alreadyExists(URI uri) {
        return nodeIndex.get(
                Neo4jUserGraph.URI_PROPERTY_NAME,
                uri.toString()
        ).hasNext();
    }
}
