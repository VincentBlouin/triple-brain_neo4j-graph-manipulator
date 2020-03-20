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
import guru.bubl.module.model.graph.ShareLevel;
import guru.bubl.module.model.graph.edge.EdgeOperator;
import guru.bubl.module.model.graph.tag.Tag;
import guru.bubl.module.model.graph.tag.TagOperator;
import guru.bubl.module.model.graph.tag.TagPojo;
import guru.bubl.module.model.graph.vertex.NbNeighbors;
import guru.bubl.module.model.graph.vertex.VertexOperator;
import guru.bubl.module.model.search.GraphIndexer;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.tag.TagOperatorNeo4J;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;

import static org.neo4j.driver.v1.Values.parameters;

public class WholeGraphAdminNeo4j implements WholeGraphAdmin {

    @Inject
    protected Driver driver;

    @Inject
    protected WholeGraph wholeGraph;

    @Inject
    protected GraphIndexer graphIndexer;

    @Override
    public void refreshNbNeighborsToAllTags() {
        wholeGraph.getAllTags().forEach(
                this::refreshNbNeighborsToTag
        );
    }

    @Override
    public void removeMetasHavingZeroReferences() {
        wholeGraph.getAllTags().forEach(
                this::removeTagIfNoReference
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
        for (Tag tag : wholeGraph.getAllTags()) {
            graphIndexer.indexMeta(
                    new TagPojo(
                            new GraphElementPojo(
                                    new FriendlyResourcePojo(
                                            tag.uri()
                                    )
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
        for (Tag tag : wholeGraph.getAllTagsOfUser(user)) {
            graphIndexer.indexMeta(
                    new TagPojo(
                            new GraphElementPojo(
                                    new FriendlyResourcePojo(
                                            tag.uri()
                                    )
                            )
                    )
            );
        }
    }

    @Override
    public WholeGraph getWholeGraph() {
        return wholeGraph;
    }

    private void refreshNbNeighborsToTag(TagOperator tag) {
        TagOperatorNeo4J tagOperatorNeo4J = (TagOperatorNeo4J) tag;
        try (Session session = driver.session()) {
            StatementResult rs = session.run(
                    tagOperatorNeo4J.queryPrefix() + "OPTIONAL MATCH (n)<-[:IDENTIFIED_TO]-(ge) " +
                            "return ge.shareLevel as shareLevel",
                    parameters(
                            "uri", tag.uri().toString()
                    )
            );
            Integer nbPrivate = 0;
            Integer nbFriend = 0;
            Integer nbPublic = 0;
            while (rs.hasNext()) {
                Record record = rs.next();
                if (record.get("shareLevel").asObject() != null) {
                    ShareLevel graphElementShareLevel = ShareLevel.get(record.get("shareLevel").asInt());
                    switch (graphElementShareLevel) {
                        case PRIVATE:
                            nbPrivate++;
                            break;
                        case FRIENDS:
                            nbFriend++;
                            break;
                        default:
                            nbPublic++;
                    }
                }
            }
            NbNeighbors nbNeighbors = tagOperatorNeo4J.getNbNeighbors();
            nbNeighbors.setPrivate(nbPrivate);
            nbNeighbors.setFriend(nbFriend);
            nbNeighbors.setPublic(nbPublic);
        }
    }

    private void removeTagIfNoReference(TagOperator tag) {
        NbNeighbors nbNeighbors = tag.getNbNeighbors();
        if (0 == nbNeighbors.getTotal()) {
            tag.remove();
        }
    }

}
