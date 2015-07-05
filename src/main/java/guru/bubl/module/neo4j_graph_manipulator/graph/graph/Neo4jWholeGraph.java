/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.graph;

import guru.bubl.module.model.WholeGraph;
import guru.bubl.module.model.graph.GraphElementOperator;
import guru.bubl.module.model.graph.GraphElementOperatorFactory;
import guru.bubl.module.model.graph.GraphElementType;
import guru.bubl.module.model.graph.edge.EdgeOperator;
import guru.bubl.module.model.graph.schema.SchemaOperator;
import guru.bubl.module.model.graph.vertex.VertexInSubGraphOperator;
import guru.bubl.module.neo4j_graph_manipulator.graph.Neo4jFriendlyResource;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.edge.Neo4jEdgeFactory;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.schema.SchemaFactory;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.vertex.Neo4jVertexFactory;
import org.apache.commons.lang.StringUtils;
import org.neo4j.graphdb.Node;
import org.neo4j.rest.graphdb.query.QueryEngine;
import org.neo4j.rest.graphdb.util.QueryResult;

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

    @Inject
    protected GraphElementOperatorFactory graphElementFactory;

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

    @Override
    public Iterator<GraphElementOperator> getAllGraphElements(){
        return new Iterator<GraphElementOperator>() {
            QueryResult<Map<String, Object>> result = queryEngine.query(
                    "START n=node:node_auto_index('" +
                            "( " + Neo4jFriendlyResource.props.type + ":" +
                            StringUtils.join(GraphElementType.names(), " OR type:") +
                            ") " +
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
            public GraphElementOperator next() {
                URI uri = URI.create(
                        iterator.next().get("uri").toString()
                );
                return graphElementFactory.withUri(
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
