package com.bioid.authenticator.base.notification;

import android.app.Activity;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v7.app.AlertDialog;

import com.bioid.authenticator.R;

/**
 * Can be used to instantiate and show commonly used dialogs.
 */
@SuppressWarnings({"SameParameterValue", "WeakerAccess"})
public class DialogHelper {

    protected final Activity activity;

    public DialogHelper(Activity activity) {
        this.activity = activity;
    }

    /**
     * Creates and shows a dialog with a single button.
     */
    public void showNewDialog(@StringRes int title, @StringRes int message, @StringRes int buttonText,
                              @DrawableRes int icon) {
        if (activity == null || activity.isFinishing()) return;

        new AlertDialog.Builder(activity)
                .setTitle(title)
                .setMessage(message)
                .setIcon(icon)
                .setPositiveButton(buttonText, null)
                .show();
    }

    /**
     * Creates and shows a dialog with a single button.
     *
     * @param onDismiss callback which is called if the user clicks on the button or presses back
     */
    public void showNewDialog(@StringRes int title, @StringRes int message, @StringRes int buttonText,
                              @DrawableRes int icon, @NonNull final Runnable onDismiss) {
        if (activity == null || activity.isFinishing()) return;

        new AlertDialog.Builder(activity)
                .setTitle(title)
                .setMessage(message)
                .setIcon(icon)
                .setPositiveButton(buttonText, null)
                .setOnDismissListener(dialog -> {
                    // using "onDismiss" because it is called even if the user does press the back button
                    onDismiss.run();
                })
                .show();
    }

    /**
     * Creates and shows a transparent dialog with a single button.
     *
     * @param onConfirm callback which is called if the user clicks on the button
     * @param onDenial  callback which is called if the user presses back or outside the dialog
     */
    public void showNewTransparentDialog(@StringRes int title, @StringRes int message, @StringRes int buttonText,
                                         @NonNull final Runnable onConfirm,
                                         @NonNull final Runnable onDenial) {
        if (activity == null || activity.isFinishing()) return;

        new AlertDialog.Builder(activity, R.style.TransparentDialogTheme)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(buttonText, (dialog, which) -> onConfirm.run())
                .setOnCancelListener(dialogInterface -> onDenial.run())
                .show();
    }
}
