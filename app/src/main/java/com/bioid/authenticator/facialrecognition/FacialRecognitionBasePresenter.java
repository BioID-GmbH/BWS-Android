package com.bioid.authenticator.facialrecognition;

import android.content.Context;
import android.support.annotation.CallSuper;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import com.bioid.authenticator.base.image.Yuv420Image;
import com.bioid.authenticator.base.logging.LoggingHelper;
import com.bioid.authenticator.base.network.NoConnectionException;
import com.bioid.authenticator.base.network.ServerErrorException;
import com.bioid.authenticator.base.network.bioid.webservice.BioIdWebserviceClient;
import com.bioid.authenticator.base.network.bioid.webservice.ChallengeResponseException;
import com.bioid.authenticator.base.network.bioid.webservice.DeviceNotRegisteredException;
import com.bioid.authenticator.base.network.bioid.webservice.LiveDetectionException;
import com.bioid.authenticator.base.network.bioid.webservice.MovementDirection;
import com.bioid.authenticator.base.network.bioid.webservice.MultipleFacesFoundException;
import com.bioid.authenticator.base.network.bioid.webservice.NoEnrollmentException;
import com.bioid.authenticator.base.network.bioid.webservice.NoFaceFoundException;
import com.bioid.authenticator.base.network.bioid.webservice.NoSamplesException;
import com.bioid.authenticator.base.network.bioid.webservice.NotRecognizedException;
import com.bioid.authenticator.base.network.bioid.webservice.WrongCredentialsException;
import com.bioid.authenticator.base.network.bioid.webservice.token.BwsToken;
import com.bioid.authenticator.base.threading.AsynchronousBackgroundHandler;
import com.bioid.authenticator.base.threading.BackgroundHandler;

/**
 * Base presenter to avoid code duplication between different implementations of {@link FacialRecognitionContract.Presenter}.
 * <p/>
 * If an implantation does offload work to a background thread it should use the {@link #backgroundHandler} from the base class!
 */
public abstract class FacialRecognitionBasePresenter<T extends BwsToken> implements FacialRecognitionContract.Presenter {

    protected static final int DELAY_TO_RETRY_IN_MILLIS = 3_000;

    private static final int MAX_FAILED_UPLOADS = 3;
    private static final int MOTION_TIMEOUT_IN_MILLIS = 12_000;
    private static final int FACE_TIMEOUT_IN_MILLIS = 4_000;
    private static final int DELAY_TO_NAVIGATE_BACK_IN_MILLIS = 3_000;
    private static final int DELAY_TO_CHECK_FOR_MOTION_IN_MILLIS = 1_000;

    protected final Context ctx;
    protected final LoggingHelper log;
    protected final FacialRecognitionContract.View view;
    protected final BackgroundHandler backgroundHandler;

    private final FaceDetection faceDetection;
    private final MotionDetection motionDetection;
    private final BioIdWebserviceClient bioIdWebserviceClient;

    protected T bwsToken;
    protected int failedOperations;
    protected int successfulUploads;
    protected int failedUploads;

    @VisibleForTesting
    int index;
    @VisibleForTesting
    PermissionState permissionState = PermissionState.UNKNOWN;
    @VisibleForTesting
    ImageDetectionState imageDetectionState = ImageDetectionState.OTHER;
    @VisibleForTesting
    MovementDirection currentDirection, destinationDirection;
    @VisibleForTesting
    Integer taskIdMotionTimeout;
    @VisibleForTesting
    Integer taskIdFaceTimeout;

    protected FacialRecognitionBasePresenter(Context ctx, LoggingHelper log, FacialRecognitionContract.View view) {
        this.ctx = ctx;
        this.log = log;
        this.view = view;
        this.backgroundHandler = new AsynchronousBackgroundHandler();
        this.faceDetection = new FaceDetection(ctx);
        this.motionDetection = new MotionDetection();
        this.bioIdWebserviceClient = new BioIdWebserviceClient();
    }

    @VisibleForTesting
    protected FacialRecognitionBasePresenter(Context ctx, LoggingHelper log, FacialRecognitionContract.View view,
                                             BackgroundHandler backgroundHandler, FaceDetection faceDetection,
                                             MotionDetection motionDetection, BioIdWebserviceClient bioIdWebserviceClient) {
        this.ctx = ctx;
        this.log = log;
        this.view = view;
        this.backgroundHandler = backgroundHandler;
        this.faceDetection = faceDetection;
        this.motionDetection = motionDetection;
        this.bioIdWebserviceClient = bioIdWebserviceClient;
    }

    @CallSuper
    @Override
    public void onResume() {
        if (permissionState == PermissionState.UNKNOWN) {
            // Try to obtain the permission if not granted.
            permissionState = PermissionState.REQUESTING_PERMISSION;
            view.requestCameraPermission();

        } else if (permissionState == PermissionState.PERMISSION_DENIED) {
            // If the user rotates the device while the fragment is going to be destroyed the process should be triggered again.
            onCameraPermissionDenied();
        }
    }

    @CallSuper
    @Override
    public void onPause() {
        if (permissionState == PermissionState.PERMISSION_GRANTED) {
            // Because of Android RuntimePermissions the user could revoke permissions while the fragment is paused.
            permissionState = PermissionState.UNKNOWN;
        }

        resetBiometricOperation();

        view.stopPreview();
    }

    @CallSuper
    @Override
    public void onCameraPermissionGranted() {
        permissionState = PermissionState.PERMISSION_GRANTED;

        view.startPreview();

        startBiometricOperation();
    }

    @CallSuper
    @Override
    public void onCameraPermissionDenied() {
        permissionState = PermissionState.PERMISSION_DENIED;

        view.showCameraPermissionErrorAndNavigateBack();
    }

    /**
     * Start the biometric operation by obtaining and setting the {@link BwsToken}.
     */
    protected abstract void startBiometricOperation();

    /**
     * Tries to detect a human face within a certain time frame.
     */
    @CallSuper
    protected void detectFace() {
        log.d("detectFace()");

        if (!faceDetection.isOperational()) {
            log.w("face detection is not operational (binaries needed for Google Mobile Vision API are not downloaded yet)");
            onNoFaceDetected();
            return;
        }

        view.showFindFaceInfo();

        // now waiting for images with face using timeout
        taskIdFaceTimeout = backgroundHandler.runWithDelay(() -> {
            log.w("face timeout occurred after %d ms", FACE_TIMEOUT_IN_MILLIS);
            imageDetectionState = ImageDetectionState.OTHER;
            view.hideMessages();
            onNoFaceDetected();
        }, FACE_TIMEOUT_IN_MILLIS);

        imageDetectionState = ImageDetectionState.WAITING_FOR_IMAGE_WITH_FACE;
    }

    /**
     * Will be called if at least one prominent face was detected.
     */
    protected abstract void onFaceDetected();

    /**
     * Will be called if no face was detected.
     */
    protected abstract void onNoFaceDetected();

    /**
     * Initiates the process of capturing a pair of images.
     * The user will be prompted to follow the movement direction instructions.
     * A motion detection algorithm will be used to check for the live detection requirements.
     *
     * @param index                the index to use for image uploads
     *                             index will be used for the reference image and index+1 for the image with motion
     * @param currentDirection     movement direction of the first image
     * @param destinationDirection movement direction of the second image
     */
    @CallSuper
    @SuppressWarnings("SameParameterValue")
    protected void captureImagePair(@IntRange(from = 0) int index,
                                    @NonNull MovementDirection currentDirection, @NonNull MovementDirection destinationDirection) {
        log.d("captureImagePair(index=%d, currentDirection=%s, destinationDirection=%s)",
                index, currentDirection, destinationDirection);

        this.index = index;
        this.currentDirection = currentDirection;
        this.destinationDirection = destinationDirection;

        view.showMovementInfo(destinationDirection);
        view.showMovementIndicator(destinationDirection);

        imageDetectionState = ImageDetectionState.WAITING_FOR_REFERENCE_IMAGE;
    }

    @CallSuper
    @Override
    public void onImageCaptured(@NonNull Yuv420Image img) {
        switch (imageDetectionState) {
            case WAITING_FOR_IMAGE_WITH_FACE:
                // do not process any new images while background operations are still running
                imageDetectionState = ImageDetectionState.OTHER;
                onPotentialImageWithFaceCaptured(img);
                break;

            case WAITING_FOR_REFERENCE_IMAGE:
                // do not process any new images while movement instructions are shown (challenge-response only)
                imageDetectionState = ImageDetectionState.OTHER;
                onReferenceImageCaptured(img);
                break;

            case WAITING_FOR_IMAGE_WITH_MOTION:
                // do not process any new images while background operations are still running
                imageDetectionState = ImageDetectionState.OTHER;
                onPotentialImageWithMotionCaptured(img);
                break;

            default:
                // nothing to do
        }
    }

    private void onPotentialImageWithFaceCaptured(@NonNull final Yuv420Image img) {
        // check for potential face in the image within the background to keep the UI responsive
        backgroundHandler.runOnBackgroundThread(
                () -> faceDetection.containsFace(img),
                faceDetected -> {
                    if (faceDetected) {
                        onImageWithFaceCaptured();
                    } else {
                        // again waiting for next potential image with face
                        imageDetectionState = ImageDetectionState.WAITING_FOR_IMAGE_WITH_FACE;
                    }
                }, e -> {
                    throw e;  // should lead to app crash
                }, null);
    }

    private void onImageWithFaceCaptured() {
        view.hideMessages();
        backgroundHandler.cancelScheduledTask(taskIdFaceTimeout);
        onFaceDetected();
    }

    private void onReferenceImageCaptured(@NonNull final Yuv420Image img) {
        log.d("onReferenceImageCaptured(img=%s)", img);

        // create motion detection template within the background to keep the UI responsive
        backgroundHandler.runOnBackgroundThread(
                () -> motionDetection.createTemplate(img),
                () -> {
                    uploadImage(img, currentDirection, index, false);

                    backgroundHandler.runWithDelay(() -> {
                        // waiting for images with motion using timeout
                        imageDetectionState = ImageDetectionState.WAITING_FOR_IMAGE_WITH_MOTION;
                        setupMotionTimeout();
                    }, DELAY_TO_CHECK_FOR_MOTION_IN_MILLIS);
                }, e -> {
                    throw e;  // should lead to app crash
                }, null);
    }

    @VisibleForTesting
    void setupMotionTimeout() {
        taskIdMotionTimeout = backgroundHandler.runWithDelay(() -> {
            log.w("motion timeout occurred after %d ms", MOTION_TIMEOUT_IN_MILLIS);
            resetBiometricOperation();
            view.showMotionDetectionWarning();
            navigateBackWithDelay(false);
        }, MOTION_TIMEOUT_IN_MILLIS);
    }

    private void onPotentialImageWithMotionCaptured(@NonNull final Yuv420Image img) {
        // check for potential motion in the image within the background to keep the UI responsive
        backgroundHandler.runOnBackgroundThread(
                () -> motionDetection.detect(img),
                motionDetected -> {
                    if (motionDetected) {
                        onImageWithMotionCaptured(img);
                    } else {
                        // again waiting for next potential image with motion
                        imageDetectionState = ImageDetectionState.WAITING_FOR_IMAGE_WITH_MOTION;
                    }
                }, e -> {
                    throw e;  // should lead to app crash
                }, null);
    }

    private void onImageWithMotionCaptured(@NonNull final Yuv420Image img) {
        log.d("onImageWithMotionCaptured(img=%s)", img);

        // cancel motion timeout and hide movement instruction text
        backgroundHandler.cancelScheduledTask(taskIdMotionTimeout);
        view.hideMessages();

        // uploading image with motion
        uploadImage(img, destinationDirection, index + 1, true);

        onImageWithMotionProcessed();
    }


    /**
     * Will be called when a image with motion has been processed completely and is currently uploading.
     */
    protected abstract void onImageWithMotionProcessed();

    private void uploadImage(final Yuv420Image img, final MovementDirection direction, final int index,
                             final boolean showUploadingInfo) {
        log.d("uploadImage(img=%s, direction=%s, index=%s, showUploadingInfo=%s)", img, direction, index, showUploadingInfo);

        if (showUploadingInfo) {
            view.showUploadingImagesInfo();
            view.showLoadingIndicator();
        }

        backgroundHandler.runOnBackgroundThread(
                () -> bioIdWebserviceClient.uploadImage(img, bwsToken, direction, index),
                this::onUploadSuccessful,
                this::onUploadFailed,
                () -> {
                    if (showUploadingInfo) {
                        view.hideMessages();
                        view.hideLoadingIndicator();
                    }
                });
    }

    /**
     * Will be called on every successful image upload.
     * Called once for the reference image and again for the image with motion.
     */
    protected abstract void onUploadSuccessful();

    /**
     * Will be called on every failed image upload.
     *
     * @param e the Exception thrown by {@link BioIdWebserviceClient#uploadImage(Yuv420Image, BwsToken, MovementDirection, int)}.
     */
    protected void onUploadFailed(RuntimeException e) {
        log.w("onUploadFailed() [successfulUploads=%d, failedUploads=%d]", successfulUploads, ++failedUploads);

        if (failedUploads >= MAX_FAILED_UPLOADS) {
            log.e("exceeded maximum number of failed uploads (MAX_FAILED_UPLOAD=%d)", MAX_FAILED_UPLOADS);
            resetBiometricOperation();
            showWarningOrError(e);
            navigateBackWithDelay(false);
            return;
        }

        final int indexForRetry = index;
        final MovementDirection currentDirectionForRetry = currentDirection;
        final MovementDirection destinationDirectionForRetry = destinationDirection;

        resetCaptureImagePair();
        if (successfulUploads % 2 == 1) {
            successfulUploads--;  // in case of failed image with motion
        }

        showWarningOrError(e);
        backgroundHandler.runWithDelay(
                () -> captureImagePair(indexForRetry, currentDirectionForRetry, destinationDirectionForRetry),
                DELAY_TO_RETRY_IN_MILLIS);
    }

    /**
     * Does reset the views and the presenters state regarding the process of capturing a pair of images.
     * (see also {@link #resetBiometricOperation()})
     */
    @VisibleForTesting
    void resetCaptureImagePair() {
        log.d("resetCaptureImagePair()");

        // unsubscribe from eventually still running tasks
        backgroundHandler.unsubscribeFromAllBackgroundTasks();

        // cancel eventually scheduled tasks
        backgroundHandler.cancelAllScheduledTasks();

        // reset motion detection template
        motionDetection.resetTemplate();

        // reset presenter
        // (do not reset "permissionState" because this is not related to the biometric operation)
        imageDetectionState = ImageDetectionState.OTHER;
        index = 0;
        currentDirection = null;
        destinationDirection = null;
        taskIdMotionTimeout = null;
        taskIdFaceTimeout = null;

        // reset ui
        view.hideLoadingIndicator();
        view.hideMovementIndicator();
    }

    /**
     * Does reset the presenters state regarding the whole biometric operation.
     * (see also {@link #resetCaptureImagePair()})
     */
    @CallSuper
    protected void resetBiometricOperation() {
        log.d("resetBiometricOperation()");

        resetCaptureImagePair();

        // reset presenter
        // (do not reset "bwsToken" and "failedOperations" because the token can be used for multiple retries)
        successfulUploads = 0;
        failedUploads = 0;

        // reset ui
        view.hideMessages();
    }

    /**
     * Shows a warning or error message to the user.
     *
     * @param e Warning messages are displayed for instances of {@link NotRecognizedException}, {@link ChallengeResponseException},
     *          {@link LiveDetectionException}, {@link NoFaceFoundException} and {@link MultipleFacesFoundException}.
     *          Error messages are displayed for instances of {@link NoConnectionException}, {@link ServerErrorException},
     *          {@link WrongCredentialsException} and {@link NoEnrollmentException}.
     *          All other exceptions will be rethrown.
     */
    @CallSuper
    protected void showWarningOrError(RuntimeException e) {
        log.w("showWarningOrError(e=%s)", e.getClass().getSimpleName());

        if (e instanceof NotRecognizedException) {
            view.showNotRecognizedWarning();
            return;
        }
        if (e instanceof ChallengeResponseException) {
            view.showChallengeResponseWarning();
            return;
        }
        if (e instanceof LiveDetectionException) {
            view.showLiveDetectionWarning();
            return;
        }
        if (e instanceof NoFaceFoundException) {
            view.showNoFaceFoundWarning();
            return;
        }
        if (e instanceof MultipleFacesFoundException) {
            view.showMultipleFacesFoundWarning();
            return;
        }
        if (e instanceof NoSamplesException) {
            view.showNoSamplesWarning();
            return;
        }
        if (e instanceof NoConnectionException) {
            view.showNoConnectionErrorAndNavigateBack();
            return;
        }
        if (e instanceof ServerErrorException) {
            view.showServerErrorAndNavigateBack();
            return;
        }
        if (e instanceof DeviceNotRegisteredException) {
            view.showDeviceNotRegisteredErrorAndNavigateBack();
            return;
        }
        if (e instanceof WrongCredentialsException) {
            view.showWrongCredentialsErrorAndNavigateBack();
            return;
        }
        if (e instanceof NoEnrollmentException) {
            view.showNoEnrollmentErrorAndNavigateBack();
            return;
        }

        throw e;  // unhandled exception should lead to app crash
    }

    /**
     * Does navigate back after a short delay which does enable the user to read the error or success message.
     *
     * @param success Used in the activity result to express if the operation has succeeded.
     */
    @CallSuper
    protected void navigateBackWithDelay(final boolean success) {
        backgroundHandler.runWithDelay(() -> view.navigateBack(success), DELAY_TO_NAVIGATE_BACK_IN_MILLIS);
    }

    @VisibleForTesting
    enum PermissionState {
        UNKNOWN,
        REQUESTING_PERMISSION,
        PERMISSION_GRANTED,
        PERMISSION_DENIED
    }

    @VisibleForTesting
    enum ImageDetectionState {
        OTHER,
        WAITING_FOR_IMAGE_WITH_FACE,
        WAITING_FOR_REFERENCE_IMAGE,
        WAITING_FOR_IMAGE_WITH_MOTION
    }
}
