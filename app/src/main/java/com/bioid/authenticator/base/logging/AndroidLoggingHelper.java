package com.bioid.authenticator.base.logging;

import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.util.ArrayMap;
import android.util.Log;

import java.util.Locale;

/**
 * Uses {@link android.util.Log} to implement the {@link LoggingHelper} interface.
 */
final class AndroidLoggingHelper implements LoggingHelper {

    @VisibleForTesting
    final String tag;
    private final ArrayMap<String, Long> stopwatchSessionIdToStartTimeInMillis = new ArrayMap<>();

    public AndroidLoggingHelper(@NonNull Class clazz) {
        this.tag = clazz.getSimpleName().substring(0, clazz.getSimpleName().length() > 23 ? 23 : clazz.getSimpleName().length());
    }

    @Override
    public void d(@NonNull String msg, Object... args) {
        Log.d(tag, format(msg, args));
    }

    @Override
    public void i(@NonNull String msg, Object... args) {
        Log.i(tag, format(msg, args));
    }

    @Override
    public void w(@NonNull String msg, Object... args) {
        Log.w(tag, format(msg, args));
    }

    @Override
    public void w(@NonNull Throwable tr, @NonNull String msg, Object... args) {
        Log.w(tag, format(msg, args), tr);
    }

    @Override
    public void e(@NonNull String msg, Object... args) {
        Log.e(tag, format(msg, args));
    }

    @Override
    public void e(@NonNull Throwable tr, @NonNull String msg, Object... args) {
        Log.e(tag, format(msg, args), tr);
    }

    private String format(@NonNull String msg, Object... args) {
        return String.format(Locale.ENGLISH, msg, args);
    }

    @Override
    public String startStopwatch(@NonNull String sessionId) {
        stopwatchSessionIdToStartTimeInMillis.put(sessionId, SystemClock.elapsedRealtime());
        return sessionId;
    }

    @Override
    public void stopStopwatch(@NonNull String sessionId) {
        Long startTime = stopwatchSessionIdToStartTimeInMillis.get(sessionId);
        if (startTime == null) {
            throw new IllegalArgumentException(String.format("no active stopwatch session with id '%s'", sessionId));
        }
        long elapsedTime = SystemClock.elapsedRealtime() - startTime;
        Log.d(tag, format("%s took %d ms", sessionId, elapsedTime));
    }
}
