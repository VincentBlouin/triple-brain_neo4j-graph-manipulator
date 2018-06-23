/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph;

import java.util.HashMap;
import java.util.Map;

public class RestApiUtilsNeo4j {

    public static Map<String, Object> wrap(Map<String,Object> props){
        return map(
                "props", props
        );
    }

    public static Map<String,Object> map(){
        return new HashMap<String,Object>();
    }
    public static Map<String,Object> map(String s1, Object o1){
        Map<String,Object> map = map();
        map.put(s1, o1);
        return map;
    }
    public static Map<String,Object> map(String s1, Object o1, String s2, Object o2){
        Map<String,Object> map = map(
                s1, o1
        );
        map.put(s2, o2);
        return map;
    }
    public static Map<String,Object> map(String s1, Object o1, String s2, Object o2, String s3, Object o3){
        Map<String,Object> map = map(
                s1, o1,
                s2, o2
        );
        map.put(s3, o3);
        return map;
    }

    public static Map<String,Object> map(String s1, Object o1, String s2, Object o2, String s3, Object o3, String s4, Object o4){
        Map<String,Object> map = map(
                s1, o1,
                s2, o2,
                s3, o3
        );
        map.put(s4, o4);
        return map;
    }

    public static Map<String,Object> map(String s1, Object o1, String s2, Object o2, String s3, Object o3, String s4, Object o4, String s5, Object o5){
        Map<String,Object> map = map(
                s1, o1,
                s2, o2,
                s3, o3,
                s4, o4
        );
        map.put(s5, o5);
        return map;
    }

    public static Map<String,Object> map(
            String s1, Object o1, String s2, Object o2, String s3, Object o3, String s4, Object o4, String s5, Object o5, String s6, Object o6
    ){
        Map<String,Object> map = map(
                s1, o1,
                s2, o2,
                s3, o3,
                s4, o4,
                s5, o5
        );
        map.put(s6, o6);
        return map;
    }

    public static Map<String,Object> map(
            String s1, Object o1, String s2, Object o2, String s3, Object o3, String s4, Object o4, String s5, Object o5, String s6, Object o6, String s7, Object o7
    ){
        Map<String,Object> map = map(
                s1, o1,
                s2, o2,
                s3, o3,
                s4, o4,
                s5, o5,
                s6, o6
        );
        map.put(s7, o7);
        return map;
    }

    public static Map<String,Object> map(
            String s1, Object o1, String s2, Object o2, String s3, Object o3, String s4, Object o4, String s5, Object o5, String s6, Object o6, String s7, Object o7, String s8, Object o8
    ){
        Map<String,Object> map = map(
                s1, o1,
                s2, o2,
                s3, o3,
                s4, o4,
                s5, o5,
                s6, o6,
                s7, o7
        );
        map.put(s8, o8);
        return map;
    }

    public static Map<String,Object> map(
            String s1, Object o1, String s2, Object o2, String s3, Object o3, String s4, Object o4, String s5, Object o5, String s6, Object o6, String s7, Object o7, String s8, Object o8, String s9, Object o9
    ){
        Map<String,Object> map = map(
                s1, o1,
                s2, o2,
                s3, o3,
                s4, o4,
                s5, o5,
                s6, o6,
                s7, o7,
                s8, o8
        );
        map.put(s9, o9);
        return map;
    }

    public static Map<String,Object> map(
            String s1, Object o1, String s2, Object o2, String s3, Object o3, String s4, Object o4, String s5, Object o5, String s6, Object o6, String s7, Object o7, String s8, Object o8, String s9, Object o9, String s10, Object o10
    ){
        Map<String,Object> map = map(
                s1, o1,
                s2, o2,
                s3, o3,
                s4, o4,
                s5, o5,
                s6, o6,
                s7, o7,
                s8, o8,
                s9, o9
        );
        map.put(s10, o10);
        return map;
    }

    public static Map<String,Object> map(
            String s1, Object o1, String s2, Object o2, String s3, Object o3, String s4, Object o4, String s5, Object o5, String s6, Object o6, String s7, Object o7, String s8, Object o8, String s9, Object o9, String s10, Object o10, String s11, Object o11
    ){
        Map<String,Object> map = map(
                s1, o1,
                s2, o2,
                s3, o3,
                s4, o4,
                s5, o5,
                s6, o6,
                s7, o7,
                s8, o8,
                s9, o9,
                s10, o10
        );
        map.put(s11, o11);
        return map;
    }
}
