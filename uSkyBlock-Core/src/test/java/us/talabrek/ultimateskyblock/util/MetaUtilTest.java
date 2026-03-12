package us.talabrek.ultimateskyblock.util;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MetaUtilTest {
    @Test
    public void testSingleMapParse() throws Exception {
        String inputMap = "{\"Color\":8}";

        Map<String, Object> outputMap = MetaUtil.createMap(inputMap);
        assertEquals(1, outputMap.size());
        assertEquals(8.0, outputMap.get("Color"));
    }
}
