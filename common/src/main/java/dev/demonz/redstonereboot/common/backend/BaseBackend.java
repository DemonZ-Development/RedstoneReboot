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

import java.util.logging.Logger;

/**
 * Base implementation for all restart backends.
 */
public abstract class BaseBackend implements RestartBackend {

    protected final Logger logger;
    private final String name;

    protected BaseBackend(Logger logger, String name) {
        this.logger = logger;
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void prepare() {
    }

    @Override
    public abstract BackendResult execute();

    @Override
    public abstract BackendState getState();

    @Override
    public abstract boolean isControllerOwned();
}