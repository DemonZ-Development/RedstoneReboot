package dev.demonz.redstonereboot.common.utils;

import java.lang.reflect.Field;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility to safely extract TPS data from MinecraftServer across different platforms and mappings.
 */
public final class MinecraftTPSUtil {

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
        if (server == null || reflectionFailed) return 20.0;

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

            long[] times = (long[]) tickTimesField.get(server);
            if (times == null || times.length == 0) return 20.0;

            long sum = 0;
            for (long t : times) {
                sum += t;
            }

            double avgNanos = (double) sum / times.length;
            return Math.min(20.0, 1000000000.0 / avgNanos);

        } catch (Exception e) {
            reflectionFailed = true;
            logger.log(Level.WARNING, "Failed to extract TPS via reflection", e);
            return 20.0;
        }
    }

    private static Field findTickTimesField(Class<?> clazz) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            // Try common mapping names
            for (String name : new String[]{"tickTimes", "h", "field_1740", "tickLengths"}) {
                try {
                    return current.getDeclaredField(name);
                } catch (NoSuchFieldException ignored) {}
            }
            current = current.getSuperclass();
        }
        return null;
    }
}
