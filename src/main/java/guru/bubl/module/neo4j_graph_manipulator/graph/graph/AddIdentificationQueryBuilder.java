/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.graph;

import guru.bubl.module.model.UserUris;
import guru.bubl.module.model.graph.GraphElementType;
import guru.bubl.module.model.graph.identification.Identifier;
import guru.bubl.module.model.graph.identification.IdentifierPojo;
import guru.bubl.module.neo4j_graph_manipulator.graph.Neo4jFriendlyResource;
import guru.bubl.module.neo4j_graph_manipulator.graph.Relationships;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.identification.Neo4jIdentification;
import guru.bubl.module.neo4j_graph_manipulator.graph.image.Neo4jImages;

public class AddIdentificationQueryBuilder {

    private IdentifierPojo identification;
    private Neo4jGraphElementOperator graphElement;
    private Boolean isOwnerOfIdentification,
            isIdentifyingToAnIdentification;

    public static AddIdentificationQueryBuilder usingIdentificationForGraphElement(
            IdentifierPojo identification, Neo4jGraphElementOperator graphElement
    ) {
        return new AddIdentificationQueryBuilder(
                identification,
                graphElement
        );
    }

    protected AddIdentificationQueryBuilder(
            IdentifierPojo identification,
            Neo4jGraphElementOperator graphElement
    ) {
        this.identification = identification;
        this.graphElement = graphElement;
        isOwnerOfIdentification = UserUris.ownerUserNameFromUri(
                identification.getExternalResourceUri()
        ).equals(graphElement.getOwnerUsername());
        isIdentifyingToAnIdentification = UserUris.isUriOfAnIdentifier(
                identification.getExternalResourceUri()
        );
    }

    public String build() {
        return String.format(
                "%sMERGE (f {%s: @external_uri, %s: @owner}) " +
                        "ON CREATE SET f.uri = @uri, " +
                        "f.%s='%s', " + // graph element type = meta
                        "f.%s=@label, " +
                        "f.%s=@comment, " +
                        "f.%s=@images, " +
                        "f.%s=@%s, " +
                        "f.%s=timestamp(), " +
                        "f.%s=timestamp(), " +
                        "f.%s=%s " + //initial nb references
                        "CREATE UNIQUE n-[r:%s]->f%s " +
                        "SET r.%s=@relationExternalUri,%s " +
                        "f.%s=f.%s + 1, " + //nbReferences
                        Neo4jFriendlyResource.LAST_MODIFICATION_QUERY_PART +
                        "%s" +
                        "RETURN f.uri as uri, " +
                        "f.external_uri as external_uri, " +
                        "f.%s as label, " +
                        "f.%s as comment, " +
                        "f.%s as images, " +
                        "f.%s as creation_date, " +
                        "f.%s as last_modification_date, " +
                        "f.%s as nbReferences",
                queryPrefix(),
                Neo4jIdentification.props.external_uri,
                Neo4jFriendlyResource.props.owner,
                Neo4jFriendlyResource.props.type,
                GraphElementType.meta,
                Neo4jFriendlyResource.props.label,
                Neo4jFriendlyResource.props.comment,
                Neo4jImages.props.images,
                Neo4jFriendlyResource.props.type,
                Neo4jFriendlyResource.props.type,
                Neo4jFriendlyResource.props.creation_date,
                Neo4jFriendlyResource.props.last_modification_date,
                Neo4jIdentification.props.nb_references,
                isOwnerOfIdentification && !isIdentifyingToAnIdentification ? "1" : "0",
                Relationships.IDENTIFIED_TO,
                isOwnerOfIdentification ?
                        String.format(
                                ", i-[r2:%s]->f ",
                                Relationships.IDENTIFIED_TO
                        ) : " ",
                Neo4jIdentification.props.relation_external_uri,
                isOwnerOfIdentification ?
                        String.format(
                                " r2.%s='%s', ",
                                Neo4jIdentification.props.relation_external_uri,
                                Identifier.DEFAULT_IDENTIFIER_RELATION_EXTERNAL_URI
                        ) : " ",
                Neo4jIdentification.props.nb_references,
                Neo4jIdentification.props.nb_references,
                isOwnerOfIdentification ? addExistingIdentificationsQueryPart() : " ",
                Neo4jFriendlyResource.props.label,
                Neo4jFriendlyResource.props.comment,
                Neo4jImages.props.images,
                Neo4jFriendlyResource.props.creation_date,
                Neo4jFriendlyResource.props.last_modification_date,
                Neo4jIdentification.props.nb_references
        );
    }

    private String queryPrefix() {
        String queryPrefix = this.graphElement.queryPrefix();
        return isOwnerOfIdentification ?
                String.format(
                        queryPrefix + ", i=node:node_auto_index(\"uri:%s\") ",
                        identification.getExternalResourceUri()
                ) : queryPrefix;
    }

    private String addExistingIdentificationsQueryPart(){
        return String.format(
                "WITH n, i, f " +
                "OPTIONAL MATCH i-[irg:%s]->g " +
                        "CREATE UNIQUE n-[nrg:%s]->g " +
                        "SET g.%s = CASE WHEN nrg.%s is null THEN g.%s + 1 ELSE g.%s END " +
                        "SET nrg.%s = CASE WHEN nrg.%s is null THEN irg.%s ELSE nrg.%s END " +
                "WITH g as f ",
                Relationships.IDENTIFIED_TO,
                Relationships.IDENTIFIED_TO,
                Neo4jIdentification.props.nb_references,
                Neo4jIdentification.props.relation_external_uri,
                Neo4jIdentification.props.nb_references,
                Neo4jIdentification.props.nb_references,
                Neo4jIdentification.props.relation_external_uri,
                Neo4jIdentification.props.relation_external_uri,
                Neo4jIdentification.props.relation_external_uri,
                Neo4jIdentification.props.relation_external_uri
        );
    }
}
