package org.triple_brain.module.neo4j_graph_manipulator.graph;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.triple_brain.module.model.BeforeAfterEachRestCall;

import javax.inject.Inject;

/*
* Copyright Mozilla Public License 1.1
*/
public class Neo4JBeforeAfterEachRestCall implements BeforeAfterEachRestCall {

    @Inject
    GraphDatabaseService graphDb;

    @Override
    public Object before() {
        return graphDb.beginTx();
    }

    @Override
    public void after(Object transactionAsObject) {
        Transaction transaction = (Transaction) transactionAsObject;
        transaction.success();
        transaction.finish();
    }
}
