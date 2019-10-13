/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.admin;

import com.google.inject.Inject;
import guru.bubl.module.model.User;
import guru.bubl.module.model.WholeGraph;
import guru.bubl.module.model.admin.WholeGraphAdmin;
import guru.bubl.module.model.graph.FriendlyResourcePojo;
import guru.bubl.module.model.graph.GraphElementPojo;
import guru.bubl.module.model.graph.edge.EdgeOperator;
import guru.bubl.module.model.graph.identification.IdentificationOperator;
import guru.bubl.module.model.graph.identification.Identifier;
import guru.bubl.module.model.graph.identification.IdentifierPojo;
import guru.bubl.module.model.graph.schema.SchemaOperator;
import guru.bubl.module.model.graph.schema.SchemaPojo;
import guru.bubl.module.model.graph.vertex.VertexOperator;
import guru.bubl.module.model.search.GraphIndexer;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.identification.IdentificationNeo4j;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Session;

import static org.neo4j.driver.v1.Values.parameters;

public class WholeGraphAdminNeo4j implements WholeGraphAdmin {

    @Inject
    protected Driver driver;

    @Inject
    protected WholeGraph wholeGraph;

    @Inject
    protected GraphIndexer graphIndexer;

    @Override
    public void refreshNumberOfReferencesToAllIdentifications() {
        wholeGraph.getAllTags().forEach(
                this::refreshNumberOfReferencesToIdentification
        );
    }

    @Override
    public void removeMetasHavingZeroReferences() {
        wholeGraph.getAllTags().forEach(
                this::removeMetaIfNoReference
        );
    }

    @Override
    public void reindexAll() {
        for (VertexOperator vertex : wholeGraph.getAllVertices()) {
            graphIndexer.indexVertex(vertex);
        }
        for (EdgeOperator edge : wholeGraph.getAllEdges()) {
            graphIndexer.indexRelation(edge);
        }
        for (SchemaOperator schemaOperator : wholeGraph.getAllSchemas()) {
            SchemaPojo schemaPojo = new SchemaPojo(schemaOperator);
            graphIndexer.indexSchema(schemaPojo);
            for (GraphElementPojo property : schemaPojo.getProperties().values()) {
                graphIndexer.indexProperty(property, schemaPojo);
            }
        }
        for (Identifier identifier : wholeGraph.getAllTags()) {
            graphIndexer.indexMeta(
                    new IdentifierPojo(
                            new FriendlyResourcePojo(
                                    identifier.uri()
                            )
                    )
            );
        }
    }

    @Override
    public void reindexAllForUser(User user) {
        for (VertexOperator vertex : wholeGraph.getAllVerticesOfUser(user)) {
            graphIndexer.indexVertex(vertex);
        }
        for (EdgeOperator edge : wholeGraph.getAllEdgesOfUser(user)) {
            graphIndexer.indexRelation(edge);
        }
        for (Identifier identifier : wholeGraph.getAllTagsOfUser(user)) {
            graphIndexer.indexMeta(
                    new IdentifierPojo(
                            new FriendlyResourcePojo(
                                    identifier.uri()
                            )
                    )
            );
        }
    }

    @Override
    public WholeGraph getWholeGraph() {
        return wholeGraph;
    }

    private void refreshNumberOfReferencesToIdentification(IdentificationOperator identification) {
        IdentificationNeo4j neo4jIdentification = (IdentificationNeo4j) identification;
        try (Session session = driver.session()) {
            session.run(
                    neo4jIdentification.queryPrefix() + "OPTIONAL MATCH (n)<-[r]-() " +
                            "WITH n, count(r) as nbReferences " +
                            "SET n.nb_references=nbReferences",
                    parameters(
                            "uri", identification.uri().toString()
                    )
            );
        }
    }

    private void removeMetaIfNoReference(IdentificationOperator identification) {
        if (0 == identification.getNbReferences()) {
            identification.remove();
        }
    }

}
