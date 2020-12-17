/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import guru.bubl.module.model.FriendlyResourceFactory;
import guru.bubl.module.model.admin.WholeGraphAdmin;
import guru.bubl.module.model.center_graph_element.CenterGraphElementOperator;
import guru.bubl.module.model.center_graph_element.CenterGraphElementOperatorFactory;
import guru.bubl.module.model.center_graph_element.CenterGraphElementsOperatorFactory;
import guru.bubl.module.model.center_graph_element.CenteredGraphElementsOperator;
import guru.bubl.module.model.graph.FriendlyResourceOperator;
import guru.bubl.module.model.graph.GraphFactory;
import guru.bubl.module.model.graph.tree_copier.TreeCopier;
import guru.bubl.module.model.graph.edge.EdgeOperator;
import guru.bubl.module.model.graph.edge.EdgeOperatorFactory;
import guru.bubl.module.model.graph.fork.ForkOperator;
import guru.bubl.module.model.graph.fork.ForkOperatorFactory;
import guru.bubl.module.model.graph.fork.NbNeighbors;
import guru.bubl.module.model.graph.fork.NbNeighborsOperatorFactory;
import guru.bubl.module.model.graph.graph_element.ForkCollectionOperator;
import guru.bubl.module.model.graph.graph_element.ForkCollectionOperatorFactory;
import guru.bubl.module.model.graph.graph_element.GraphElementOperator;
import guru.bubl.module.model.graph.graph_element.GraphElementOperatorFactory;
import guru.bubl.module.model.graph.group_relation.GroupRelationFactory;
import guru.bubl.module.model.graph.group_relation.GroupRelationOperator;
import guru.bubl.module.model.graph.pattern.PatternUser;
import guru.bubl.module.model.graph.pattern.PatternUserFactory;
import guru.bubl.module.model.graph.relation.RelationFactory;
import guru.bubl.module.model.graph.relation.RelationOperator;
import guru.bubl.module.model.graph.tag.TagFactory;
import guru.bubl.module.model.graph.tag.TagOperator;
import guru.bubl.module.model.graph.vertex.VertexFactory;
import guru.bubl.module.model.graph.vertex.VertexOperator;
import guru.bubl.module.model.notification.NotificationOperator;
import guru.bubl.module.neo4j_graph_manipulator.graph.admin.WholeGraphAdminNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.center_graph_element.CenterGraphElementOperatorNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.center_graph_element.CenterGraphElementsOperatorNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.GraphFactoryNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.UserGraphFactoryNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.EdgeOperatorNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.subgraph.SubGraphExtractorFactoryNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.fork.ForkOperatorNeo4J;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.fork.NbNeighborsOperatorNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.graph_element.ForkCollectionOperatorNeo4J;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.graph_element.GraphElementFactoryNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.graph_element.GraphElementOperatorNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.graph_element.GraphElementSpecialOperatorFactory;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.group_relation.GroupRelationFactoryNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.group_relation.GroupRelationOperatorNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.notification.NotificationOperatorNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.pattern.PatternUserNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.relation.RelationFactoryNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.relation.RelationOperatorNeo4J;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.tag.TagOperatorNeo4J;
import guru.bubl.module.model.graph.tree_copier.TreeCopierFactory;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.tree_copier.TreeCopierNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.vertex.VertexFactoryNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.vertex.VertexOperatorNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.image.ImageFactoryNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.search.GraphSearchModuleNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.tag.TagFactoryNeo4J;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;

import javax.inject.Singleton;

public class Neo4jModule extends AbstractModule {

    public static final Integer BOLT_PORT_FOR_PROD = 7687;
    public static final Integer BOLT_PORT_FOR_TESTS = 8022;
    public static final String DB_PATH_FOR_TESTS = "/tmp/triple_brain/neo4j/db";
    public static final String NEO4J_PASSWORD_FOR_TESTS = "proute";

    private String dbUser, dbPassword;
    private Integer boltPort;

    public static Neo4jModule usingEmbedded() {
        return new Neo4jModule("", "", -1);
    }

    public static Neo4jModule withUserPasswordAndPort(String dbUser, String dbPassword, Integer boltPort) {
        return new Neo4jModule(dbUser, dbPassword, boltPort);
    }

    protected Neo4jModule(String dbUser, String dbPassword, Integer boltPort) {
        this.dbUser = dbUser;
        this.dbPassword = dbPassword;
        this.boltPort = boltPort;
    }

    @Override
    protected void configure() {
        if (boltPort > -1) {
            Driver driver = GraphDatabase.driver(
                    "bolt://localhost:" + this.boltPort,
                    AuthTokens.basic(this.dbUser, this.dbPassword)
            );
            bind(Driver.class).toInstance(
                    driver
            );
        }

        install(new GraphSearchModuleNeo4j());

        bind(WholeGraphAdmin.class).to(WholeGraphAdminNeo4j.class);
        FactoryModuleBuilder factoryModuleBuilder = new FactoryModuleBuilder();

        install(factoryModuleBuilder
                .implement(CenteredGraphElementsOperator.class, CenterGraphElementsOperatorNeo4j.class)
                .build(CenterGraphElementsOperatorFactory.class));

        install(factoryModuleBuilder
                .implement(CenterGraphElementOperator.class, CenterGraphElementOperatorNeo4j.class)
                .build(CenterGraphElementOperatorFactory.class));

        install(factoryModuleBuilder
                .implement(PatternUser.class, PatternUserNeo4j.class)
                .build(PatternUserFactory.class));

        install(factoryModuleBuilder
                .build(RelationFactoryNeo4j.class));

        install(factoryModuleBuilder
                .build(UserGraphFactoryNeo4j.class));

        install(factoryModuleBuilder
                .implement(VertexOperator.class, VertexOperatorNeo4j.class)
                .build(VertexFactory.class));

        install(factoryModuleBuilder
                .build(VertexFactoryNeo4j.class));

        install(factoryModuleBuilder
                .build(SubGraphExtractorFactoryNeo4j.class));

        install(factoryModuleBuilder
                .implement(RelationOperator.class, RelationOperatorNeo4J.class)
                .build(RelationFactory.class));

        install(factoryModuleBuilder
                .implement(GraphElementOperator.class, GraphElementOperatorNeo4j.class)
                .build(GraphElementOperatorFactory.class)
        );

        install(factoryModuleBuilder
                .implement(ForkCollectionOperator.class, ForkCollectionOperatorNeo4J.class)
                .build(ForkCollectionOperatorFactory.class));

        install(factoryModuleBuilder
                .implement(ForkOperator.class, ForkOperatorNeo4J.class)
                .build(ForkOperatorFactory.class)
        );

        install(factoryModuleBuilder
                .implement(NbNeighbors.class, NbNeighborsOperatorNeo4j.class)
                .build(NbNeighborsOperatorFactory.class)
        );

        install(factoryModuleBuilder
                .implement(EdgeOperator.class, EdgeOperatorNeo4j.class)
                .build(EdgeOperatorFactory.class)
        );

        install(factoryModuleBuilder
                .build(GraphElementFactoryNeo4j.class));

        install(factoryModuleBuilder
                .implement(FriendlyResourceOperator.class, FriendlyResourceNeo4j.class)
                .build(FriendlyResourceFactory.class)
        );
        install(factoryModuleBuilder
                .build(FriendlyResourceFactoryNeo4j.class)
        );
        install(factoryModuleBuilder
                .build(ImageFactoryNeo4j.class)
        );

        install(factoryModuleBuilder
                .implement(TagOperator.class, TagOperatorNeo4J.class)
                .build(TagFactory.class)
        );
        install(factoryModuleBuilder
                .build(TagFactoryNeo4J.class)
        );
        install(factoryModuleBuilder
                .implement(GroupRelationOperator.class, GroupRelationOperatorNeo4j.class)
                .build(GroupRelationFactory.class)
        );
        install(factoryModuleBuilder
                .build(GroupRelationFactoryNeo4j.class));

        install(factoryModuleBuilder
                .implement(TreeCopier.class, TreeCopierNeo4j.class)
                .build(TreeCopierFactory.class));

        bind(GraphFactory.class).to(GraphFactoryNeo4j.class).in(Singleton.class);
        bind(GraphElementSpecialOperatorFactory.class);
        bind(NotificationOperator.class).to(NotificationOperatorNeo4j.class).in(Singleton.class);
    }

}
