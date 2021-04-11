package guru.bubl.module.neo4j_graph_manipulator.graph.export;

import guru.bubl.module.model.UserUris;
import guru.bubl.module.model.graph.ShareLevel;
import guru.bubl.module.model.graph.graph_element.GraphElement;
import guru.bubl.module.model.graph.group_relation.GroupRelation;
import guru.bubl.module.model.graph.group_relation.GroupRelationPojo;
import guru.bubl.module.model.graph.relation.Relation;
import guru.bubl.module.model.graph.subgraph.SubGraph;
import guru.bubl.module.model.graph.subgraph.SubGraphPojo;
import guru.bubl.module.model.graph.subgraph.UserGraph;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ExportSubGraphToMarkdown {
    private SubGraph subGraph = SubGraphPojo.empty();
    private URI centerUri;
    private Set<URI> centers;
    private Set<URI> visitedParents = new HashSet<>();
    private UserGraph userGraph;


    public ExportSubGraphToMarkdown(UserGraph userGraph, URI centerUri, Set<URI> centers) {
        this.userGraph = userGraph;
        this.centerUri = centerUri;
        this.centers = centers;
    }

    public String export() {
        return buildForParentUri(centerUri, null, 0);
    }

    public String buildForParentUri(URI parentUri, Relation parentRelation, Integer depth) {
        visitedParents.add(parentUri);
        StringBuilder markdown = new StringBuilder();
        subGraph.mergeWith(
                userGraph.aroundForkUriWithDepthInShareLevels(
                        parentUri,
                        1,
                        ShareLevel.allShareLevelsInt
                )
        );
        GraphElement parent = subGraph.vertexWithIdentifier(parentUri);
        if (parent == null) {
            parent = subGraph.getGroupRelations().get(parentUri);
        }
        Boolean isCenter = parentUri.equals(centerUri) && parentRelation == null;
        if (isCenter) {
            markdown.append("# ");
        } else {
            markdown.append(" ".repeat(Math.max(0, depth * 2)));
            markdown.append("* ");
        }
        String relationLabel = "";
        if (parentRelation != null && !parentRelation.label().trim().equals("")) {
            relationLabel = "(" + parentRelation.label() + ") ";
        }
        markdown.append(relationLabel);
        if (UserUris.isUriOfAGroupRelation(parentUri)) {
            markdown.append("(" + parent.label() + ")");
        } else if (!isCenter && centers.contains(parentUri)) {
            markdown.append("[[" + MdFile.applyNameFilter(parent.label()) + "]]").append("\n");
            return markdown.toString();
        } else {
            markdown.append(parent.label());
        }
        markdown.append("\n");
        URI parentForkUri = otherUri(parentUri, parentRelation);
        List<Relation> relationsCopy = new ArrayList(subGraph.edges().values());
        for (Relation relation : relationsCopy) {
            URI otherUri = otherUri(parentUri, relation);
            if (otherUri != null && (parentForkUri == null || !parentForkUri.equals(otherUri)) && !visitedParents.contains(otherUri)) {
                markdown.append(
                        buildForParentUri(
                                otherUri,
                                relation,
                                depth + 1
                        )
                );
            }
        }
        List<GroupRelationPojo> groupRelationsCopy = new ArrayList(subGraph.getGroupRelations().values());
        for (GroupRelationPojo groupRelation : groupRelationsCopy) {
            if (parentUri.equals(groupRelation.getSourceForkUri()) && !visitedParents.contains(groupRelation.uri())) {
                markdown.append(
                        buildForParentUri(
                                groupRelation.uri(),
                                null,
                                depth + 1
                        )
                );
            }
        }
        return markdown.toString();
    }

    private URI otherUri(URI parentUri, Relation relation) {
        if (relation == null) {
            return null;
        }
        if (parentUri == null) {
            return centerUri;
        }
        if (relation.sourceUri().equals(parentUri)) {
            return relation.destinationUri();
        } else if (relation.destinationUri().equals(parentUri)) {
            return relation.sourceUri();
        } else {
            return null;
        }
    }

}
