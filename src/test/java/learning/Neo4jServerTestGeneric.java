/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package learning;

public class Neo4jServerTestGeneric {

//    protected static Injector injector;
//
//    @Inject
//    static protected GraphDatabaseService graphDatabaseService;
//
//    protected Transaction transaction;
//
//    @BeforeClass
//    public static void realBeforeClass() {
//        injector = Guice.createInjector(
//                Neo4jModule.forTestingUsingEmbedded()
//        );
//        graphDatabaseService = injector.getInstance(GraphDatabaseService.class);
//    }
//
//    @Before
//    public void before() {
//        injector.injectMembers(this);
//        transaction = graphDatabaseService.beginTx();
//        removeEverything();
//    }
//
//
//    @After
//    public void after(){
//        transaction.close();
//    }
//
//    @AfterClass
//    public static void afterClass() {
//        graphDatabaseService.shutdown();
//        Neo4jModule.clearDb();
//    }
//
//    private void removeEverything() {
//        NoEx.wrap(() -> {
//                    String query = "START n = node(*) OPTIONAL MATCH n-[r]-() DELETE n, r;";
//                    Statement stmt = connection.createStatement();
//                    return stmt.executeQuery(
//                            query
//                    );
//                }
//        );
//    }

}
