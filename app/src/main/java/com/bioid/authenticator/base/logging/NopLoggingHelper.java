package com.bioid.authenticator.base.logging;

import android.support.annotation.NonNull;

/**
 * No-operation implementation of the {@link LoggingHelper} interface.
 */
final class NopLoggingHelper implements LoggingHelper {

    @Override
    public void d(@NonNull String msg, Object... args) {
    }

    @Override
    public void i(@NonNull String msg, Object... args) {
    }

    @Override
    public void w(@NonNull String msg, Object... args) {
    }

    @Override
    public void w(@NonNull Throwable tr, @NonNull String msg, Object... args) {
    }

    @Override
    public void e(@NonNull String msg, Object... args) {
    }

    @Override
    public void e(@NonNull Throwable tr, @NonNull String msg, Object... args) {
    }

    @Override
    public String startStopwatch(@NonNull String sessionId) {
        return sessionId;
    }

    @Override
    public void stopStopwatch(@NonNull String sessionId) {
    }
}
