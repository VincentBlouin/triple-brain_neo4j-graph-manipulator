package org.triple_brain.module.neo4j_graph_manipulator.graph;

import org.neo4j.cypher.ExecutionEngine;
import org.neo4j.cypher.ExecutionResult;
import org.neo4j.graphdb.Node;
import org.triple_brain.module.model.WholeGraph;
import org.triple_brain.module.model.graph.edge.EdgeOperator;
import org.triple_brain.module.model.graph.vertex.VertexOperator;

import javax.inject.Inject;
import java.util.Iterator;

/*
* Copyright Mozilla Public License 1.1
*/
public class Neo4jWholeGraph implements WholeGraph {

    @Inject
    protected ExecutionEngine engine;

    @Inject
    protected Neo4jVertexFactory neo4jVertexFactory;

    @Inject
    protected Neo4jEdgeFactory neo4jEdgeFactory;

    @Override
    public Iterator<VertexOperator> getAllVertices() {
        return new Iterator<VertexOperator>() {
            ExecutionResult result = engine.execute(
                    "START n = node(*) " +
                            "MATCH n:vertex " +
                            "RETURN n"
            );

            @Override
            public boolean hasNext() {
                return result.hasNext();
            }

            @Override
            public VertexOperator next() {
                return neo4jVertexFactory.createOrLoadUsingNode(
                        (Node) result.next().get("n").get()
                );
            }

            @Override
            public void remove() {
                //To change body of implemented methods use File | Settings | File Templates.
            }
        };
    }

    @Override
    public Iterator<EdgeOperator> getAllEdges() {
        return new Iterator<EdgeOperator>() {
            ExecutionResult result = engine.execute(
                    "START relation=node(*) " +
                            "MATCH relation-[:" +
                            Relationships.SOURCE_VERTEX +
                            "]->vertex " +
                            "RETURN relation"
            );

            @Override
            public boolean hasNext() {
                return result.hasNext();
            }

            @Override
            public EdgeOperator next() {
                return neo4jEdgeFactory.createOrLoadWithNode(
                        (Node) result.next().get("relation").get()
                );
            }

            @Override
            public void remove() {
                //To change body of implemented methods use File | Settings | File Templates.
            }
        };
    }
}
