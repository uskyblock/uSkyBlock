package dk.lockfuglsang.minecraft.yml;

import org.bukkit.configuration.InvalidConfigurationException;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.mockito.hamcrest.MockitoHamcrest;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * Created by R4zorax on 15/07/2016.
 */
public class YmlConfigurationTest {
    @Test
    public void saveToString() throws Exception {
        File simpleYml = new File(getClass().getClassLoader().getResource("yml/simple.yml").toURI());
        YmlConfiguration config = new YmlConfiguration();
        config.load(simpleYml);

        config.set("root.child node.abe", "lincoln\nwas a wonderful\npresident");
        String expected = new String(Files.readAllBytes(Paths.get(getClass().getClassLoader().getResource("yml/simple_expected.yml").toURI())), "UTF-8");
        assertThat(config.saveToString(), is(expected));
    }


    @Test
    public void testGetStringList_Spaces() throws Exception {
        File simpleYml = new File(getClass().getClassLoader().getResource("yml/simple.yml").toURI());
        YmlConfiguration config = new YmlConfiguration();
        config.load(simpleYml);

        List<String> actual = config.getStringList("root.child node.some-section.space-list");
        assertThat(actual, Matchers.contains("13:1", "34:3"));
    }

    @Test
    public void testGetStringList_List() throws Exception {
        File simpleYml = new File(getClass().getClassLoader().getResource("yml/simple.yml").toURI());
        YmlConfiguration config = new YmlConfiguration();
        config.load(simpleYml);

        List<String> actual = config.getStringList("root.child node.some-section.another-list");
        assertThat(actual, Matchers.contains("what now", "do you know?"));
    }

    @Test
    public void testGetStringList_OneLineList() throws Exception {
        File simpleYml = new File(getClass().getClassLoader().getResource("yml/simple.yml").toURI());
        YmlConfiguration config = new YmlConfiguration();
        config.load(simpleYml);

        List<String> actual = config.getStringList("root.child node.some-section.section-list");
        assertThat(actual, Matchers.contains("Hej", "Mor"));
    }

}