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
        return name.replaceAll("[^a-zA-Z0-9a-zwÀ-Üà-øoù-ÿŒœ\\.\\-]", "_");
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getContent() {
        return content;
    }
}
