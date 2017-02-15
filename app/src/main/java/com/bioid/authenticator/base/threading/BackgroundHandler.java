package com.bioid.authenticator.base.threading;

import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.bioid.authenticator.base.functional.Consumer;
import com.bioid.authenticator.base.functional.Supplier;

/**
 * Can be used to offload work onto a background thread.
 */
@SuppressWarnings({"SameParameterValue", "UnusedReturnValue", "unused"})
public interface BackgroundHandler {

    /**
     * Does execute a runnable on the calling thread after a specified delay.
     *
     * @param runnable      runs on calling thread after delay (the operation should not throw any exception)
     * @param delayInMillis time to wait in milliseconds (waiting is non blocking)
     * @return ID of associated scheduled task which can be used to cancel the task.
     */
    int runWithDelay(@NonNull Runnable runnable, @IntRange(from = 0) long delayInMillis);

    /**
     * Does cancel a scheduled task.
     * The task won't be executed anymore.
     * <p/>
     * If the task has already completed or no task with such an ID is present nothing does happen.
     *
     * @param taskId ID of the task
     */
    void cancelScheduledTask(int taskId);

    /**
     * Does cancel all scheduled tasks.
     * The tasks won't be executed anymore.
     */
    void cancelAllScheduledTasks();

    /**
     * Does execute a supplier on a background thread.
     * <p/>
     * As soon as the supplier returns the result will be passed to the onSuccess callback on the calling (non background) thread.
     * If the supplier terminated with a exception the exception will be passed to the onError callback on the calling thread.
     * <p/>
     * After completion of the supplier the onComplete callback will be executed on the calling thread.
     * This does happen before onSuccess or onError are executed.
     * If onComplete throws an exception onError will be called instead of onSuccess with the exception from onComplete.
     *
     * @param supplier   runs on the background thread
     * @param onSuccess  runs on the calling thread
     * @param onError    runs on the calling thread (will not be called if null)
     * @param onComplete runs on the calling thread (will not be called if null)
     * @return ID of associated background task which can be used to unsubscribe from the task.
     */
    <T> int runOnBackgroundThread(@NonNull final Supplier<T> supplier,
                                  @NonNull final Consumer<T> onSuccess, @Nullable final Consumer<RuntimeException> onError,
                                  @Nullable final Runnable onComplete);

    /**
     * Does execute a runnable on a background thread.
     * <p/>
     * As soon as the runnable completes the onSuccess callback will be executed on the calling (non background) thread.
     * If the runnable terminated with a exception the exception will be passed to the onError callback on the calling thread.
     * <p/>
     * After completion of the runnable the onComplete callback will be executed on the calling thread.
     * This does happen before onSuccess or onError are executed.
     * If onComplete throws an exception onError will be called instead of onSuccess with the exception from onComplete.
     *
     * @param runnable   runs on the background thread
     * @param onSuccess  runs on the calling thread (will not be called if null)
     * @param onError    runs on the calling thread (will not be called if null)
     * @param onComplete runs on the calling thread (will not be called if null)
     * @return ID of associated background task which can be used to unsubscribe from the task.
     */
    int runOnBackgroundThread(@NonNull final Runnable runnable,
                              @Nullable final Runnable onSuccess, @Nullable final Consumer<RuntimeException> onError,
                              @Nullable final Runnable onComplete);

    /**
     * Does unsubscribe any callback functions from the background task.
     * The actual operation in the background will still be executed but no callback (onSuccess, onError or onComplete) will be called.
     * <p/>
     * If the task has already completed or no task with such an ID is present nothing does happen.
     *
     * @param taskId ID of the task
     */
    void unsubscribeFromBackgroundTask(int taskId);

    /**
     * Does unsubscribe from any background task.
     * The actual operation in the background will still be executed but no callback (onSuccess, onError or onComplete) will be called.
     */
    void unsubscribeFromAllBackgroundTasks();
}
