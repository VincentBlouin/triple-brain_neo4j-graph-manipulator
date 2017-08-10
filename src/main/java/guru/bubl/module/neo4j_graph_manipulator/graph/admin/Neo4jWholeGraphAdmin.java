/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.admin;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import guru.bubl.module.common_utils.NoExRun;
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
import guru.bubl.module.model.graph.vertex.Vertex;
import guru.bubl.module.model.graph.vertex.VertexOperator;
import guru.bubl.module.model.search.GraphIndexer;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.identification.Neo4jIdentification;

import java.sql.Connection;

public class Neo4jWholeGraphAdmin implements WholeGraphAdmin {

    @Inject
    protected Connection connection;

    @Inject
    protected WholeGraph wholeGraph;

    @Inject
    protected GraphIndexer graphIndexer;

    @Override
    public void refreshNumberOfReferencesToAllIdentifications() {
        wholeGraph.getAllIdentifications().forEach(
                this::refreshNumberOfReferencesToIdentification
        );
    }

    @Override
    public void removeMetasHavingZeroReferences() {
        wholeGraph.getAllIdentifications().forEach(
                this::removeMetaIfNoReference
        );
    }

    @Override
    public void reindexAll(){
        for(VertexOperator vertex : wholeGraph.getAllVertices()){
            graphIndexer.indexVertex(vertex);
        }
        for(EdgeOperator edge : wholeGraph.getAllEdges()){
            graphIndexer.indexRelation(edge);
        }
        for(SchemaOperator schemaOperator : wholeGraph.getAllSchemas()){
            SchemaPojo schemaPojo = new SchemaPojo(schemaOperator);
            graphIndexer.indexSchema(schemaPojo);
            for(GraphElementPojo property : schemaPojo.getProperties().values()){
                graphIndexer.indexProperty(property, schemaPojo);
            }
        }
        for(Identifier identifier: wholeGraph.getAllIdentifications()){
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
        Neo4jIdentification neo4jIdentification = (Neo4jIdentification) identification;
        String query = String.format(
                "%s OPTIONAL MATCH n<-[r]-() " +
                        "WITH n, count(r) as nbReferences " +
                        "SET n.%s=nbReferences",
                neo4jIdentification.queryPrefix(),
                Neo4jIdentification.props.nb_references
        );
        NoExRun.wrap(() -> connection.createStatement().execute(query)).get();
    }
    private void removeMetaIfNoReference(IdentificationOperator identification) {
        if(0 == identification.getNbReferences()){
            identification.remove();
        }
    }

}
