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


package dev.demonz.redstonereboot.bukkit.scheduler;

import dev.demonz.redstonereboot.common.scheduler.PlatformTaskScheduler;
import org.bukkit.plugin.java.JavaPlugin;

public final class BukkitSchedulerFactory {

    private static volatile Boolean foliaEnvironment;

    public static PlatformTaskScheduler create(JavaPlugin plugin) {
        if (isFoliaEnvironment()) {
            try {
                return new FoliaTaskScheduler(plugin);
            } catch (Exception exception) {
                plugin.getLogger().warning("Folia environment detected but scheduler bridge failed to initialize. Falling back to Bukkit: " + exception.getMessage());
            }
        }
        return new BukkitTaskScheduler(plugin);
    }

    public static boolean isFoliaEnvironment() {
        if (foliaEnvironment != null) return foliaEnvironment;
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            foliaEnvironment = true;
            return true;
        } catch (ClassNotFoundException exception) {
            foliaEnvironment = false;
            return false;
        }
    }
}