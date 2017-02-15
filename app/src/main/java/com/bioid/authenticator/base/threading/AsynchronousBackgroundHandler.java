package com.bioid.authenticator.base.threading;

import android.os.AsyncTask;
import android.os.CountDownTimer;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.ArrayMap;
import android.support.v4.util.SimpleArrayMap;

import com.bioid.authenticator.base.functional.Consumer;
import com.bioid.authenticator.base.functional.Supplier;

/**
 * Uses Android capabilities to offload work to a pool of background threads.
 */
final public class AsynchronousBackgroundHandler implements BackgroundHandler {

    private final ArrayMap<Integer, CountDownTimer> scheduledTasks = new ArrayMap<>();
    private final ArrayMap<Integer, AsyncTask> backgroundTasks = new ArrayMap<>();

    private <T> int getTaskId(SimpleArrayMap<Integer, T> taskList) {
        for (int i = 0; i < Integer.MAX_VALUE; i++) {
            if (!taskList.containsKey(i)) {
                return i;
            }
        }
        throw new IllegalStateException("no more task ids available");
    }

    @Override
    public int runWithDelay(@NonNull final Runnable runnable, @IntRange(from = 0) long delayInMillis) {
        final int taskId = getTaskId(scheduledTasks);

        CountDownTimer scheduledTask = new CountDownTimer(delayInMillis, delayInMillis) {

            @Override
            public void onTick(long l) {
                // do nothing (is disabled anyway because "countDownInterval" is set to "millisInFuture")
            }

            @Override
            public void onFinish() {
                scheduledTasks.remove(taskId);
                runnable.run();
            }
        }.start();

        scheduledTasks.put(taskId, scheduledTask);

        return taskId;
    }

    @Override
    public void cancelScheduledTask(int taskId) {
        CountDownTimer scheduledTask = scheduledTasks.remove(taskId);
        if (scheduledTask != null) {
            scheduledTask.cancel();
        }
    }

    @Override
    public void cancelAllScheduledTasks() {
        for (Integer taskId : scheduledTasks.keySet()) {
            scheduledTasks.get(taskId).cancel();
        }
        scheduledTasks.clear();
    }

    @Override
    public <T> int runOnBackgroundThread(@NonNull Supplier<T> supplier,
                                         @NonNull Consumer<T> onSuccess, @Nullable Consumer<RuntimeException> onError,
                                         @Nullable Runnable onComplete) {
        final int taskId = getTaskId(backgroundTasks);

        AsyncTask<Void, Void, T> backgroundTask = new AsyncTaskWithException<>(taskId, supplier, onSuccess, onError, onComplete)
                .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        backgroundTasks.put(taskId, backgroundTask);
        return taskId;
    }

    @Override
    public int runOnBackgroundThread(@NonNull final Runnable runnable,
                                     @Nullable final Runnable onSuccess, @Nullable Consumer<RuntimeException> onError,
                                     @Nullable Runnable onComplete) {
        final int taskId = getTaskId(backgroundTasks);

        AsyncTask<Void, Void, Object> backgroundTask = new AsyncTaskWithException<>(taskId, new Supplier<Object>() {
            @Override
            public Object get() {
                runnable.run();
                return new Object();  // returning a non null value indicates success
            }
        }, new Consumer<Object>() {
            @Override
            public void accept(Object ignored) {
                if (onSuccess != null) {
                    onSuccess.run();
                }
            }
        }, onError, onComplete).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        backgroundTasks.put(taskId, backgroundTask);
        return taskId;
    }

    @Override
    public void unsubscribeFromBackgroundTask(int taskId) {
        AsyncTask backgroundTask = backgroundTasks.remove(taskId);
        if (backgroundTask != null) {
            // Passing in "false" because the already running background task should not be interrupted.
            // If the task has completed or is marked as canceled we don't care, therefore the return value won't be checked.
            backgroundTask.cancel(false);
        }
    }

    @Override
    public void unsubscribeFromAllBackgroundTasks() {
        for (Integer taskId : backgroundTasks.keySet()) {
            // Passing in "false" because the already running background task should not be interrupted.
            // If the task has completed or is marked as canceled we don't care, therefore the return value won't be checked.
            backgroundTasks.get(taskId).cancel(false);
        }
        backgroundTasks.clear();
    }

    private class AsyncTaskWithException<T> extends AsyncTask<Void, Void, T> {

        private final int taskId;
        private final Supplier<T> supplier;
        private final Consumer<T> onSuccess;
        private final Consumer<RuntimeException> onError;
        private final Runnable onComplete;

        private RuntimeException exception = null;

        public AsyncTaskWithException(int taskId, @NonNull Supplier<T> supplier,
                                      @Nullable Consumer<T> onSuccess, @Nullable Consumer<RuntimeException> onError,
                                      @Nullable Runnable onComplete) {
            this.taskId = taskId;
            this.supplier = supplier;
            this.onSuccess = onSuccess;
            this.onError = onError;
            this.onComplete = onComplete;
        }

        @Override
        protected T doInBackground(Void... params) {
            try {
                return supplier.get();
            } catch (RuntimeException e) {
                exception = e;
                return null;
            }
        }

        @Override
        protected void onPostExecute(T result) {
            backgroundTasks.remove(taskId);

            if (onComplete != null) {
                try {
                    onComplete.run();
                } catch (RuntimeException e) {
                    exception = e;
                }
            }

            if (exception != null) {
                if (onError != null) {
                    onError.accept(exception);
                }
                return;
            }

            if (onSuccess != null) {
                onSuccess.accept(result);
            }
        }
    }
}
