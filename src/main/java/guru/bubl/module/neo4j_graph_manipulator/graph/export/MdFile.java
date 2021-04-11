package guru.bubl.module.neo4j_graph_manipulator.graph.export;

public class MdFile {
    private String name;
    private String content;

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
}
