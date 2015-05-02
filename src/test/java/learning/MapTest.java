/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package learning;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class MapTest {
    @Test
    public void can_keep_original_value_if_hashmap_merge_conflicts(){
        Map<String, String> original = new HashMap<String, String>();
        original.put("pomme", "avion");
        Map<String, String> newest = new HashMap<String, String>();
        newest.put("pomme", "banane");
        newest.putAll(original);
        assertThat(
                newest.get("pomme"), is("avion")
        );
    }
}
