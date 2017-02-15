package com.bioid.authenticator.base.mvp;

import android.app.Activity;
import android.app.Fragment;

/**
 * The interface LifecycleAware indicates that the presenter does need to react to some lifecycle events.
 */
public interface LifecycleAware {

    /**
     * Callback which is called if {@link Activity#onResume()} or {@link Fragment#onResume()} is called.
     */
    void onResume();

    /**
     * Callback which is called if {@link Activity#onPause()} or {@link Fragment#onPause()} ()} is called.
     */
    void onPause();
}
