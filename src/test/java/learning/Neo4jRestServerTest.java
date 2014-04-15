package learning;

import org.junit.Ignore;
import org.junit.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.rest.graphdb.RestAPI;
import org.neo4j.rest.graphdb.batch.BatchCallback;
import org.neo4j.rest.graphdb.util.QueryResult;

import java.util.*;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.triple_brain.module.neo4j_graph_manipulator.graph.Neo4jRestApiUtils.wrap;

/*
 * Copyright Mozilla Public License 1.1
 */
public class Neo4jRestServerTest extends  Neo4jServerTestGeneric{

    public enum Relationships implements RelationshipType {
        test
    }

    @Test
    public void can_use_rest_server() {
        removeWholeGraph();
        assertThat(
                getNumberOfNodes(), is(0)
        );
        addNode();
        assertThat(
                getNumberOfNodes(), is(1)
        );
    }

    @Test
    public void can_use_global_private_api_in_batch_operations() {
        TestBatchResult result = new TestBatchResult();
        assertTrue(null == result.n1);
        assertTrue(null == result.n2);
        assertTrue(null == result.r1);
        result = restApi.executeBatch(new BatchCallback<TestBatchResult>() {
            @Override
            public TestBatchResult recordBatch(RestAPI batchRestApi) {
                TestBatchResult r = new TestBatchResult();
                r.n1 = addNode();
                r.n2 = addNode();
                r.r1 = createRelationShip(
                        r.n1,
                        r.n2
                );
                return r;
            }
        });
        assertFalse(null == result.n1);
        assertFalse(null == result.n2);
        assertFalse(null == result.r1);
    }

    @Test
    @Ignore("using embedded for now and it makes it fail")
    public void making_empty_transactions_doesnt_make_a_rest_call() {
        Transaction tx = restApi.beginTx();
        tx.success();
        tx.finish();
        //look in console :)
    }

    @Test
    public void can_have_a_transaction_started_before_batch_operation() {
        restApi.executeBatch(new BatchCallback<TestBatchResult>() {
            @Override
            public TestBatchResult recordBatch(RestAPI batchRestApi) {
                TestBatchResult r = new TestBatchResult();
                r.n1 = addNode();
                r.n2 = addNode();
                r.r1 = createRelationShip(
                        r.n1,
                        r.n2
                );
                return r;
            }
        });
    }

    private Relationship createRelationShip(Node n1, Node n2) {
        return n1.createRelationshipTo(
                n2,
                Relationships.test
        );
    }

    private Integer getNumberOfNodes() {
        QueryResult<Map<String, Object>> result = queryEngine.query(
                "start n=node(*) return count(n) as total",
                Collections.EMPTY_MAP
        );
        Iterator<Map<String, Object>> iterator = result.iterator();
        Long numberOfNodes = iterator.hasNext() ?
                (Long) iterator.next().get("total") :
                new Long(0);
        return Integer.valueOf(numberOfNodes.toString());
    }

    private Node addNode() {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("id", UUID.randomUUID().toString());
        props.put("name", "firstNode");
        QueryResult<Map<String, Node>> result = queryEngine.query(
                "CREATE (n {props}) RETURN n",
                wrap(
                        props
                )
        );
        return result.iterator().next().get("n");
    }
    public void removeWholeGraph() {
        restApi.query(
                "START n = node(*), r=relationship(*) DELETE n, r;",
                Collections.EMPTY_MAP
        );
    }
}
