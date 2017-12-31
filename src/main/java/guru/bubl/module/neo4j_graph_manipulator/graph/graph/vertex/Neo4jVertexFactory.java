/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.graph.vertex;

import guru.bubl.module.model.graph.vertex.VertexFactory;
import org.neo4j.graphdb.Node;

import java.net.URI;

public interface Neo4jVertexFactory extends VertexFactory {
    Neo4jVertexInSubGraphOperator createOrLoadUsingNode(Node node);

    @Override
    Neo4jVertexInSubGraphOperator createForOwner(String username);

    @Override
    Neo4jVertexInSubGraphOperator withUri(URI uri);
}
