package dev.demonz.redstonereboot.common.utils;

import java.lang.reflect.Field;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class MinecraftTPSUtil {

    private static final Object LOCK = new Object();
    private static Field tickTimesField;
    private static boolean reflectionFailed = false;

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
                "field_1740", "field_4735"
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
