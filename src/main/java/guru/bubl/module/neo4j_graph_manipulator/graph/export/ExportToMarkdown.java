package guru.bubl.module.neo4j_graph_manipulator.graph.export;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import guru.bubl.module.model.User;
import guru.bubl.module.model.UserUris;
import guru.bubl.module.model.center_graph_element.*;
import guru.bubl.module.model.graph.GraphFactory;
import guru.bubl.module.model.graph.ShareLevel;
import guru.bubl.module.model.graph.subgraph.SubGraph;
import guru.bubl.module.model.graph.subgraph.UserGraph;
import net.lingala.zip4j.ZipFile;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;

import java.io.File;
import java.io.FileWriter;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.neo4j.driver.Values.parameters;

public class ExportToMarkdown {

    private static SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

    @Inject
    private Driver driver;

    @javax.inject.Inject
    private GraphFactory graphFactory;

    @Inject
    private CenterGraphElementsOperatorFactory centerGraphElementsOperatorFactory;

    private String username;

    @AssistedInject
    protected ExportToMarkdown(
            @Assisted String username
    ) {
        this.username = username;
    }


    public File export() {
        System.out.println("start export v8 " + formatter.format(new Date()));
        return writeFilesToZip(
                exportStrings()
        );
    }

    public LinkedHashMap<URI, MdFile> exportStrings() {
        LinkedHashMap<URI, MdFile> centers = new LinkedHashMap<>();
        Integer page = 0;
        Boolean hasMore = true;
        Integer skip = 28;
        while (hasMore) {
            CenteredGraphElementsOperator centeredGraphElementsOperator = centerGraphElementsOperatorFactory.usingLimitAndSkip(
                    skip,
                    skip * page
            );
            List<CenterGraphElementPojo> centersPojo = centeredGraphElementsOperator.getPublicAndPrivateForOwner(User.withUsername(username));
            for (CenterGraphElement center : centersPojo) {
                if (!UserUris.isUriOfATag(center.getGraphElement().uri()) && !UserUris.isUriOfARelation(center.getGraphElement().uri())) {
                    centers.put(
                            center.getGraphElement().uri(),
                            new MdFile(
                                    center.getGraphElement().label()
                            )
                    );
                }
            }
            if (centersPojo.size() == 0) {
                hasMore = false;
            }
            page++;
        }

//        try (Session session = driver.session()) {
//            Result rs = session.run(
//                    "MATCH (center:GraphElement{owner:$owner}) " +
//                            "WHERE EXISTS(center.last_center_date) " +
//                            "RETURN center.uri as uri, center.label as label",
//                    parameters(
//                            "owner",
//                            username
//                    )
//            );
//            while (rs.hasNext()) {
//                Record record = rs.next();
//                System.out.println("building " + record.get("label").asString() + " " + formatter.format(new Date()));
//                centers.put(
//                        URI.create(record.get("uri").asString()),
//                        new MdFile(
//                                record.get("label").asString()
//                        )
//                );
//            }
//        }
        System.out.println("done loading centers");
        UserGraph userGraph = graphFactory.loadForUser(
                User.withUsername(username)
        );
        for (URI centerUri : centers.keySet()) {
            MdFile mdFile = centers.get(centerUri);
//            System.out.println("export markdown for " + mdFile.getName());
            ExportSubGraphToMarkdown exportSubGraphToMarkdown = new ExportSubGraphToMarkdown(
                    userGraph,
                    centerUri,
                    centers.keySet()
            );
//            System.out.println("building md file " + mdFile.getName());
            mdFile.setContent(exportSubGraphToMarkdown.export());
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
        return centers;
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

    private File writeFilesToZip(LinkedHashMap<URI, MdFile> files) {
        System.out.println("start writing files " + formatter.format(new Date()));
        String PATH = "/tmp/mindrespect.com/" + username;
        try {
            Files.createDirectories(Paths.get(PATH));
            ZipFile zipFile = new ZipFile("/tmp/mindrespect.com/" + username + "/" + username + ".zip");
            for (MdFile file : files.values()) {
                String filePath = PATH + "/" + file.getName() + ".md";
                FileWriter myWriter = new FileWriter(filePath);
                myWriter.write(file.getContent());
                myWriter.close();
                zipFile.addFile(new File(filePath));
            }
            System.out.println("done writing file " + formatter.format(new Date()));
            return zipFile.getFile();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
