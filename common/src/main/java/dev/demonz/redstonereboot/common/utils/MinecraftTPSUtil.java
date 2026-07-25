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

import java.lang.reflect.Field;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility to safely extract TPS data from MinecraftServer across different platforms and mappings.
 *
 * <p><b>Java 9+ Module Access Note:</b> On Java 9+ with module encapsulation,
 * the reflective call {@code Field.setAccessible(true)} on the tick-time array field
 * of {@code MinecraftServer} may throw {@link InaccessibleObjectException} unless
 * the JVM is started with the appropriate {@code --add-opens} flag. For example:</p>
 *
 * <pre>{@code
 * --add-opens java.base/java.lang=ALL-UNNAMED
 * --add-opens net.minecraft.server/net.minecraft.server=ALL-UNNAMED
 * }</pre>
 *
 * <p>If the flag is missing, this utility gracefully falls back to returning 20.0 TPS
 * and logs a warning. Fabric and Forge loaders typically handle this automatically,
 * but standalone or custom launch configurations may need the flag added manually.</p>
 */
public final class MinecraftTPSUtil {

    private static final Object LOCK = new Object();
    private static Field tickTimesField;
    private static boolean reflectionFailed = false;

    /**
     * Calculate TPS from a MinecraftServer instance using reflection to find the tickTimes field.
     * 
     * @param server The MinecraftServer instance (must be passed as Object to avoid direct dependency)
     * @param logger Logger for errors
     * @return Calculated TPS (0.0 to 20.0)
     */
    public static double calculateTPS(Object server, Logger logger) {
        if (server == null) return 20.0;

        synchronized (LOCK) {
            if (reflectionFailed) return 20.0;

            try {
                if (tickTimesField == null) {
                    tickTimesField = findTickTimesField(server.getClass());
                    if (tickTimesField == null) {
                        reflectionFailed = true;
                        logger.warning("Could not find tickTimes field on " + server.getClass().getName());
                        return 20.0;
                    }
                    tickTimesField.setAccessible(true);
                }

                Object raw = tickTimesField.get(server);
                if (raw == null) return 20.0;

                long[] times;
                if (raw instanceof long[]) {
                    times = (long[]) raw;
                } else if (raw instanceof double[]) {
                    double[] dtimes = (double[]) raw;
                    double dsum = 0;
                    for (double t : dtimes) dsum += t;
                    double avgNanos = dsum / dtimes.length;
                    return Math.min(20.0, 1000000000.0 / avgNanos);
                } else {
                    return 20.0;
                }

                if (times.length == 0) return 20.0;
                long sum = 0;
                for (long t : times) sum += t;

                double avgNanos = (double) sum / times.length;
                return Math.min(20.0, 1000000000.0 / avgNanos);

            } catch (Exception e) {
                reflectionFailed = true;
                logger.log(Level.WARNING, "Failed to extract TPS via reflection", e);
                return 20.0;
            }
        }
    }

    private static Field findTickTimesField(Class<?> clazz) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            for (String name : new String[]{
                "tickTimes", "tickLengths",
                "field_1740", "field_4735"   // common intermediary names for tick time arrays
            }) {
                try {
                    return current.getDeclaredField(name);
                } catch (NoSuchFieldException ignored) {}
            }
            current = current.getSuperclass();
        }
        return null;
    }
}