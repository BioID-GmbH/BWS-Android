package com.bioid.authenticator.facialrecognition.verification;

import android.app.Fragment;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;

import com.bioid.authenticator.R;
import com.bioid.authenticator.base.network.bioid.webservice.token.VerificationTokenProvider;
import com.bioid.authenticator.facialrecognition.FacialRecognitionFragment;

/**
 * The VerificationActivity is the entry point of the user verification process.
 * <p/>
 * Make sure to add an instance of {@link VerificationTokenProvider} using {@link #EXTRA_TOKEN_PROVIDER} to the starting intent!
 * <p/>
 * The "resultCode" returned by this activity if started via {@link #startActivityForResult(Intent, int)} will be {@link #RESULT_OK}
 * (verification successful) or {@link #RESULT_CANCELED} (verification not successful).
 */
final public class VerificationActivity extends AppCompatActivity {

    /**
     * Key of the extra for the {@link VerificationTokenProvider}.
     */
    public static final String EXTRA_TOKEN_PROVIDER = "token_provider";

    @VisibleForTesting
    public static final String TAG_FACIAL_RECOGNITION_FRAGMENT = "FacialRecognitionFragment";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        DataBindingUtil.setContentView(this, R.layout.activity_facial_recognition);

        VerificationTokenProvider tokenProvider = getIntent().getParcelableExtra(EXTRA_TOKEN_PROVIDER);
        if (tokenProvider == null) {
            throw new IllegalStateException("missing or invalid extra: " + EXTRA_TOKEN_PROVIDER);
        }

        setupFragment(tokenProvider);
    }

    private void setupFragment(@NonNull VerificationTokenProvider tokenProvider) {
        Fragment facialRecognitionFragment = getFragmentManager().findFragmentByTag(TAG_FACIAL_RECOGNITION_FRAGMENT);
        if (facialRecognitionFragment == null) {
            // fragment is not retained
            FacialRecognitionFragment fragment = FacialRecognitionFragment.newInstanceForVerification(tokenProvider);
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.add(R.id.frame_layout, fragment, TAG_FACIAL_RECOGNITION_FRAGMENT);
            transaction.commit();
        }
    }
}

