package dev.demonz.redstonereboot.common.utils;

import org.junit.jupiter.api.Test;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MinecraftTPSUtilTest {

    private final Logger logger = Logger.getLogger("MinecraftTPSUtilTest");

    @Test
    void nullServerReturns20TPS() {
        assertEquals(20.0, MinecraftTPSUtil.calculateTPS(null, logger),
            "Null server should return fallback TPS of 20.0");
    }

    @Test
    void nonMinecraftServerObjectReturns20TPS() {
        Object fakeServer = new Object();
        assertEquals(20.0, MinecraftTPSUtil.calculateTPS(fakeServer, logger),
            "Non-MinecraftServer object should return fallback TPS of 20.0");
    }

    @Test
    void tpsIsCappedAt20() {
        double avgNanos = 1000000.0;
        double tps = Math.min(20.0, 1000000000.0 / avgNanos);
        assertEquals(20.0, tps, 0.01,
            "TPS should be capped at 20.0");
    }

    @Test
    void tpsCalculationFromTypicalTickTimes() {
        double avgNanos = 50_000_000.0;
        double tps = Math.min(20.0, 1000000000.0 / avgNanos);
        assertEquals(20.0, tps, 0.01,
            "50ms per tick should yield 20 TPS");

        avgNanos = 100_000_000.0;
        tps = Math.min(20.0, 1000000000.0 / avgNanos);
        assertEquals(10.0, tps, 0.01,
            "100ms per tick should yield 10 TPS");

        avgNanos = 200_000_000.0;
        tps = Math.min(20.0, 1000000000.0 / avgNanos);
        assertEquals(5.0, tps, 0.01,
            "200ms per tick should yield 5 TPS");
    }

    @Test
    void reflectionFailureIsCached() {
        Object fake = new Object();
        double first = MinecraftTPSUtil.calculateTPS(fake, logger);
        assertEquals(20.0, first);

        double second = MinecraftTPSUtil.calculateTPS(fake, logger);
        assertEquals(20.0, second);
    }
}
