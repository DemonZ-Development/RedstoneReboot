package dev.demonz.redstonereboot.common.backend;

/**
 * Result of a restart backend execution.
 */
public enum BackendResult {
    /**
     * The restart signal was accepted (e.g., API call success).
     * For controller backends, this means the backend now "owns" the restart.
     */
    ACCEPTED,

    /**
     * The restart signal was explicitly rejected or failed (e.g., Auth error).
     * The shutdown sequence should be cancelled.
     */
    FAILED,

    /**
     * The result is ambiguous (e.g., Timeout).
     * The shutdown should be cancelled and a lockout period initiated.
     */
    UNKNOWN
}
