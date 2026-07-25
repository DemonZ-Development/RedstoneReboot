/*
 * Copyright (c) 2026 DemonZ Development
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


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
        double avgNanos = 1000000.0; // 1ms per tick → would be 1000 TPS uncapped
        double tps = Math.min(20.0, 1000000000.0 / avgNanos);
        assertEquals(20.0, tps, 0.01,
            "TPS should be capped at 20.0");
    }


    @Test
    void tpsCalculationFromTypicalTickTimes() {
        double avgNanos = 50_000_000.0; // 50ms
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