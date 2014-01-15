package org.triple_brain.module.neo4j_graph_manipulator.graph;

import org.apache.commons.lang.StringUtils;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.index.ReadableIndex;
import org.triple_brain.module.common_utils.Uris;
import org.triple_brain.module.model.graph.vertex.Vertex;

import javax.inject.Inject;
import java.net.URI;

/*
* Copyright Mozilla Public License 1.1
*/
public class Neo4JUtils {

    @Inject
    private ReadableIndex<Node> nodeIndex;

    @Inject
    private GraphDatabaseService graphDb;

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
                Neo4JUserGraph.URI_PROPERTY_NAME,
                uri.toString()
        ).getSingle();
    }

    public URI uriOfNode(Node node) {
        return Uris.get(node.getProperty(
                Neo4JUserGraph.URI_PROPERTY_NAME
        ).toString());
    }

    public URI getUriOfEndNodeUsingRelationship(Node node, RelationshipType relationshipType) {
        return Uris.get(node
                .getSingleRelationship(relationshipType, Direction.OUTGOING)
                .getEndNode()
                .getProperty(
                        Neo4JUserGraph.URI_PROPERTY_NAME
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
        Node externalResourceAsNode = graphDb.createNode();
        externalResourceAsNode.setProperty(
                Neo4JUserGraph.URI_PROPERTY_NAME,
                uri.toString()
        );
        return externalResourceAsNode;
    }

    public Node getFromUri(URI uri) {
        return nodeIndex.get(
                Neo4JUserGraph.URI_PROPERTY_NAME,
                uri.toString()
        ).getSingle();
    }

    public Boolean alreadyExists(URI uri) {
        return nodeIndex.get(
                Neo4JUserGraph.URI_PROPERTY_NAME,
                uri.toString()
        ).hasNext();
    }
}
