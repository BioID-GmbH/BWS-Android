package com.bioid.authenticator.facialrecognition.enrollment;

import android.app.Fragment;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;

import com.bioid.authenticator.R;
import com.bioid.authenticator.base.network.bioid.webservice.token.EnrollmentTokenProvider;
import com.bioid.authenticator.facialrecognition.FacialRecognitionFragment;

/**
 * The EnrollmentActivity is the entry point of the user enrollment process.
 * <p/>
 * Make sure to add an instance of {@link EnrollmentTokenProvider} using {@link #EXTRA_TOKEN_PROVIDER} to the starting intent!
 * <p/>
 * The "resultCode" returned by this activity if started via {@link #startActivityForResult(Intent, int)} will be {@link #RESULT_OK}
 * (enrollment successful) or {@link #RESULT_CANCELED} (enrollment not successful).
 */
final public class EnrollmentActivity extends AppCompatActivity {

    /**
     * Key of the extra for the {@link EnrollmentTokenProvider}.
     */
    public static final String EXTRA_TOKEN_PROVIDER = "token_provider";

    @VisibleForTesting
    public static final String TAG_FACIAL_RECOGNITION_FRAGMENT = "FacialRecognitionFragment";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        DataBindingUtil.setContentView(this, R.layout.activity_facial_recognition);

        EnrollmentTokenProvider tokenProvider = getIntent().getParcelableExtra(EXTRA_TOKEN_PROVIDER);
        if (tokenProvider == null) {
            throw new IllegalStateException("missing or invalid extra: " + EXTRA_TOKEN_PROVIDER);
        }

        setupFragment(tokenProvider);
    }

    private void setupFragment(@NonNull EnrollmentTokenProvider tokenProvider) {
        Fragment facialRecognitionFragment = getFragmentManager().findFragmentByTag(TAG_FACIAL_RECOGNITION_FRAGMENT);
        if (facialRecognitionFragment == null) {
            // fragment is not retained
            FacialRecognitionFragment fragment = FacialRecognitionFragment.newInstanceForEnrollment(tokenProvider);
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.add(R.id.frame_layout, fragment, TAG_FACIAL_RECOGNITION_FRAGMENT);
            transaction.commit();
        }
    }
}
