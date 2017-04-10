package com.bioid.authenticator.base.notification;

import android.app.Activity;
import android.content.DialogInterface;
import android.support.annotation.CheckResult;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v7.app.AlertDialog;

import com.bioid.authenticator.R;

/**
 * Can be used to instantiate commonly used dialogs.
 */
@SuppressWarnings({"SameParameterValue", "WeakerAccess"})
public class DialogHelper {

    protected final Activity activity;

    public DialogHelper(Activity activity) {
        this.activity = activity;
    }

    /**
     * Creates a dialog with a single button.
     *
     * @return fully configured dialog builder
     */
    @CheckResult
    public AlertDialog.Builder newDialog(@StringRes int title, @StringRes int message, @StringRes int buttonText,
                                         @DrawableRes int icon) {
        return new AlertDialog.Builder(activity)
                .setTitle(title)
                .setMessage(message)
                .setIcon(icon)
                .setPositiveButton(buttonText, null);
    }

    /**
     * Creates a dialog with a single button.
     *
     * @param onDismiss callback which is called if the user clicks on the button or presses back
     * @return fully configured dialog builder
     */
    @CheckResult
    public AlertDialog.Builder newDialog(@StringRes int title, @StringRes int message, @StringRes int buttonText,
                                         @DrawableRes int icon, @NonNull final Runnable onDismiss) {
        return newDialog(title, message, buttonText, icon)
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        // using "onDismiss" because it is called even if the user does press the back button
                        onDismiss.run();
                    }
                });
    }

    /**
     * Creates a transparent dialog with a single button.
     *
     * @param onConfirm callback which is called if the user clicks on the button
     * @param onDenial  callback which is called if the user presses back or outside the dialog
     * @return fully configured dialog builder
     */
    @CheckResult
    public AlertDialog.Builder newTransparentDialog(@StringRes int title, @StringRes int message, @StringRes int buttonText,
                                                    @NonNull final Runnable onConfirm,
                                                    @NonNull final Runnable onDenial) {
        return new AlertDialog.Builder(activity, R.style.TransparentDialogTheme)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(buttonText, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        onConfirm.run();
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialogInterface) {
                        onDenial.run();
                    }
                });
    }
}
