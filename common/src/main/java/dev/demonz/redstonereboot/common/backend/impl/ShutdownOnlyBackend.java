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


package dev.demonz.redstonereboot.common.backend.impl;

import dev.demonz.redstonereboot.common.backend.BackendResult;
import dev.demonz.redstonereboot.common.backend.SupervisorBackend;

import java.util.logging.Logger;

/**
 * Fallback backend that relies on host environment (Pterodactyl, Systemd, Docker, wrapper script) for restart.
 */
public class ShutdownOnlyBackend extends SupervisorBackend {

    public ShutdownOnlyBackend(Logger logger) {
        super(logger, "ShutdownOnly");
    }

    @Override
    public BackendResult execute() {
        return BackendResult.ACCEPTED;
    }

    @Override
    public BackendState getState() {
        return BackendState.DEPEND_ON_HOST;
    }
}