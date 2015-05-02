/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package org.triple_brain.module.neo4j_graph_manipulator.graph.graph;

import org.neo4j.graphdb.Node;
import org.neo4j.rest.graphdb.query.QueryEngine;
import org.neo4j.rest.graphdb.util.QueryResult;
import org.triple_brain.module.model.WholeGraph;
import org.triple_brain.module.model.graph.GraphElementType;
import org.triple_brain.module.model.graph.edge.EdgeOperator;
import org.triple_brain.module.model.graph.schema.SchemaOperator;
import org.triple_brain.module.model.graph.vertex.VertexInSubGraphOperator;
import org.triple_brain.module.neo4j_graph_manipulator.graph.Neo4jFriendlyResource;
import org.triple_brain.module.neo4j_graph_manipulator.graph.Relationships;
import org.triple_brain.module.neo4j_graph_manipulator.graph.graph.edge.Neo4jEdgeFactory;
import org.triple_brain.module.neo4j_graph_manipulator.graph.graph.schema.SchemaFactory;
import org.triple_brain.module.neo4j_graph_manipulator.graph.graph.vertex.Neo4jVertexFactory;

import javax.inject.Inject;
import java.net.URI;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

public class Neo4jWholeGraph implements WholeGraph {

    @Inject
    protected QueryEngine queryEngine;

    @Inject
    protected Neo4jVertexFactory neo4jVertexFactory;

    @Inject
    protected Neo4jEdgeFactory neo4jEdgeFactory;

    @Inject
    protected SchemaFactory schemaFactory;

    @Override
    public Iterator<VertexInSubGraphOperator> getAllVertices() {
        return new Iterator<VertexInSubGraphOperator>() {
            QueryResult<Map<String, Object>> result = queryEngine.query(
                    "START n=node:node_auto_index('" +
                            Neo4jFriendlyResource.props.type + ":" + GraphElementType.vertex +
                            "') " +
                            "RETURN n",
                    Collections.emptyMap()
            );
            Iterator<Map<String, Object>> iterator = result.iterator();
            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public VertexInSubGraphOperator next() {
                Node node = (Node) iterator.next().get("n");
                return neo4jVertexFactory.createOrLoadUsingNode(
                        node
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
            QueryResult<Map<String,Object>> result = queryEngine.query(
                    "START n=node:node_auto_index('" +
                            Neo4jFriendlyResource.props.type + ":" + GraphElementType.edge +
                            "') " +
                            "RETURN n",
                    Collections.EMPTY_MAP
            );
            Iterator<Map<String, Object>> iterator =result.iterator();
            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public EdgeOperator next() {
                return neo4jEdgeFactory.createOrLoadWithNode(
                        (Node) iterator.next().get("n")
                );
            }

            @Override
            public void remove() {
                //To change body of implemented methods use File | Settings | File Templates.
            }
        };
    }

    @Override
    public Iterator<SchemaOperator> getAllSchemas() {
        return new Iterator<SchemaOperator>() {
            QueryResult<Map<String, Object>> result = queryEngine.query(
                    "START n=node:node_auto_index('" +
                            Neo4jFriendlyResource.props.type + ":" + GraphElementType.schema +
                            "') " +
                            "RETURN n." + Neo4jFriendlyResource.props.uri + " as uri",
                    Collections.emptyMap()
            );
            Iterator<Map<String, Object>> iterator = result.iterator();
            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public SchemaOperator next() {
                URI uri = URI.create(
                        iterator.next().get("uri").toString()
                );
                return schemaFactory.withUri(
                        uri
                );
            }

            @Override
            public void remove() {
                //To change body of implemented methods use File | Settings | File Templates.
            }
        };
    }
}
