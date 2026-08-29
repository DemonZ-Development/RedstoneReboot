package dev.demonz.redstonereboot.common.backend;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class EnvironmentDetector {

    public static List<String> detectPotentialBackends() {
        List<String> results = new ArrayList<>();

        try {
            if (Files.exists(Paths.get("/run/systemd/system"))) {
                results.add("SYSTEMD");
            }
        } catch (SecurityException ignored) {
        }

        try {
            if (Files.exists(Paths.get("/.dockerenv"))) {
                results.add("DOCKER");
            }
        } catch (SecurityException ignored) {
        }

        String ptero = System.getenv("PTERODACTYL");
        if ("1".equals(ptero)) {
            results.add("PTERODACTYL");
        } else {
            try {
                if (Files.exists(Paths.get(".pterodactyl"))) {
                    results.add("PTERODACTYL");
                }
            } catch (SecurityException ignored) {
            }
        }

        return results;
    }
}
