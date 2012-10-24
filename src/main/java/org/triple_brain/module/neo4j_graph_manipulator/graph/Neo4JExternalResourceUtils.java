package org.triple_brain.module.neo4j_graph_manipulator.graph;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.ReadableIndex;

import javax.inject.Inject;
import java.net.URI;

/*
* Copyright Mozilla Public License 1.1
*/
public class Neo4JExternalResourceUtils {

    @Inject
    private ReadableIndex<Node> nodeIndex;

    @Inject
    private GraphDatabaseService graphDb;

    public Node getOrCreateNodeWithUri(URI uri) {
        if(alreadyExists(uri)){
            return getFromUri(uri);
        }else{
            return create(uri);
        }
    }

    public Node create(URI uri){
        Node externalResourceAsNode = graphDb.createNode();
        externalResourceAsNode.setProperty(
                Neo4JUserGraph.URI_PROPERTY_NAME,
                uri.toString()
        );
        return externalResourceAsNode;
    }

    public Node getFromUri(URI uri){
        return nodeIndex.get(
                Neo4JUserGraph.URI_PROPERTY_NAME,
                uri.toString()
        ).getSingle();
    }

    public Boolean alreadyExists(URI uri){
        return nodeIndex.get(
                Neo4JUserGraph.URI_PROPERTY_NAME,
                uri.toString()
        ).hasNext();
    }



}
