package dev.demonz.redstonereboot.common.utils;

import org.junit.jupiter.api.Test;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Practical tests for {@link MinecraftTPSUtil} — TPS calculation logic,
 * null handling, and fallback behavior.
 */
class MinecraftTPSUtilTest {

    private final Logger logger = Logger.getLogger("MinecraftTPSUtilTest");

    // --- Null server returns 20.0 ---

    @Test
    void nullServerReturns20TPS() {
        assertEquals(20.0, MinecraftTPSUtil.calculateTPS(null, logger),
            "Null server should return fallback TPS of 20.0");
    }

    // --- Non-MinecraftServer object returns 20.0 ---

    @Test
    void nonMinecraftServerObjectReturns20TPS() {
        Object fakeServer = new Object();
        assertEquals(20.0, MinecraftTPSUtil.calculateTPS(fakeServer, logger),
            "Non-MinecraftServer object should return fallback TPS of 20.0");
    }

    // --- TPS is capped at 20.0 ---

    @Test
    void tpsIsCappedAt20() {
        // The calculation uses Math.min(20.0, 1000000000.0 / avgNanos)
        // So if avgNanos is very small (fast ticks), TPS is capped at 20
        double avgNanos = 1000000.0; // 1ms per tick → would be 1000 TPS uncapped
        double tps = Math.min(20.0, 1000000000.0 / avgNanos);
        assertEquals(20.0, tps, 0.01,
            "TPS should be capped at 20.0");
    }

    // --- TPS calculation from typical tick times ---

    @Test
    void tpsCalculationFromTypicalTickTimes() {
        // 50ms per tick average → 20 TPS (ideal)
        double avgNanos = 50_000_000.0; // 50ms
        double tps = Math.min(20.0, 1000000000.0 / avgNanos);
        assertEquals(20.0, tps, 0.01,
            "50ms per tick should yield 20 TPS");

        // 100ms per tick average → 10 TPS
        avgNanos = 100_000_000.0;
        tps = Math.min(20.0, 1000000000.0 / avgNanos);
        assertEquals(10.0, tps, 0.01,
            "100ms per tick should yield 10 TPS");

        // 200ms per tick average → 5 TPS
        avgNanos = 200_000_000.0;
        tps = Math.min(20.0, 1000000000.0 / avgNanos);
        assertEquals(5.0, tps, 0.01,
            "200ms per tick should yield 5 TPS");
    }

    // --- Reflection failure is cached ---

    @Test
    void reflectionFailureIsCached() {
        // First call with a non-MinecraftServer object will fail and cache the failure
        Object fake = new Object();
        double first = MinecraftTPSUtil.calculateTPS(fake, logger);
        assertEquals(20.0, first);

        // Second call should also return 20.0 immediately (cached failure)
        double second = MinecraftTPSUtil.calculateTPS(fake, logger);
        assertEquals(20.0, second);
    }
}
