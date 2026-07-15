package us.talabrek.ultimateskyblock.ittest.cli;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HarnessCliTest {
    @Test
    void derivesBukkitOfflineUuid() {
        assertEquals(UUID.fromString("b50ad385-829d-3141-a216-7e7d7539ba7f"), HarnessCli.offlineUuid("Notch"));
        assertEquals(UUID.fromString("0853a413-79ab-3a45-953f-be922b6bac00"), HarnessCli.offlineUuid("UsbItPlayer"));
    }
}
