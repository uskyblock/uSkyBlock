package dk.lockfuglsang.minecraft.nbt;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.hamcrest.CoreMatchers;
import org.junit.Test;

import java.io.StringReader;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests the NBTUtil
 */
public class NBTUtilTest {

    /**
     * Tests that the JSONMap will return the proper syntax.
     */
    @Test
    public void testJSONMap() {
        String jsonString = "{\"Potion\":\"minecraft:empty\",\"CustomPotionEffects\":[{\"Id\":1},{\"Id\":2}]}";
        Gson gson = new Gson();

        Map<String, Object> map = gson.fromJson(new StringReader(jsonString), new TypeToken<Map<String, Object>>(){}.getType());
        assertThat(map.get("Potion"), CoreMatchers.is("minecraft:empty"));
        assertThat(map.get("CustomPotionEffects"), instanceOf(List.class));
        assertThat(((List<?>)map.get("CustomPotionEffects")).getFirst(), instanceOf(Map.class));
    }

    @Test
    public void testGetGraftBukkitVersion() {
        assertThat("net.minecraft.server.v1_10_R1.NBTTagString".split("\\.")[3], is("v1_10_R1"));
    }
}
