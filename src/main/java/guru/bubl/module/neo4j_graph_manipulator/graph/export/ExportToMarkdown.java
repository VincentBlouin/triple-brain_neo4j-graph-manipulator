package guru.bubl.module.neo4j_graph_manipulator.graph.export;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import guru.bubl.module.model.User;
import guru.bubl.module.model.graph.GraphFactory;
import guru.bubl.module.model.graph.ShareLevel;
import guru.bubl.module.model.graph.edge.Edge;
import guru.bubl.module.model.graph.edge.EdgePojo;
import guru.bubl.module.model.graph.relation.Relation;
import guru.bubl.module.model.graph.subgraph.SubGraph;
import guru.bubl.module.model.graph.subgraph.SubGraphPojo;
import guru.bubl.module.model.graph.subgraph.UserGraph;
import guru.bubl.module.model.graph.vertex.Vertex;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;

import java.net.URI;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static org.neo4j.driver.Values.parameters;

public class ExportToMarkdown {

    @Inject
    private Driver driver;

    @javax.inject.Inject
    private GraphFactory graphFactory;

    private String username;

    @AssistedInject
    protected ExportToMarkdown(
            @Assisted String username
    ) {
        this.username = username;
    }


    private List<Files> export() {
        return null;
    }

    public List<String> exportStrings() {
        List<URI> centers = new ArrayList<>();
        try (Session session = driver.session()) {
            Result rs = session.run(
                    "MATCH (center:GraphElement{owner:$owner}) " +
                            "WHERE EXISTS(center.last_center_date) " +
                            "RETURN center.uri as uri",
                    parameters(
                            "owner",
                            username
                    )
            );
            while (rs.hasNext()) {
                Record record = rs.next();
                centers.add(
                        URI.create(record.get("uri").asString())
                );
            }
        }
        UserGraph userGraph = graphFactory.loadForUser(
                User.withUsername(username)
        );
        List<String> pages = new ArrayList<>();
        for (URI centerUri : centers) {
            SubGraph subGraph = userGraph.aroundForkUriWithDepthInShareLevels(
                    centerUri,
                    200,
                    ShareLevel.allShareLevelsInt
            );
            ExportSubGraphToMarkdown exportSubGraphToMarkdown = new ExportSubGraphToMarkdown(
                    subGraph,
                    centerUri,
                    centers
            );
            pages.add(
                    exportSubGraphToMarkdown.export()
            );
        }
//        try (Session session = driver.session()) {
//            Result rs = session.run(
//                    "MATCH (center:GraphElement{owner:$owner}) " +
//                            "WHERE EXISTS(center.last_center_date) " +
//                            "set center:Center " +
//                            "WITH center, COLLECT(center) as terminatorNodes " +
//                            "CALL apoc.path.subgraphAll(center,{}) YIELD nodes " +
//                            "return path as label ",
//                            "WITH collect(nodes) as nodesByCenter " +
//                            "UNWIND nodesByCenter as centers " +
//                            "RETURN reduce(labels = ' ', n IN centers | labels + n.label + ' ') AS label",
//                            "UNWIND centers as proute " +
//                            "RETURN proute.label as label",
//                    parameters(
//                            "owner",
//                            username
//                    )
//            );
//            while (rs.hasNext()) {
//                Record record = rs.next();
//                pages.add(
//                        record.get("label").asObject().toString()
//                );
//            }
//        }
//        for (URI uri : centers) {
//            try (Session session = driver.session()) {
//                Result rs = session.run(
//                        "MATCH centers"
//                        "MATCH (center:GraphElement{uri:$uri}) " +
//                                "WITH center," +
//                                "CALL apoc.path.subgraphAll(center, { terminatorNodes: COLLECT(c)}) YIELD input, output, error " +
//                                "WITH collect(output) as centerNodes " +
//                                "UNWIND centerNodes as c " +
//                                "RETURN c.uri as uri, c.label",
//                        parameters(
//                                "uri",
//                                uri.toString(),
//                                "centers",
//                                centers
//                        )
//                );
//                while (rs.hasNext()) {
//                    Record record = rs.next();
//                    pages.add(
//                            record.get("label").asString()
//                    );
//                }
//            }
//        }
        return pages;
    }

//    public static File zip(List<File> files, String filename) {
//        File zipfile = new File(filename);
//        // Create a buffer for reading the files
//        byte[] buf = new byte[1024];
//        try {
//            // create the ZIP file
//            ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipfile));
//            // compress the files
//            for (int i = 0; i < files.size(); i++) {
//                FileInputStream in = new FileInputStream(files.get(i).getCanonicalName());
//                // add ZIP entry to output stream
//                out.putNextEntry(new ZipEntry(files.get(i).getName()));
//                // transfer bytes from the file to the ZIP file
//                int len;
//                while ((len = in.read(buf)) > 0) {
//                    out.write(buf, 0, len);
//                }
//                // complete the entry
//                out.closeEntry();
//                in.close();
//            }
//            // complete the ZIP file
//            out.close();
//            return zipfile;
//        } catch (IOException ex) {
//            System.err.println(ex.getMessage());
//        }
//        return null;
//    }

}
