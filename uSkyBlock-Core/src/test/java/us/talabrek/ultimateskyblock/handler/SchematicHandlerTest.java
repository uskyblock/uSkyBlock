package us.talabrek.ultimateskyblock.handler;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Path;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class SchematicHandlerTest {
    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void resolveTargetAcceptsNormalEntry() throws Exception {
        Path root = tmp.getRoot().toPath().resolve("plugins/uSkyBlock/schematics");
        Optional<Path> result = SchematicHandler.resolveTarget(root, "default.schematic");

        assertThat("Expected normal schematic path to resolve", result.isPresent(), is(true));
        assertThat(result.get().startsWith(root.toAbsolutePath().normalize()), is(true));
        assertThat(result.get().getFileName().toString(), is("default.schematic"));
    }

    @Test
    public void resolveTargetRejectsTraversal() throws Exception {
        Path root = tmp.getRoot().toPath().resolve("plugins/uSkyBlock/schematics");
        Optional<Path> result = SchematicHandler.resolveTarget(root, "../evil.schematic");

        assertThat("Traversal paths must be rejected", result.isPresent(), is(false));
    }
}
