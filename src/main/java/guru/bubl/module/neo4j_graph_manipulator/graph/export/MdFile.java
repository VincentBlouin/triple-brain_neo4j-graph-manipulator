package guru.bubl.module.neo4j_graph_manipulator.graph.export;

public class MdFile {
    private String name;
    private String content;
    Long creationDate;
    Long lastModificationDate;

    public MdFile(String name) {
        this.name = name;
    }

    public String getName() {
        if (name.trim().equals("")) {
            return "write it";
        }
        return applyNameFilter(name);
    }

    public static String applyNameFilter(String string) {
        return string.replaceAll("[^a-zA-Z0-9\\sa-zwÀ-Üà-øoù-ÿŒœ\\.\\-]", "_");
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getContent() {
        return content;
    }

    public Long getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Long creationDate) {
        this.creationDate = creationDate;
    }

    public Long getLastModificationDate() {
        return lastModificationDate;
    }

    public void setLastModificationDate(Long lastModificationDate) {
        this.lastModificationDate = lastModificationDate;
    }

    public static String formatLabel(String label){
        // \R is regex to replace all line breaks
        return label.replaceAll("\\R", "");
    }
}
