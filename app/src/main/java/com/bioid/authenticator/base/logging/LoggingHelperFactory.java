package com.bioid.authenticator.base.logging;

import android.support.annotation.NonNull;

import com.bioid.authenticator.BuildConfig;

/**
 * Factory to obtain {@link LoggingHelper} instances.
 */
public final class LoggingHelperFactory {

    /**
     * Creates a new LoggingHelper.
     */
    public static LoggingHelper create(@NonNull Class clazz) {
        if (BuildConfig.DEBUG) {
            return new AndroidLoggingHelper(clazz);
        } else {
            return new NopLoggingHelper();
        }
    }
}
