package guru.bubl.module.neo4j_graph_manipulator.graph.export;

import guru.bubl.module.model.graph.relation.Relation;
import guru.bubl.module.model.graph.subgraph.SubGraph;
import org.codehaus.jettison.json.JSONObject;

import java.net.URI;
import java.util.Comparator;
import java.util.Map;

public class CompareByChildrenIndex implements Comparator<Relation> {

    private SubGraph subGraph;
    private JSONObject indexInfo;
    private URI parentUri;

    public CompareByChildrenIndex(URI parentUri, SubGraph subGraph, JSONObject indexInfo) {
        this.parentUri = parentUri;
        this.subGraph = subGraph;
        this.indexInfo = indexInfo;
    }

    @Override
    public int compare(Relation r1, Relation r2) {
        return this.getIndexForRelation(r1) - this.getIndexForRelation(r2);
    }

    private int getIndexForRelation(Relation relation) {
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
