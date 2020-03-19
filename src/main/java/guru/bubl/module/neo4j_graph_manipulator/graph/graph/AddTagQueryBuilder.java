/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.graph;

import guru.bubl.module.model.graph.ShareLevel;
import guru.bubl.module.model.graph.tag.TagPojo;
import guru.bubl.module.neo4j_graph_manipulator.graph.FriendlyResourceNeo4j;

public class AddTagQueryBuilder {

    private String queryPrefix;
    private ShareLevel sourceShareLevel;

    public static AddTagQueryBuilder usingIdentificationForGraphElement(
            String queryPrefix,
            ShareLevel sourceShareLevel
    ) {
        return new AddTagQueryBuilder(
                queryPrefix,
                sourceShareLevel
        );
    }

    protected AddTagQueryBuilder(
            String queryPrefix,
            ShareLevel sourceShareLevel
    ) {
        this.queryPrefix = queryPrefix;
        this.sourceShareLevel = sourceShareLevel;
    }

    public String build() {
        String neighborsPropertyName = sourceShareLevel.getNbNeighborsPropertyName();
        return String.format(
                "%sMERGE (f:Resource:GraphElement:Meta{external_uri:$external_uri, owner:$owner}) " +
                        "ON CREATE SET f.uri=$metaUri, " +
                        "f.shareLevel=$shareLevel, " +
                        "f.label=$label, " +
                        "f.comment=$comment, " +
                        "f.private_context=$privateContext, " +
                        "f.public_context=$publicContext, " +
                        "f.images=$images, " +
                        "f.creation_date=$creationDate, " +
                        "f.last_modification_date=timestamp(), " +
                        "f.nb_private_neighbors=0," +
                        "f.nb_friend_neighbors=0," +
                        "f.nb_public_neighbors=0 " +
                        "CREATE UNIQUE (n)-[r:IDENTIFIED_TO]->(f) " +
                        "SET r.relation_external_uri=$relationExternalUri, " +
                        "f.%s=f.%s + 1, " +
                        FriendlyResourceNeo4j.LAST_MODIFICATION_QUERY_PART +
                        "RETURN f.uri as uri, " +
                        "f.external_uri as external_uri, " +
                        "f.label as label, " +
                        "f.comment as comment, " +
                        "f.images as images, " +
                        "f.creation_date as creation_date, " +
                        "f.last_modification_date as last_modification_date, " +
                        "f.nb_private_neighbors as nbPrivateNeighbors, " +
                        "f.nb_friend_neighbors as nbFriendNeighbors, " +
                        "f.nb_public_neighbors as nbPublicNeighbors, " +
                        "f.shareLevel ",
                queryPrefix,
                neighborsPropertyName,
                neighborsPropertyName
        );
    }
}
