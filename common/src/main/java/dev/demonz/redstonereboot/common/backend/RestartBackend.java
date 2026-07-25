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

/**
 * Interface for a restart execution backend.
 */
public interface RestartBackend {

    /**
     * Get the identifying name of this backend.
     */
    String getName();

    /**
     * Prepare for restart (e.g., ensure scripts are generated or connections alive).
     * Called at the start of the countdown.
     */
    void prepare();

    /**
     * Execute the restart logic.
     *
     * @return the result of the execution attempt
     */
    BackendResult execute();

    /**
     * Get the current diagnostic state of the backend.
     */
    BackendState getState();

    /**
     * @return true if this backend handles both shutdown and startup (no local shutdown needed).
     */
    boolean isControllerOwned();

    /**
     * Release any resources held by this backend (e.g., HTTP clients, connections).
     * Called when the backend is replaced during a reload.
     */
    default void cleanup() {}

    /**
     * Verification states for the 'doctor' diagnostic tool.
     */
    enum BackendState {
        /** Backend is configured, wired, and verified. */
        FULL,
        /** Configured but verification (API/Connectivity) failed. */
        ASSISTED,
        /** Script/Service generated but not 'wired' into the startup command. */
        GENERATED,
        /** Server relies on host environment (Pterodactyl, systemd, Docker, external script) for restart. */
        DEPEND_ON_HOST,
        /** Deprecated alias for DEPEND_ON_HOST. */
        @Deprecated
        SHUTDOWN_ONLY,
        /** Critical configuration missing. */
        MISCONFIGURED
    }
}