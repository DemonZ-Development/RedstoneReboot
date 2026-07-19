package dev.demonz.redstonereboot.bukkit;

import dev.demonz.redstonereboot.common.manager.RestartHistory;
import dev.demonz.redstonereboot.common.manager.RestartManager;
import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class RedstoneRebootFeaturesTest {

    private ServerMock server;
    private RedstoneRebootPlugin plugin;

    @BeforeEach
    public void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(RedstoneRebootPlugin.class);
    }

    @AfterEach
    public void tearDown() {
        MockBukkit.unmock();
    }

    private PlayerMock admin() {
        PlayerMock p = server.addPlayer();
        p.addAttachment(plugin, "redstonereboot.use", true);
        p.addAttachment(plugin, "redstonereboot.status", true);
        p.addAttachment(plugin, "redstonereboot.doctor", true);
        p.addAttachment(plugin, "redstonereboot.restart.now", true);
        p.addAttachment(plugin, "redstonereboot.restart.cancel", true);
        p.addAttachment(plugin, "redstonereboot.notify", true);
        return p;
    }

    private List<String> drain(PlayerMock p, int max) {
        List<String> out = new ArrayList<>();
        for (int i = 0; i < max; i++) {
            try {
                String m = p.nextMessage();
                if (m == null) break;
                out.add(m);
            } catch (AssertionError e) {
                break;
            }
        }
        return out;
    }

    @Test
    public void testDoctorShowsLiveStats() {
        // 3 players online (doc + 2 others) so the Players stat is non-zero
        PlayerMock doc = admin();
        server.addPlayer();
        server.addPlayer();

        doc.performCommand("reboot doctor");

        List<String> msgs = drain(doc, 10);
        String joined = String.join("\n", msgs);

        assertTrue(joined.contains("RedstoneReboot Diagnostics"), "doctor header missing: " + joined);
        assertTrue(joined.contains("Live Stats"), "live stats missing: " + joined);
        assertTrue(joined.contains("TPS:"), "tps line missing: " + joined);
        assertTrue(joined.contains("Memory:"), "memory line missing: " + joined);
        assertTrue(joined.contains("Players:"), "players line missing: " + joined);
        assertTrue(joined.contains("Restart Status"), "restart status missing: " + joined);
        assertTrue(joined.contains("Scheduled"), "scheduled restart line missing: " + joined);
        assertTrue(joined.matches("(?s).*Players:.*3.*"),
                "expected 3 online players reported, got: " + joined);
    }

    @Test
    public void testDoctorShowsInProgressRestartWithInitiator() {
        PlayerMock doc = admin();
        doc.performCommand("reboot now 30");

        RestartManager rm = plugin.getRestartManager();
        assertTrue(rm.isRestartInProgress());
        assertTrue(rm.getCurrentRestartReason().getDisplayName().contains("Manual"),
                "expected Manual reason, got: " + rm.getCurrentRestartReason().getDisplayName());

        doc.performCommand("reboot doctor");
        List<String> msgs = drain(doc, 12);
        String joined = String.join("\n", msgs);

        assertTrue(joined.contains("Restart Status:") && joined.contains("In progress"),
                "expected in-progress restart status, got: " + joined);
        assertTrue(joined.contains("Initiator:"), "expected initiator in doctor output, got: " + joined);
        assertTrue(joined.contains(doc.getName()), "expected initiator name in doctor output, got: " + joined);

        // cancel so we don't leave a countdown running
        doc.performCommand("reboot cancel");
        assertFalse(rm.isRestartInProgress());
    }

    @Test
    public void testHistoryRecordsScheduleAndCancel() {
        PlayerMock admin = admin();
        RestartManager rm = plugin.getRestartManager();

        admin.performCommand("reboot now 30");
        assertTrue(rm.isRestartInProgress());

        admin.performCommand("reboot cancel");
        assertFalse(rm.isRestartInProgress());

        // History should contain at least the SCHEDULED and CANCELLED events
        List<RestartHistory.Entry> entries = rm.getHistory().getRecent(10);
        assertTrue(entries.size() >= 2, "expected >=2 history entries, got: " + entries.size());

        admin.performCommand("reboot history");
        List<String> msgs = drain(admin, 8);
        String joined = String.join("\n", msgs);

        assertTrue(joined.contains("Recent Restarts"), "history header missing: " + joined);
        assertFalse(joined.contains("No restart events recorded"),
                "history should not be empty: " + joined);
        assertTrue(joined.contains("SCHEDULED"), "SCHEDULED entry missing: " + joined);
        assertTrue(joined.contains("CANCELLED"), "CANCELLED entry missing: " + joined);
    }
}
