package com.bioid.authenticator.base.annotations;

import android.support.annotation.IntDef;

import com.google.android.gms.vision.Frame;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Denotes that the values of the annotated element should be one of the {@link Frame} rotation constants.
 */
@IntDef({Frame.ROTATION_0, Frame.ROTATION_90, Frame.ROTATION_180, Frame.ROTATION_270})
@Retention(RetentionPolicy.SOURCE)
public @interface FrameRotation {
}
