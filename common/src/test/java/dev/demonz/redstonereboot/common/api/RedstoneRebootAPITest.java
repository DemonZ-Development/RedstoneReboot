package dev.demonz.redstonereboot.common.api;

import dev.demonz.redstonereboot.common.manager.RestartReason;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RedstoneRebootAPITest {

    @Test
    void dispatchHonorsAdapterFilters() {
        RedstoneRebootAPI api = new RedstoneRebootAPI(null);
        AtomicInteger deliveries = new AtomicInteger();
        MessageAdapter adapter = new MessageAdapter() {
            @Override
            public void onMessage(MessageContext context) {
                deliveries.incrementAndGet();
            }

            @Override
            public boolean handlesPostponed() {
                return false;
            }

            @Override
            public boolean handlesAlerts() {
                return false;
            }
        };

        api.registerMessageAdapter(adapter);
        try {
            api.dispatchMessage(context(MessageContext.Type.POSTPONED));
            api.dispatchMessage(context(MessageContext.Type.SCHEDULED_ALERT));
            api.dispatchMessage(context(MessageContext.Type.FINAL_ALERT));
            assertEquals(1, deliveries.get());
        } finally {
            api.unregisterMessageAdapter(adapter);
        }
    }

    @Test
    void dispatchRejectsNullContext() {
        RedstoneRebootAPI api = new RedstoneRebootAPI(null);
        assertThrows(NullPointerException.class, () -> api.dispatchMessage(null));
    }

    private static MessageContext context(MessageContext.Type type) {
        return new MessageContext(
            type,
            30,
            RestartReason.SCHEDULED,
            "test",
            "message",
            null,
            null,
            System.currentTimeMillis(),
            "test",
            "test"
        );
    }
}
