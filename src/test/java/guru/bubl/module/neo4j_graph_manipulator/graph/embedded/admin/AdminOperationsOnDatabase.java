/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.embedded.admin;

import org.junit.BeforeClass;

import java.sql.Connection;
import java.sql.DriverManager;

public class AdminOperationsOnDatabase {

    protected static Connection connection;

    @BeforeClass
    public static void before() throws Exception{
        Class.forName("org.neo4j.jdbc.Driver");
        connection = DriverManager.getConnection(
                "jdbc:neo4j://localhost:9594/db/data?debug=true"

        );
    }
}
