/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph;

import guru.bubl.module.model.GraphTransaction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;

import javax.inject.Inject;

public class Neo4jGraphTransaction implements GraphTransaction {

    @Inject
    GraphDatabaseService service;

    @Override
    public Object before()
    {
        return service.beginTx();
    }

    @Override
    public void after(Object transactionAsObject) {
        Transaction transaction = (Transaction) transactionAsObject;
        transaction.success();
        transaction.close();
    }
}
