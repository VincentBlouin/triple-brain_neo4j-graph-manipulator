package org.triple_brain.module.neo4j_graph_manipulator.graph;

import org.junit.Test;
import org.triple_brain.module.model.graph.Edge;
import org.triple_brain.module.model.graph.Vertex;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/*
* Copyright Mozilla Public License 1.1
*/
public class Neo4JVertexTest extends Neo4JGeneralGraphManipulatorTest{

    @Test
    public void can_update_label() {
        Edge newEdge = vertexA.addVertexAndRelation();
        Vertex vertex = newEdge.destinationVertex();
        vertex.label("Ju-Ji-Tsu");
        assertThat(vertex.label(), is("Ju-Ji-Tsu"));
    }
}
