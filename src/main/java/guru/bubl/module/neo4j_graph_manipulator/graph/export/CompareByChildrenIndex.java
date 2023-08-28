package guru.bubl.module.neo4j_graph_manipulator.graph.export;

import guru.bubl.module.model.graph.edge.Edge;
import guru.bubl.module.model.graph.subgraph.SubGraph;
import org.codehaus.jettison.json.JSONObject;

import java.net.URI;
import java.util.Comparator;

public class CompareByChildrenIndex implements Comparator<Edge> {

    private SubGraph subGraph;
    private JSONObject indexInfo;
    private URI parentUri;

    public CompareByChildrenIndex(URI parentUri, SubGraph subGraph, JSONObject indexInfo) {
        this.parentUri = parentUri;
        this.subGraph = subGraph;
        this.indexInfo = indexInfo;
    }

    @Override
    public int compare(Edge r1, Edge r2) {
        return this.getIndexForRelation(r1) - this.getIndexForRelation(r2);
    }

    private int getIndexForRelation(Edge relation) {
//        System.out.println(relation.label());
//        System.out.println(indexInfo);
        if (indexInfo == null) {
//            System.out.println("null index info");
            return 0;
        }
        URI forkUri = relation.getOtherForkUri(parentUri);
        if(forkUri == null){
//            System.out.println("null fork uri");
            return 0;
        }
        JSONObject indexJson = indexInfo.optJSONObject(forkUri.toString());
        if(indexJson == null){
//            System.out.println("null index");
            return 0;
        }
//        System.out.println(indexJson.optInt("index", -1));
        return indexJson.optInt("index", -1);
    }
}
