package guru.bubl.module.neo4j_graph_manipulator.graph.export;

public class MdFile {
    private String name;
    private String content;

    public MdFile(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getContent() {
        return content;
    }
}
