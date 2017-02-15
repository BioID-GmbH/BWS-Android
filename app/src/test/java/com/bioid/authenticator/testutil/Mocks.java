package com.bioid.authenticator.testutil;

import android.util.Size;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Provides helper functions to create mock objects for testing.
 */
public final class Mocks {

    /**
     * The android.jar for unit tests does not implement Size.
     */
    public static Size mockSize(int width, int height) {
        Size mock = mock(Size.class);
        when(mock.getWidth()).thenReturn(width);
        when(mock.getHeight()).thenReturn(height);
        return mock;
    }
}
