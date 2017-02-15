package com.bioid.authenticator.base.annotations;

import android.content.res.Configuration;
import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Denotes that the values of the annotated element should be one of the {@link Configuration} orientation constants.
 */
@IntDef({Configuration.ORIENTATION_PORTRAIT, Configuration.ORIENTATION_LANDSCAPE})
@Retention(RetentionPolicy.SOURCE)
public @interface ConfigurationOrientation {
}
