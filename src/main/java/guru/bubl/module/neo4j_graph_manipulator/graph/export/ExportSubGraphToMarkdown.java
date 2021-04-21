package guru.bubl.module.neo4j_graph_manipulator.graph.export;

import com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter;
import guru.bubl.module.model.UserUris;
import guru.bubl.module.model.graph.ShareLevel;
import guru.bubl.module.model.graph.edge.Edge;
import guru.bubl.module.model.graph.graph_element.GraphElement;
import guru.bubl.module.model.graph.group_relation.GroupRelationPojo;
import guru.bubl.module.model.graph.relation.Relation;
import guru.bubl.module.model.graph.subgraph.SubGraph;
import guru.bubl.module.model.graph.subgraph.SubGraphPojo;
import guru.bubl.module.model.graph.subgraph.UserGraph;
import guru.bubl.module.model.graph.tag.Tag;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ExportSubGraphToMarkdown {
    private SubGraph subGraph = SubGraphPojo.empty();
    private URI centerUri;
    private Set<URI> centers;
    private Set<URI> visitedParents = new HashSet<>();
    private UserGraph userGraph;

    private List<String> footNotes = new ArrayList<>();


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
        if (!isCenter) {
            markdown.append(" ".repeat(Math.max(0, depth * 2)));
            markdown.append("* ");
        }
        String relationLabel = "";
        if (parentRelation != null && !parentRelation.label().trim().equals("")) {
            relationLabel = "(" + MdFile.formatLabel(parentRelation.label()) + ")" + buildNoteReference(parentRelation) + buildTagString(parentRelation) + " ";
        }
        markdown.append(relationLabel);
        String parentLabel = isCenter ? "" : MdFile.formatLabel(parent.label());
        String afterLabel = buildNoteReference(parent) + buildTagString(parent);
        if (UserUris.isUriOfAGroupRelation(parentUri)) {
            markdown.append("(" + parentLabel + ")" + afterLabel);
        } else if (!isCenter && centers.contains(parentUri)) {
            markdown.append("[[" + MdFile.applyNameFilter(parent.label()) + "]]").append("\n");
            return markdown.toString();
        } else {
            markdown.append(parentLabel + afterLabel);
        }
        if (!isCenter || !afterLabel.trim().equals("")) {
            markdown.append("\n");
        }
        URI parentForkUri = otherUri(parentUri, parentRelation);
        JSONObject childrenIndex = new JSONObject();
        try {
            if (!parent.getChildrenIndex().equals("")) {
                childrenIndex = new JSONObject(parent.getChildrenIndex());
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        CompareByChildrenIndex compareByChildrenIndex = new CompareByChildrenIndex(
                parentUri,
                subGraph,
                childrenIndex
        );
        List<Edge> edges = Stream.concat(subGraph.edges().values().stream(), subGraph.getGroupRelations().values().stream())
                .collect(Collectors.toList());
        Collections.sort(edges, compareByChildrenIndex);
        for (Edge edge : edges) {
            if (UserUris.isUriOfAGroupRelation(edge.uri())) {
                GroupRelationPojo groupRelationPojo = (GroupRelationPojo) edge;
                if (parentUri.equals(groupRelationPojo.getSourceForkUri()) && !visitedParents.contains(edge.uri())) {
                    markdown.append(
                            buildForParentUri(
                                    edge.uri(),
                                    null,
                                    depth + 1
                            )
                    );
                }
            } else {
                Relation relation = (Relation) edge;
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
        }
        if (isCenter) {
            markdown.append(buildFootnotes());
        }
        return markdown.toString();
    }

    private String buildFootnotes() {
        StringBuilder footNotesStr = new StringBuilder();
        for (int i = 1; i <= footNotes.size(); i++) {
            footNotesStr.append(" [^" + i + "]: ");
            String note = FlexmarkHtmlConverter.builder().build().convert(footNotes.get(i - 1));
            footNotesStr.append(note.trim() + "\n");
        }
        return footNotesStr.toString();
    }

    private String buildTagString(GraphElement graphElement) {
        List<String> tagLabels = new ArrayList<>();
        for (Tag tag : graphElement.getTags().values()) {
            tagLabels.add("#" + tag.label().trim().replaceAll("\\s+", "-"));
        }
        String space = "";
        if (tagLabels.size() > 0) {
            space = " ";
        }
        return space + String.join(" ", tagLabels);
    }

    private String buildNoteReference(GraphElement graphElement) {
        if (!graphElement.gotComments()) {
            return "";
        }
        footNotes.add(graphElement.comment());
        return " [^" + footNotes.size() + "] ";
    }

    private URI otherUri(URI parentUri, Relation relation) {
        if (relation == null) {
            return null;
        }
        if (parentUri == null) {
            return centerUri;
        }
        return relation.getOtherForkUri(parentUri);
    }

}
