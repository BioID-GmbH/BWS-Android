package com.bioid.authenticator.base.annotations;

import android.support.annotation.IntDef;
import android.view.Surface;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Denotes that the values of the annotated element should be one of the {@link Surface} rotation constants.
 */
@IntDef({Surface.ROTATION_0, Surface.ROTATION_90, Surface.ROTATION_180, Surface.ROTATION_270})
@Retention(RetentionPolicy.SOURCE)
public @interface SurfaceRotation {
}
