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


package dev.demonz.redstonereboot.common.scheduler;

/**
 * Handle to a scheduled task that allows cancellation.
 * <p>
 * Returned by {@link PlatformTaskScheduler} when scheduling tasks.
 * Calling {@link #cancel()} is idempotent; cancelling an already-cancelled
 * or completed task has no effect.
 * </p>
 *
 * @since 1.0.0
 */
@FunctionalInterface
public interface ScheduledTaskHandle {

    /**
     * Cancel this scheduled task. If the task has already completed or been
     * cancelled, this method has no effect.
     */
    void cancel();
}