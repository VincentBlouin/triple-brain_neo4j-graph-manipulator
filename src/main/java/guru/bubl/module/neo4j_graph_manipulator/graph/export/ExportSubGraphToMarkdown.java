package guru.bubl.module.neo4j_graph_manipulator.graph.export;

import guru.bubl.module.model.UserUris;
import guru.bubl.module.model.graph.graph_element.GraphElement;
import guru.bubl.module.model.graph.group_relation.GroupRelationPojo;
import guru.bubl.module.model.graph.relation.Relation;
import guru.bubl.module.model.graph.subgraph.SubGraph;

import java.net.URI;
import java.util.List;

public class ExportSubGraphToMarkdown {
    private SubGraph subGraph;
    private URI centerUri;
    private List<URI> centers;

    public ExportSubGraphToMarkdown(SubGraph subGraph, URI centerUri, List<URI> centers) {
        this.subGraph = subGraph;
        this.centerUri = centerUri;
        this.centers = centers;
    }

    public String export() {
        return buildForParentUri(centerUri, null, 0);
    }

    public String buildForParentUri(URI parentUri, Relation parentRelation, Integer depth) {
        StringBuilder markdown = new StringBuilder();
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
            markdown.append("[[" + parent.label() + "]]");
            return markdown.toString();
        } else {
            markdown.append(parent.label());
        }
        markdown.append("\n");
        URI parentForkUri = otherUri(parentUri, parentRelation);
        for (Relation relation : subGraph.edges().values()) {
            URI otherUri = otherUri(parentUri, relation);
            if (otherUri != null && (parentForkUri == null || !parentForkUri.equals(otherUri))) {
                markdown.append(
                        buildForParentUri(
                                otherUri,
                                relation,
                                depth + 1
                        )
                );
            }
        }
        for (GroupRelationPojo groupRelation : subGraph.getGroupRelations().values()) {
            if (parentUri.equals(groupRelation.getSourceForkUri())) {
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
