package dev.demonz.redstonereboot.common.backend;

/**
 * Result of a restart backend execution attempt.
 * <p>
 * Each backend's {@link RestartBackend#execute()} method returns one of these
 * values to indicate whether the restart signal was accepted, rejected, or
 * had an ambiguous outcome.
 * </p>
 *
 * @see RestartBackend
 * @since 1.0.0
 */
public enum BackendResult {
    /**
     * The restart signal was accepted (e.g., API call returned 2xx).
     * For controller backends, this means the backend now "owns" the restart.
     */
    ACCEPTED,

    /**
     * The restart signal was explicitly rejected or failed (e.g., auth error, 4xx).
     * The shutdown sequence should be cancelled.
     */
    FAILED,

    /**
     * The result is ambiguous (e.g., network timeout, 5xx).
     * The shutdown should be cancelled and a lockout period initiated.
     */
    UNKNOWN
}
