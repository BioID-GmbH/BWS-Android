package com.bioid.authenticator.facialrecognition.verification;

import android.content.Context;
import android.support.annotation.VisibleForTesting;

import com.bioid.authenticator.base.logging.LoggingHelper;
import com.bioid.authenticator.base.logging.LoggingHelperFactory;
import com.bioid.authenticator.base.network.bioid.webservice.BioIdWebserviceClient;
import com.bioid.authenticator.base.network.bioid.webservice.LiveDetectionException;
import com.bioid.authenticator.base.network.bioid.webservice.MovementDirection;
import com.bioid.authenticator.base.network.bioid.webservice.NoFaceFoundException;
import com.bioid.authenticator.base.network.bioid.webservice.token.VerificationToken;
import com.bioid.authenticator.base.network.bioid.webservice.token.VerificationTokenProvider;
import com.bioid.authenticator.base.threading.BackgroundHandler;
import com.bioid.authenticator.facialrecognition.FacialRecognitionBasePresenter;
import com.bioid.authenticator.facialrecognition.FacialRecognitionContract;
import com.bioid.authenticator.facialrecognition.FacialRecognitionFragment;
import com.bioid.authenticator.facialrecognition.MotionDetection;

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
    private static final int DELAY_TO_CONTINUE_WITHIN_CHALLENGE_IN_MILLIS = 2_000;

    @VisibleForTesting
    int nextPairForChallenge;

    private final VerificationTokenProvider tokenProvider;
    private final BioIdWebserviceClient bioIdWebserviceClient;

    public VerificationPresenter(Context ctx, FacialRecognitionContract.View view, VerificationTokenProvider tokenProvider) {
        super(ctx, LoggingHelperFactory.create(VerificationPresenter.class), view);
        this.tokenProvider = tokenProvider;
        this.bioIdWebserviceClient = new BioIdWebserviceClient();
    }

    @VisibleForTesting
    VerificationPresenter(Context ctx, LoggingHelper log, FacialRecognitionContract.View view, BackgroundHandler backgroundHandler,
                          MotionDetection motionDetection, VerificationTokenProvider tokenProvider,
                          BioIdWebserviceClient bioIdWebserviceClient) {

        // using null dependencies makes sure the base class functionality won't be tested (MotionDetection is used in reset)
        super(ctx, log, view, backgroundHandler, null, motionDetection, null);

        this.tokenProvider = tokenProvider;
        this.bioIdWebserviceClient = bioIdWebserviceClient;
    }

    @Override
    protected void startBiometricOperation() {
        log.i("startBiometricOperation()");

        view.showInitialisationInfo();

        backgroundHandler.runWithDelay(() -> backgroundHandler.runOnBackgroundThread(
                () -> tokenProvider.requestVerificationToken(ctx),
                token -> {
                    bwsToken = token;
                    failedOperations = 0;  // bound to token
                    log.d("using token: %s", bwsToken);

                    detectFace();
                }, e -> {
                    resetBiometricOperation();
                    showWarningOrError(e);
                }, view::hideMessages)
                , AUTO_FOCUS_AND_WHITE_BALANCE_DELAY_IN_MILLIS);
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

        if (bwsToken.isChallengeResponse()) {
            // challenges example: [ [left, right, down], [down, up, right], [up, down, left] ]
            captureImagePair(0, MovementDirection.any, getCurrentChallenge()[0]);
            nextPairForChallenge = 1;  // now pointing to the start of the next image pair of the challenge
        } else {
            captureImagePair(0, MovementDirection.any, MovementDirection.any);
        }
    }

    private MovementDirection[] getCurrentChallenge() {
        return bwsToken.getChallenges()[failedOperations];
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
    protected void onImageWithMotionProcessed() {
        if (!bwsToken.isChallengeResponse()) {
            view.hideMovementIndicator();
            return;
        }

        MovementDirection[] currentChallenge = getCurrentChallenge();

        if (nextPairForChallenge == currentChallenge.length) {
            // now uploading last image pair
            view.hideMovementIndicator();
            return;
        }

        // challenge does require further images
        view.resetMovementIndicator();

        final int nextIndex = nextPairForChallenge + 1;  // using +1 because the first (any) image is not specified within challenge
        final MovementDirection nextCurrentDirection = currentChallenge[nextPairForChallenge];
        final MovementDirection nextTargetDirection = currentChallenge[nextPairForChallenge + 1];
        backgroundHandler.runWithDelay(
                () -> captureImagePair(nextIndex, nextCurrentDirection, nextTargetDirection)
                , DELAY_TO_CONTINUE_WITHIN_CHALLENGE_IN_MILLIS);

        nextPairForChallenge += 2;
    }

    @Override
    protected void resetBiometricOperation() {
        super.resetBiometricOperation();

        this.nextPairForChallenge = 0;
    }

    @Override
    protected void onUploadSuccessful() {
        log.d("onUploadSuccessful() [successfulUploads=%d, failedUploads=%d]", ++successfulUploads, failedUploads);

        if (successfulUploads % 2 == 1) {
            log.d("waiting for second image upload to complete");
            return;
        }

        // reference image and image with motion are uploaded
        if (bwsToken.isChallengeResponse()) {
            MovementDirection[] currentChallenge = getCurrentChallenge();
            if (successfulUploads == currentChallenge.length + 1) {
                // no more image pairs within challenge
                // using +1 because the first image with direction "any" is never specified in the challenge
                verify();
            } else {
                log.d("waiting for other image uploads from challenge to complete");
            }
        } else {
            // no challenge response -> one image pair is enough
            verify();
        }
    }

    @Override
    protected void onUploadFailed(RuntimeException e) {
        if (bwsToken.isChallengeResponse() && (e instanceof NoFaceFoundException || e instanceof LiveDetectionException)) {
            // capturing the image pair again would not work because of the current challenge
            // -> complete the current challenge even if it will fail for sure
            // -> after unsuccessful verification the user can try again with the next challenge
            log.w("got %s during Challenge-Response, proceeding with challenge", e.getClass().getSimpleName());
            onUploadSuccessful();
        } else {
            super.onUploadFailed(e);
        }
    }

    @VisibleForTesting
    void verify() {
        log.d("verify()");

        view.showVerifyingInfo();

        backgroundHandler.runOnBackgroundThread(() -> bioIdWebserviceClient.verify(bwsToken), () -> {
            log.i("verification successful");

            view.showVerificationSuccess();
            navigateBackWithDelay(true);
        }, e -> {
            log.i("verification not successful");
            showWarningOrError(e);

            if (++failedOperations >= bwsToken.getMaxTries()) {
                log.e("exceeded maximum number of failed operations (maxTries=%d)", bwsToken.getMaxTries());
                navigateBackWithDelay(false);
            } else {
                retryProcessWithDelay();
            }
        }, this::resetBiometricOperation);
    }

    private void retryProcessWithDelay() {
        log.d("retryProcessWithDelay()");

        backgroundHandler.runWithDelay(this::startVerificationProcess, DELAY_TO_RETRY_IN_MILLIS);
    }
}
