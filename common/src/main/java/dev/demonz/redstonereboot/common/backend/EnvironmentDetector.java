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


package dev.demonz.redstonereboot.common.backend;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Advisory environment detector to help users choose the right backend.
 */
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