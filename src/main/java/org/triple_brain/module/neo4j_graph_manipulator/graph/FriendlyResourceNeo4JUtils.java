package org.triple_brain.module.neo4j_graph_manipulator.graph;

import com.hp.hpl.jena.vocabulary.RDFS;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.ReadableIndex;
import org.triple_brain.module.common_utils.Uris;
import org.triple_brain.module.model.FriendlyResource;

import javax.inject.Inject;
import java.net.URI;

/*
* Copyright Mozilla Public License 1.1
*/
public class FriendlyResourceNeo4JUtils {

    @Inject
    private ReadableIndex<Node> nodeIndex;

    @Inject
    private GraphDatabaseService graphDb;

    public FriendlyResource loadFromUri(URI uri){
        Node node = nodeIndex.get(
                Neo4JUserGraph.URI_PROPERTY_NAME,
                uri.toString()
        ).getSingle();
        return loadFromNode(node);
    }

    public FriendlyResource loadFromNode(Node node){
        return FriendlyResource.withUriAndLabel(
                Uris.get(node.getProperty(Neo4JUserGraph.URI_PROPERTY_NAME).toString()),
                node.getProperty(RDFS.label.getURI()).toString()
        );
    }

    public Node addInGraph(FriendlyResource friendlyResource){
        Node friendlyResourceAsNode = graphDb.createNode();
        friendlyResourceAsNode.setProperty(
                Neo4JUserGraph.URI_PROPERTY_NAME,
                friendlyResource.uri().toString()
        );
        friendlyResourceAsNode.setProperty(
                RDFS.label.getURI(),
                friendlyResource.label()
        );
        return friendlyResourceAsNode;
    }

    public void remove(FriendlyResource friendlyResource){
        Node node = nodeIndex.get(
                Neo4JUserGraph.URI_PROPERTY_NAME,
                friendlyResource.uri().toString()
        ).getSingle();
        node.getRelationships()
                .iterator()
                .next()
                .delete();
        node.removeProperty(RDFS.label.getURI());
        node.removeProperty(Neo4JUserGraph.URI_PROPERTY_NAME);
        node.delete();
    }

}
