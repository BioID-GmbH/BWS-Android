package com.bioid.authenticator.base.logging;

import android.support.annotation.NonNull;

/**
 * Provides a similar API to {@link android.util.Log} for logging with a stopwatch extension.
 * All log messages can have arguments like in {@link String#format(String, Object...)}.
 * <p/>
 * To obtain a LoggingHelper instance use the {@link LoggingHelperFactory}.
 * <p/>
 * Implementations must not be thread-safe!
 */
@SuppressWarnings("unused")
public interface LoggingHelper {

    /**
     * Logging on log-level debug.
     */
    void d(@NonNull String msg, Object... args);

    /**
     * Logging on log-level info.
     */
    void i(@NonNull String msg, Object... args);

    /**
     * Logging on log-level warning.
     */
    void w(@NonNull String msg, Object... args);

    /**
     * Logging on log-level warning.
     */
    void w(@NonNull Throwable tr, @NonNull String msg, Object... args);

    /**
     * Logging on log-level error.
     */
    void e(@NonNull String msg, Object... args);

    /**
     * Logging on log-level error.
     */
    void e(@NonNull Throwable tr, @NonNull String msg, Object... args);

    /**
     * Starts a new stopwatch session.
     * If a stopwatch session the same id is already present it will be replaced.
     *
     * @param sessionId of the session (see also {@link #stopStopwatch(String)})
     * @return the sessionId passed in as parameter
     */
    String startStopwatch(@NonNull String sessionId);

    /**
     * Stops a stopwatch session which does log the elapsed time on log-level debug.
     *
     * @param sessionId of the session (see also {@link #startStopwatch(String)})
     * @throws IllegalArgumentException if no active stopwatch session is present for the given id
     */
    void stopStopwatch(@NonNull String sessionId);
}
