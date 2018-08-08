package com.bioid.authenticator.facialrecognition.enrollment;

import android.content.Context;
import android.support.annotation.VisibleForTesting;

import com.bioid.authenticator.base.logging.LoggingHelper;
import com.bioid.authenticator.base.logging.LoggingHelperFactory;
import com.bioid.authenticator.base.network.bioid.webservice.BioIdWebserviceClient;
import com.bioid.authenticator.base.network.bioid.webservice.MovementDirection;
import com.bioid.authenticator.base.network.bioid.webservice.token.EnrollmentToken;
import com.bioid.authenticator.base.network.bioid.webservice.token.EnrollmentTokenProvider;
import com.bioid.authenticator.base.threading.BackgroundHandler;
import com.bioid.authenticator.facialrecognition.FacialRecognitionBasePresenter;
import com.bioid.authenticator.facialrecognition.FacialRecognitionContract;
import com.bioid.authenticator.facialrecognition.FacialRecognitionFragment;

/**
 * Presenter for the {@link FacialRecognitionFragment} doing user enrollment.
 */
public class EnrollmentPresenter extends FacialRecognitionBasePresenter<EnrollmentToken> {

    private final EnrollmentTokenProvider tokenProvider;
    private final BioIdWebserviceClient bioIdWebserviceClient;

    public EnrollmentPresenter(Context ctx, FacialRecognitionContract.View view, EnrollmentTokenProvider tokenProvider) {
        super(ctx, LoggingHelperFactory.create(EnrollmentPresenter.class), view);

        this.tokenProvider = tokenProvider;
        this.bioIdWebserviceClient = new BioIdWebserviceClient();
    }

    @VisibleForTesting
    EnrollmentPresenter(Context ctx, LoggingHelper log, FacialRecognitionContract.View view, BackgroundHandler backgroundHandler,
                        EnrollmentTokenProvider tokenProvider, BioIdWebserviceClient bioIdWebserviceClient) {

        // using null dependencies makes sure the base class functionality won't be tested
        super(ctx, log, view, backgroundHandler, null, null, null);

        this.tokenProvider = tokenProvider;
        this.bioIdWebserviceClient = bioIdWebserviceClient;
    }

    @Override
    protected void startBiometricOperation() {
        log.d("startBiometricOperation()");

        view.showInitialisationInfo();

        backgroundHandler.runOnBackgroundThread(
                () -> tokenProvider.requestEnrollmentToken(ctx),
                token -> {
                    bwsToken = token;
                    failedOperations = 0;  // bound to token
                    log.d("using token: %s", bwsToken);

                    view.promptForEnrollmentProcessExplanation();
                }, e -> {
                    resetBiometricOperation();
                    showWarningOrError(e);
                },
                view::hideMessages);
    }

    @Override
    protected void onFaceDetected() {
        throw new IllegalStateException("onFaceDetected() called on EnrollmentPresenter");
    }

    @Override
    protected void onNoFaceDetected() {
        throw new IllegalStateException("onNoFaceDetected() called on EnrollmentPresenter");
    }

    @Override
    public void promptForProcessExplanationAccepted() {
        log.d("promptForProcessExplanationAccepted()");

        startEnrollmentProcess();
    }

    @Override
    public void promptForProcessExplanationRejected() {
        log.d("promptForProcessExplanationRejected()");

        view.navigateBack(false);
    }

    private void startEnrollmentProcess() {
        log.d("startEnrollmentProcess() [failedOperations=%d]", failedOperations);

        captureImagePair(0, MovementDirection.any, MovementDirection.any);
    }

    @Override
    public void promptToTurn90DegreesAccepted() {
        log.d("promptToTurn90DegreesAccepted()");

        captureImagePair(successfulUploads, MovementDirection.any, MovementDirection.any);
    }

    @Override
    public void promptToTurn90DegreesRejected() {
        log.d("promptToTurn90DegreesRejected()");

        view.navigateBack(false);
    }

    @Override
    protected void onImageWithMotionProcessed() {
        view.hideMovementIndicator();
    }

    @Override
    protected void onUploadSuccessful() {
        log.d("onUploadSuccessful() [successfulUploads=%d, failedUploads=%d]", ++successfulUploads, failedUploads);

        if (successfulUploads % 2 == 1) {
            log.d("waiting for second image upload to complete");
            return;
        }

        if (successfulUploads < 8) {
            log.i("entering next position");
            view.promptToTurn90Degrees();
            return;
        }

        // reference image and image with motion are uploaded
        enroll();
    }

    private void enroll() {
        log.d("enroll()");

        view.showEnrollingInfo();

        backgroundHandler.runOnBackgroundThread(
                () -> bioIdWebserviceClient.enroll(bwsToken),
                () -> {
                    log.i("enrollment successful");
                    view.showEnrollmentSuccess();
                    navigateBackWithDelay(true);
                }, e -> {
                    log.i("enrollment not successful");
                    showWarningOrError(e);
                    if (++failedOperations >= bwsToken.getMaxTries()) {
                        log.e("exceeded maximum number of failed operations (maxTries=%d)", bwsToken.getMaxTries());
                        navigateBackWithDelay(false);
                    } else {
                        retryWithDelay();
                    }
                },
                this::resetBiometricOperation);
    }

    private void retryWithDelay() {
        log.d("retryWithDelay()");

        backgroundHandler.runWithDelay(this::startEnrollmentProcess, DELAY_TO_RETRY_IN_MILLIS);
    }
}
