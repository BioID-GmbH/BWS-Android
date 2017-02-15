package com.bioid.authenticator.facialrecognition.verification;

import android.content.Context;
import android.renderscript.RenderScript;
import android.support.annotation.VisibleForTesting;

import com.bioid.authenticator.base.functional.Consumer;
import com.bioid.authenticator.base.functional.Supplier;
import com.bioid.authenticator.base.logging.LoggingHelper;
import com.bioid.authenticator.base.logging.LoggingHelperFactory;
import com.bioid.authenticator.base.network.bioid.webservice.BioIdWebserviceClient;
import com.bioid.authenticator.base.network.bioid.webservice.MovementDirection;
import com.bioid.authenticator.base.network.bioid.webservice.token.VerificationToken;
import com.bioid.authenticator.base.network.bioid.webservice.token.VerificationTokenProvider;
import com.bioid.authenticator.base.threading.BackgroundHandler;
import com.bioid.authenticator.facialrecognition.FacialRecognitionBasePresenter;
import com.bioid.authenticator.facialrecognition.FacialRecognitionContract;
import com.bioid.authenticator.facialrecognition.FacialRecognitionFragment;

/**
 * Presenter for the {@link FacialRecognitionFragment} doing user verification.
 */
public class VerificationPresenter extends FacialRecognitionBasePresenter<VerificationToken> {

    /**
     * This delay makes it more likely to have a stable auto focus and white balance setup.
     * Because the verification process does start as soon as a face was found, which can be very quickly, this delay was introduced.
     * Not having this delay could lead to false positives in the motion detection algorithm.
     */
    private static final int AUTO_FOCUS_AND_WHITE_BALANCE_DELAY_IN_MILLIS = 500;

    private final VerificationTokenProvider tokenProvider;
    private final BioIdWebserviceClient bioIdWebserviceClient;

    public VerificationPresenter(Context ctx, FacialRecognitionContract.View view, RenderScript rs,
                                 VerificationTokenProvider tokenProvider) {
        super(ctx, LoggingHelperFactory.create(VerificationPresenter.class), view, rs);
        this.tokenProvider = tokenProvider;
        this.bioIdWebserviceClient = new BioIdWebserviceClient(rs);
    }

    @VisibleForTesting
    VerificationPresenter(Context ctx, LoggingHelper log, FacialRecognitionContract.View view, BackgroundHandler backgroundHandler,
                          VerificationTokenProvider tokenProvider, BioIdWebserviceClient bioIdWebserviceClient) {

        // using null dependencies makes sure the base class functionality won't be tested
        super(ctx, log, view, backgroundHandler, null, null, null, null, null);

        this.tokenProvider = tokenProvider;
        this.bioIdWebserviceClient = bioIdWebserviceClient;
    }

    @Override
    protected void startBiometricOperation() {
        log.i("startBiometricOperation()");

        view.showInitialisationInfo();

        backgroundHandler.runWithDelay(new Runnable() {
            @Override
            public void run() {
                backgroundHandler.runOnBackgroundThread(new Supplier<VerificationToken>() {
                    @Override
                    public VerificationToken get() {
                        return tokenProvider.requestVerificationToken(ctx);
                    }
                }, new Consumer<VerificationToken>() {
                    @Override
                    public void accept(VerificationToken token) {
                        bwsToken = token;

                        detectFace();
                    }
                }, new Consumer<RuntimeException>() {
                    @Override
                    public void accept(RuntimeException e) {
                        resetBiometricOperation();
                        showWarningOrError(e);
                    }
                }, new Runnable() {
                    @Override
                    public void run() {
                        view.hideMessages();
                    }
                });
            }
        }, AUTO_FOCUS_AND_WHITE_BALANCE_DELAY_IN_MILLIS);
    }

    @Override
    protected void onFaceDetected() {
        log.d("onFaceDetected()");

        startVerificationProcess();
    }

    @Override
    protected void onNoFaceDetected() {
        log.d("onNoFaceDetected()");

        // start the verification process anyway, relying on the BioID server face detection
        startVerificationProcess();
    }

    private void startVerificationProcess() {
        log.d("startVerificationProcess() [failedOperations=%d]", failedOperations);

        captureImagePair(MovementDirection.any, MovementDirection.any);
    }

    @Override
    public void promptForProcessExplanationAccepted() {
        throw new IllegalStateException("promptForProcessExplanationAccepted() called on VerificationPresenter");
    }

    @Override
    public void promptForProcessExplanationRejected() {
        throw new IllegalStateException("promptForProcessExplanationRejected() called on VerificationPresenter");
    }

    @Override
    public void promptToTurn90DegreesAccepted() {
        throw new IllegalStateException("promptToTurn90DegreesAccepted() called on VerificationPresenter");
    }

    @Override
    public void promptToTurn90DegreesRejected() {
        throw new IllegalStateException("promptToTurn90DegreesRejected() called on VerificationPresenter");
    }

    @Override
    protected void onUploadSuccessful() {
        log.d("onUploadSuccessful() [successfulUploads=%d, failedUploads=%d]", successfulUploads, failedUploads);

        if (successfulUploads % 2 == 1) {
            log.d("waiting for second image upload to complete");
            return;
        }

        // reference image and image with motion are uploaded
        verify();
    }

    private void verify() {
        log.d("verify()");

        view.showVerifyingInfo();

        backgroundHandler.runOnBackgroundThread(new Runnable() {
            @Override
            public void run() {
                bioIdWebserviceClient.verify(bwsToken);
            }
        }, new Runnable() {
            @Override
            public void run() {
                log.i("verification successful");

                view.showVerificationSuccess();
                navigateBackWithDelay(true);
            }
        }, new Consumer<RuntimeException>() {
            @Override
            public void accept(RuntimeException e) {
                showWarningOrError(e);

                if (++failedOperations >= MAX_FAILED_OPERATIONS) {
                    navigateBackWithDelay(false);
                } else {
                    retryProcessWithDelay();
                }
            }
        }, new Runnable() {
            @Override
            public void run() {
                resetBiometricOperation();
            }
        });
    }

    private void retryProcessWithDelay() {
        log.d("retryProcessWithDelay()");

        backgroundHandler.runWithDelay(new Runnable() {
            @Override
            public void run() {
                startVerificationProcess();
            }
        }, DELAY_TO_RETRY_IN_MILLIS);
    }
}
