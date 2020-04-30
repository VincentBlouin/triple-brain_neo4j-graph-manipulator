/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.admin;

import com.google.inject.Inject;
import guru.bubl.module.model.admin.WholeGraphAdmin;
import guru.bubl.module.model.graph.graph_element.GraphElement;
import guru.bubl.module.model.graph.graph_element.GraphElementType;
import guru.bubl.module.model.graph.ShareLevel;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Session;

import java.util.Set;

import static org.neo4j.driver.v1.Values.parameters;

public class WholeGraphAdminNeo4j implements WholeGraphAdmin {

    @Inject
    protected Driver driver;

    @Override
    public void refreshNbNeighbors() {
        refreshNbNeighborsOfForks(GraphElementType.Vertex);
        refreshNbNeighborsOfForks(GraphElementType.GroupRelation);
    }

    private void refreshNbNeighborsOfForks(GraphElementType graphElementType) {
        refreshNbNeighborsToForksInShareLevels(
                graphElementType,
                ShareLevel.shareLevelsToSet(ShareLevel.PRIVATE),
                "nb_private_neighbors"
        );
        refreshNbNeighborsToForksInShareLevels(
                graphElementType,
                ShareLevel.shareLevelsToSet(ShareLevel.FRIENDS),
                "nb_friend_neighbors"
        );
        refreshNbNeighborsToForksInShareLevels(
                graphElementType,
                ShareLevel.publicShareLevels,
                "nb_public_neighbors"
        );
    }

    private void refreshNbNeighborsToForksInShareLevels(GraphElementType graphElementType, Set<ShareLevel> shareLevels, String nbNeighborsName) {
        try (Session session = driver.session()) {
            session.run(
                    String.format(
                            "MATCH(n:%s) OPTIONAL MATCH (n)<-[:SOURCE|DESTINATION]-(e:Edge)," +
                                    "(e)-[:SOURCE|DESTINATION]->(o) WHERE o.shareLevel in $shareLevels " +
                                    "WITH n,o " +
                                    "OPTIONAL MATCH (n)-[:SOURCE]->(closeVertex:Vertex) WHERE closeVertex.shareLevel in $shareLevels " +
                                    "WITH n,o, closeVertex " +
                                    "OPTIONAL MATCH (n)<-[:SOURCE|DESTINATION]-(groupRelation:GroupRelation) WHERE groupRelation.shareLevel in $shareLevels " +
                                    "WITH n, COUNT(o) + COUNT(DISTINCT closeVertex) + COUNT(groupRelation) as nbNeighbors " +
                                    "SET n.%s=nbNeighbors",
                            graphElementType.name(),
                            nbNeighborsName
                    ),
                    parameters(
                            "shareLevels", ShareLevel.shareLevelsToIntegers(shareLevels)
                    )
            );
        }
    }


    @Override
    public void refreshNbNeighborsOfTags() {
        refreshNbNeighborsToTagsInShareLevels(
                ShareLevel.shareLevelsToSet(ShareLevel.PRIVATE), "nb_private_neighbors"
        );
        refreshNbNeighborsToTagsInShareLevels(
                ShareLevel.shareLevelsToSet(ShareLevel.FRIENDS), "nb_friend_neighbors"
        );
        refreshNbNeighborsToTagsInShareLevels(
                ShareLevel.publicShareLevels, "nb_public_neighbors"
        );
    }

    private void refreshNbNeighborsToTagsInShareLevels(Set<ShareLevel> shareLevels, String nbNeighborsName) {
        try (Session session = driver.session()) {
            session.run(
                    String.format(
                            "MATCH(n:Meta) OPTIONAL MATCH (n)<-[:IDENTIFIED_TO]-(ge) " +
                                    "WHERE ge.shareLevel in $shareLevels " +
                                    "WITH n, count(ge) as nbNeighbors " +
                                    "SET n.%s=nbNeighbors",
                            nbNeighborsName
                    ),
                    parameters(
                            "shareLevels", ShareLevel.shareLevelsToIntegers(shareLevels)
                    )
            );
        }
    }

    @Override
    public void reindexAll() {
        indexForksOfType(GraphElementType.Vertex);
        indexForksOfType(GraphElementType.GroupRelation);
        indexTags();
        indexEdges();
    }

    private void indexEdges() {
        try (Session session = driver.session()) {
            session.run(
                    "MATCH(n:Edge) MATCH (n)-[:SOURCE|DESTINATION]->(v) " +
                            "WITH n, v, (v.nb_private_neighbors + v.nb_friend_neighbors + v.nb_public_neighbors) as nbNeighbors " +
                            "ORDER BY nbNeighbors DESC " +
                            "WITH reduce(a=\"\", os in collect(v) |  a + \"{{\" + substring(os.label, 0, 20)) as concatContext, n " +
                            "WITH n, substring(concatContext, 2, 51) as context " +
                            "SET n.private_context = context, n.friend_context = context, n.public_context = context"
            );
        }
    }

    private void indexTags() {
        indexTagsInShareLevel(
                ShareLevel.allShareLevels,
                "private_context"
        );
        indexTagsInShareLevel(
                ShareLevel.friendShareLevels,
                "friend_context"
        );
        indexTagsInShareLevel(
                ShareLevel.publicShareLevels,
                "public_context"
        );
    }

    private void indexTagsInShareLevel(Set<ShareLevel> shareLevels, String contextName) {
        String nbNeighborsTemplate = buildNbNeighborsTemplateForShareLevels(shareLevels);
        String nbNeighborsSum = nbNeighborsTemplate.replaceAll("%s", "allGraphElements");
        try (Session session = driver.session()) {
            session.run(
                    String.format("MATCH(n:Meta) OPTIONAL MATCH (n)<-[:IDENTIFIED_TO]-(ov:Vertex) WHERE  ov.label <>'' AND ov.shareLevel in $shareLevels " +
                                    "WITH n,ov OPTIONAL MATCH (n)<-[:IDENTIFIED_TO]-(e:Edge) WHERE e.shareLevel in $shareLevels " +
                                    "WITH n,COLLECT(ov) as ovList,e " +
                                    "OPTIONAL MATCH (e)-[:DESTINATION]->(v) WHERE v.label <>'' " +
                                    "WITH n, ovList + collect(v) as allVertices " +
                                    "OPTIONAL MATCH (n)<-[:IDENTIFIED_TO]->(gr:GroupRelation) WHERE gr.label <>'' AND gr.shareLevel in $shareLevels " +
                                    "WITH n, allVertices + collect(gr) as allGraphElementsList " +
                                    "UNWIND  allGraphElementsList as allGraphElements " +
                                    "WITH n, allGraphElements, (%s) as nbNeighbors " +
                                    "ORDER BY nbNeighbors DESC " +
                                    "WITH reduce(a=\"\", os in collect(allGraphElements) |  a + \"{{\" + substring(os.label, 0, 20)) as context, n " +
                                    "SET n.%s = CASE n.comment WHEN \"\" THEN substring(context, 2, 110) ELSE n.comment END",
                            nbNeighborsSum,
                            contextName
                    ),
                    parameters(
                            "shareLevels", ShareLevel.shareLevelsToIntegers(shareLevels)
                    )
            );
        }
    }

    private void indexForksOfType(GraphElementType graphElementType) {
        indexForksOfTypeInShareLevels(
                graphElementType,
                ShareLevel.allShareLevels,
                "private_context"
        );
        indexForksOfTypeInShareLevels(
                graphElementType,
                ShareLevel.friendShareLevels,
                "friend_context"
        );
        indexForksOfTypeInShareLevels(
                graphElementType,
                ShareLevel.publicShareLevels,
                "public_context"
        );
    }

    private void indexForksOfTypeInShareLevels(GraphElementType graphElementType, Set<ShareLevel> shareLevels, String contextName) {
        String nbNeighborsTemplate = buildNbNeighborsTemplateForShareLevels(shareLevels);
        String verticesNbNeighborsSum = nbNeighborsTemplate.replaceAll("%s", "o");
        String groupRelationsNbNeighborsSum = nbNeighborsTemplate.replaceAll("%s", "gr");
        String closeVerticesNbNeighborsSum = nbNeighborsTemplate.replaceAll("%s", "closeVertex");
        try (Session session = driver.session()) {
            session.run(
                    String.format("MATCH(n:%s) OPTIONAL MATCH (n)<-[:SOURCE|DESTINATION]->(e:Edge)," +
                                    "(e)<-[:SOURCE|DESTINATION]->(o) WHERE o.label <> '' AND o.shareLevel in $shareLevels " +
                                    "WITH n,o, COLLECT({label:o.label, nbNeighbors:%s}) as vertices " +
                                    "OPTIONAL MATCH (n)<-[:SOURCE|DESTINATION]->(gr:GroupRelation) WHERE gr.label <> '' AND gr.shareLevel in $shareLevels " +
                                    "WITH n, vertices, COLLECT({label:gr.label, nbNeighbors: %s}) as groupRelations " +
                                    "OPTIONAL MATCH (n)<-[:SOURCE|DESTINATION]->(closeVertex:Vertex) WHERE closeVertex.label <> '' AND closeVertex.shareLevel in $shareLevels " +
                                    "WITH n, vertices, groupRelations, COLLECT({label:closeVertex.label, nbNeighbors: %s}) as closeVertices " +
                                    "WITH n, vertices + groupRelations + closeVertices as allGraphElements " +
                                    "UNWIND allGraphElements as o " +
                                    "WITH n,o.label as label, o.nbNeighbors as nbNeighbors " +
                                    "ORDER BY nbNeighbors DESC " +
                                    "WITH reduce(a=\"\", oneLabel in collect(DISTINCT label) |  a + \"{{\" + substring(oneLabel, 0, 20)) as context, n " +
                                    "SET n.%s = substring(context, 2, 110)",
                            graphElementType.name(),
                            verticesNbNeighborsSum,
                            groupRelationsNbNeighborsSum,
                            closeVerticesNbNeighborsSum,
                            contextName
                    ),
                    parameters(
                            "shareLevels", ShareLevel.shareLevelsToIntegers(shareLevels)
                    )
            );
        }
    }

    private String buildNbNeighborsTemplateForShareLevels(Set<ShareLevel> shareLevels) {
        String nbNeighborsTemplate = "%s.nb_public_neighbors";
        if (shareLevels.contains(ShareLevel.FRIENDS)) {
            nbNeighborsTemplate += " + %s.nb_friend_neighbors ";
        }
        if (shareLevels.contains(ShareLevel.PRIVATE)) {
            nbNeighborsTemplate += " + %s.nb_private_neighbors ";
        }
        return nbNeighborsTemplate;
    }
}
