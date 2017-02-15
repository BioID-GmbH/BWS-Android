package com.bioid.authenticator.facialrecognition;

import android.content.Context;
import android.graphics.Bitmap;
import android.renderscript.RSIllegalArgumentException;

import com.bioid.authenticator.base.annotations.Rotation;
import com.bioid.authenticator.base.image.GrayscaleImage;
import com.bioid.authenticator.base.image.ImageFormatConverter;
import com.bioid.authenticator.base.image.ImageTransformer;
import com.bioid.authenticator.base.image.Yuv420Image;
import com.bioid.authenticator.base.logging.LoggingHelper;
import com.bioid.authenticator.base.network.NoConnectionException;
import com.bioid.authenticator.base.network.ServerErrorException;
import com.bioid.authenticator.base.network.TechnicalException;
import com.bioid.authenticator.base.network.bioid.webservice.BioIdWebserviceClient;
import com.bioid.authenticator.base.network.bioid.webservice.ChallengeResponseException;
import com.bioid.authenticator.base.network.bioid.webservice.LiveDetectionException;
import com.bioid.authenticator.base.network.bioid.webservice.MovementDirection;
import com.bioid.authenticator.base.network.bioid.webservice.MultipleFacesFoundException;
import com.bioid.authenticator.base.network.bioid.webservice.NoEnrollmentException;
import com.bioid.authenticator.base.network.bioid.webservice.NoFaceFoundException;
import com.bioid.authenticator.base.network.bioid.webservice.NotRecognizedException;
import com.bioid.authenticator.base.network.bioid.webservice.WrongCredentialsException;
import com.bioid.authenticator.base.network.bioid.webservice.token.BwsToken;
import com.bioid.authenticator.base.network.bioid.webservice.token.VerificationToken;
import com.bioid.authenticator.base.threading.BackgroundHandler;
import com.bioid.authenticator.facialrecognition.FacialRecognitionBasePresenter.ImageDetectionState;
import com.bioid.authenticator.facialrecognition.FacialRecognitionBasePresenter.PermissionState;
import com.bioid.authenticator.testutil.SynchronousBackgroundHandler;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class FacialRecognitionBasePresenterTest {

    @Rotation
    private static final int IMAGE_ROTATION = 180;
    private static final VerificationToken BWS_TOKEN = new VerificationToken("could be any BwsToken");
    private static final MovementDirection CURRENT_DIRECTION = MovementDirection.any;
    private static final MovementDirection DESTINATION_DIRECTION = MovementDirection.left;
    private static final int TASK_ID_MOTION_TIMEOUT = 99;
    private static final int TASK_ID_FACE_TIMEOUT = 88;
    private static final int SUCCESSFUL_IMAGE_UPLOADS = 2;
    private static final int FAILED_IMAGE_UPLOADS = 1;

    @Mock
    private Context ctx;
    @Mock
    private LoggingHelper log;
    @Mock
    private FacialRecognitionContract.View view;
    @Spy
    private SynchronousBackgroundHandler backgroundHandler;
    @Mock
    private ImageFormatConverter imageFormatConverter;
    @Mock
    private ImageTransformer imageTransformer;
    @Mock
    private FaceDetection faceDetection;
    @Mock
    private MotionDetection motionDetection;
    @Mock
    private BioIdWebserviceClient bioIdWebserviceClient;

    @Mock
    private Yuv420Image imageAsYUV420;
    @Mock
    private Bitmap imageAsRGB;
    @Mock
    private GrayscaleImage imageAsGrayscale;
    @Mock
    private GrayscaleImage rotatedImageAsGrayscale;

    @Mock
    private Yuv420Image referenceImageAsYUV420;
    @Mock
    private GrayscaleImage referenceImageAsGrayscale;
    @Mock
    private GrayscaleImage rotatedReferenceImageAsGrayscale;

    private FacialRecognitionBasePresenterForTest presenter;

    private static class FacialRecognitionBasePresenterForTest extends FacialRecognitionBasePresenter<VerificationToken> {

        private boolean startBiometricOperationCalled = false;
        private boolean onFaceDetectedCalled = false;
        private boolean onNoFaceDetectedCalled = false;
        private boolean onUploadSuccessfulCalled = false;

        private FacialRecognitionBasePresenterForTest(Context ctx, LoggingHelper log, FacialRecognitionContract.View view,
                                                      BackgroundHandler backgroundHandler, ImageFormatConverter imageFormatConverter,
                                                      ImageTransformer imageTransformer, FaceDetection faceDetection,
                                                      MotionDetection motionDetection, BioIdWebserviceClient bioIdWebserviceClient) {
            super(ctx, log, view, backgroundHandler, imageFormatConverter, imageTransformer, faceDetection, motionDetection,
                    bioIdWebserviceClient);

            this.bwsToken = BWS_TOKEN;
            this.currentDirection = CURRENT_DIRECTION;
            this.destinationDirection = DESTINATION_DIRECTION;
            this.taskIdMotionTimeout = TASK_ID_MOTION_TIMEOUT;
            this.taskIdFaceTimeout = TASK_ID_FACE_TIMEOUT;
            this.successfulUploads = SUCCESSFUL_IMAGE_UPLOADS;
            this.failedUploads = FAILED_IMAGE_UPLOADS;
        }

        @Override
        protected void startBiometricOperation() {
            startBiometricOperationCalled = true;
        }

        @Override
        protected void onFaceDetected() {
            onFaceDetectedCalled = true;
        }

        @Override
        protected void onNoFaceDetected() {
            onNoFaceDetectedCalled = true;
        }

        @Override
        protected void onUploadSuccessful() {
            onUploadSuccessfulCalled = true;
        }

        @Override
        public void promptForProcessExplanationAccepted() {
        }

        @Override
        public void promptForProcessExplanationRejected() {
        }

        @Override
        public void promptToTurn90DegreesAccepted() {
        }

        @Override
        public void promptToTurn90DegreesRejected() {
        }
    }

    @Before
    public void setUp() throws Exception {
        presenter = new FacialRecognitionBasePresenterForTest(ctx, log, view, backgroundHandler, imageFormatConverter,
                imageTransformer, faceDetection, motionDetection, bioIdWebserviceClient);
    }

    @Test
    public void onResume_ifPermissionStateIsUnknown_permissionWillBeRequested() throws Exception {
        presenter.onResume();

        verify(view).requestCameraPermission();
        assertThat(presenter.permissionState, is(PermissionState.REQUESTING_PERMISSION));
    }

    @Test
    public void onResume_ifPermissionStateIsDenied_showCameraPermissionErrorAndNavigateBackWillBeCalled() throws Exception {
        presenter.permissionState = PermissionState.PERMISSION_DENIED;

        presenter.onResume();

        verify(view).showCameraPermissionErrorAndNavigateBack();
    }

    @Test
    public void onResume_ifPermissionStateIsAnyOtherState_nothingWillHappen() throws Exception {
        presenter.permissionState = PermissionState.REQUESTING_PERMISSION;

        presenter.onResume();

        verify(view, never()).requestCameraPermission();
        verify(view, never()).showCameraPermissionErrorAndNavigateBack();
        assertThat(presenter.permissionState, is(PermissionState.REQUESTING_PERMISSION));
    }

    @Test
    public void onPause_ifPermissionIsGranted_permissionStateIsResetToUnknown() throws Exception {
        presenter.permissionState = PermissionState.PERMISSION_GRANTED;

        presenter.onPause();

        assertThat(presenter.permissionState, is(PermissionState.UNKNOWN));
    }

    @Test
    public void onPause_biometricOperationResetWillBeApplied() throws Exception {
        presenter.onPause();

        assertBiometricOperationReset();
    }

    @Test
    public void onPause_previewWillBeStopped() throws Exception {
        presenter.onPause();

        verify(view).stopPreview();
    }

    @Test
    public void onCameraPermissionGranted_permissionStateIsSetToGranted() throws Exception {
        presenter.onCameraPermissionGranted();

        assertThat(presenter.permissionState, is(PermissionState.PERMISSION_GRANTED));
    }

    @Test
    public void onCameraPermissionGranted_previewWillBeStarted() throws Exception {
        presenter.onCameraPermissionGranted();

        verify(view).startPreview();
    }

    @Test
    public void onCameraPermissionGranted_startBiometricOperationWillBeCalled() throws Exception {
        presenter.onCameraPermissionGranted();

        assertThat(presenter.startBiometricOperationCalled, is(true));
    }

    @Test
    public void onCameraPermissionDenied_permissionStateIsSetToDenied() throws Exception {
        presenter.onCameraPermissionDenied();

        assertThat(presenter.permissionState, is(PermissionState.PERMISSION_DENIED));
    }

    @Test
    public void onCameraPermissionDenied_showCameraPermissionErrorAndNavigateBackWillBeCalled() throws Exception {
        presenter.onCameraPermissionDenied();

        verify(view).showCameraPermissionErrorAndNavigateBack();
    }

    @Test
    public void detectFace_ifFaceDetectionIsNotOperationalCallNoFaceDetected() throws Exception {
        when(faceDetection.isOperational()).thenReturn(false);

        presenter.detectFace();

        assertThat(presenter.onNoFaceDetectedCalled, is(true));
    }

    @Test
    public void detectFace_ifFaceDetectionIsNotOperationalNoTimeoutWillBeStarted() throws Exception {
        when(faceDetection.isOperational()).thenReturn(false);

        presenter.detectFace();

        assertThat(presenter.taskIdFaceTimeout, is(not(SynchronousBackgroundHandler.TASK_ID)));
    }

    @Test
    public void detectFace_findFaceInfoWillBeShown() throws Exception {
        when(faceDetection.isOperational()).thenReturn(true);

        presenter.detectFace();

        verify(view).showFindFaceInfo();
    }

    @Test
    public void detectFace_stateIsSetToWaitingForImageWithFace() throws Exception {
        when(faceDetection.isOperational()).thenReturn(true);

        presenter.detectFace();

        assertThat(presenter.imageDetectionState, is(ImageDetectionState.WAITING_FOR_IMAGE_WITH_FACE));
    }

    @Test
    public void onImageCaptured_ifWaitingForReferenceImage_taskIdForFaceTimeoutWillBeSet() throws Exception {
        when(faceDetection.isOperational()).thenReturn(true);

        presenter.detectFace();

        assertThat(presenter.taskIdFaceTimeout, is(SynchronousBackgroundHandler.TASK_ID));
    }

    @Test
    public void detectFace_ifFaceDetectionTimeoutOccursOnNoFaceDetectedWillBeCalled() throws Exception {
        when(faceDetection.isOperational()).thenReturn(true);
        backgroundHandler.doNothingOnRunWithDelay();
        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        when(backgroundHandler.runWithDelay(captor.capture(), anyLong())).thenReturn(SynchronousBackgroundHandler.TASK_ID);

        presenter.detectFace();
        reset(view);  // only look at the mock interactions from the captured runnable
        captor.getValue().run();

        assertThat(presenter.imageDetectionState, is(ImageDetectionState.OTHER));
        verify(view).hideMessages();
        assertThat(presenter.onNoFaceDetectedCalled, is(true));
    }

    @Test
    public void captureImagePair_movementInstructionsAreShown() throws Exception {
        presenter.captureImagePair(CURRENT_DIRECTION, DESTINATION_DIRECTION);

        verify(view).showMovementInfo(DESTINATION_DIRECTION);
    }

    @Test
    public void captureImagePair_nowWaitingForReferenceImage() throws Exception {
        presenter.captureImagePair(CURRENT_DIRECTION, DESTINATION_DIRECTION);

        assertThat(presenter.imageDetectionState, is(ImageDetectionState.WAITING_FOR_REFERENCE_IMAGE));
    }

    @Test
    public void onImageCaptured_ifWaitingForImageWithFace_findFaceInfoMessageWillBeHidden() throws Exception {
        when(faceDetection.containsFace(imageAsRGB, IMAGE_ROTATION)).thenReturn(true);
        mockStateWaitingForImageWithFace();

        presenter.onImageCaptured(imageAsYUV420, IMAGE_ROTATION);

        verify(view).hideMessages();
    }

    @Test
    public void onImageCaptured_ifWaitingForImageWithFace_onFaceDetectedWillBeCalled() throws Exception {
        when(faceDetection.containsFace(imageAsRGB, IMAGE_ROTATION)).thenReturn(true);
        mockStateWaitingForImageWithFace();

        presenter.onImageCaptured(imageAsYUV420, IMAGE_ROTATION);

        assertThat(presenter.onFaceDetectedCalled, is(true));
    }

    @Test
    public void onImageCaptured_ifWaitingForImageWithFace_faceDetectionTimeoutIsCanceled() throws Exception {
        when(faceDetection.containsFace(imageAsRGB, IMAGE_ROTATION)).thenReturn(true);
        mockStateWaitingForImageWithFace();

        presenter.onImageCaptured(imageAsYUV420, IMAGE_ROTATION);

        verify(backgroundHandler).cancelScheduledTask(TASK_ID_FACE_TIMEOUT);
    }

    @Test
    public void onImageCaptured_ifYuvToRgbConversionIsBroken_errorWillBeSuppressedAndFaceDetectedWillBeCalled() throws Exception {
        mockStateWaitingForImageWithFace();
        when(imageFormatConverter.yuv420ToRgb(imageAsYUV420)).thenThrow(RSIllegalArgumentException.class);

        presenter.onImageCaptured(imageAsYUV420, IMAGE_ROTATION);

        verify(view).hideMessages();
        assertThat(presenter.onFaceDetectedCalled, is(true));
        verify(backgroundHandler).cancelScheduledTask(TASK_ID_FACE_TIMEOUT);
    }

    @Test
    public void onImageCaptured_ifNoFaceWasDetected_stateIsResetToWaitingForImageWithFace() throws Exception {
        when(faceDetection.containsFace(imageAsRGB, IMAGE_ROTATION)).thenReturn(false);
        mockStateWaitingForImageWithFace();

        presenter.onImageCaptured(imageAsYUV420, IMAGE_ROTATION);

        assertThat(presenter.imageDetectionState, is(ImageDetectionState.WAITING_FOR_IMAGE_WITH_FACE));
    }

    @Test
    public void onImageCaptured_ifWaitingForReferenceImage_stateIsSetToWaitingForImageWithMotion() throws Exception {
        mockStateWaitingForReferenceImage();

        presenter.onImageCaptured(referenceImageAsYUV420, IMAGE_ROTATION);

        assertThat(presenter.imageDetectionState, is(ImageDetectionState.WAITING_FOR_IMAGE_WITH_MOTION));
    }

    @Test
    public void onImageCaptured_ifWaitingForReferenceImage_imageWillBeKeptInMemory() throws Exception {
        mockStateWaitingForReferenceImage();

        presenter.onImageCaptured(referenceImageAsYUV420, IMAGE_ROTATION);

        assertThat(presenter.referenceImage, is(rotatedReferenceImageAsGrayscale));
    }

    @Test
    public void onImageCaptured_ifWaitingForReferenceImage_imageWillBeUploaded() throws Exception {
        mockStateWaitingForReferenceImage();

        presenter.onImageCaptured(referenceImageAsYUV420, IMAGE_ROTATION);

        verify(bioIdWebserviceClient).uploadImage(rotatedReferenceImageAsGrayscale, BWS_TOKEN, CURRENT_DIRECTION, SUCCESSFUL_IMAGE_UPLOADS);
        assertThat(presenter.successfulUploads, is(SUCCESSFUL_IMAGE_UPLOADS + 1));
        assertThat(presenter.onUploadSuccessfulCalled, is(true));
    }

    @Test
    public void onImageCaptured_ifMotionDetectionTimeoutOccurs_showsMotionDetectionWarning() throws Exception {
        mockStateWaitingForReferenceImage();
        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        when(backgroundHandler.runWithDelay(captor.capture(), anyLong())).thenReturn(SynchronousBackgroundHandler.TASK_ID);

        presenter.onImageCaptured(referenceImageAsYUV420, IMAGE_ROTATION);
        reset(view, backgroundHandler);  // only look at the mock interactions from the captured runnable
        captor.getValue().run();

        assertBiometricOperationReset();
        verify(view).showMotionDetectionWarning();
    }

    @Test
    public void onImageCaptured_ifWaitingForReferenceImage_taskIdForMotionTimeoutWillBeSet() throws Exception {
        mockStateWaitingForReferenceImage();

        presenter.onImageCaptured(referenceImageAsYUV420, IMAGE_ROTATION);

        assertThat(presenter.taskIdMotionTimeout, is(SynchronousBackgroundHandler.TASK_ID));
    }

    @Test
    public void onImageCaptured_ifWaitingForImageWithMotion_stateIsSetToOther() throws Exception {
        mockStateWaitingForImageWithMotion();

        presenter.onImageCaptured(imageAsYUV420, IMAGE_ROTATION);

        assertThat(presenter.imageDetectionState, is(ImageDetectionState.OTHER));
    }

    @Test
    public void onImageCaptured_ifWaitingForImageWithMotion_motionDetectionTimeoutIsCanceled() throws Exception {
        mockStateWaitingForImageWithMotion();

        presenter.onImageCaptured(imageAsYUV420, IMAGE_ROTATION);

        verify(backgroundHandler).cancelScheduledTask(TASK_ID_MOTION_TIMEOUT);
    }

    @Test
    public void onImageCaptured_ifWaitingForImageWithMotion_movementInfoWillBeHidden() throws Exception {
        mockStateWaitingForImageWithMotion();

        presenter.onImageCaptured(imageAsYUV420, IMAGE_ROTATION);

        verify(view, times(2)).hideMessages();  // counting two invocation because the uploading images message will also be hidden
    }

    @Test
    public void onImageCaptured_ifWaitingForImageWithMotion_imageWillBeUploaded() throws Exception {
        mockStateWaitingForImageWithMotion();

        presenter.onImageCaptured(imageAsYUV420, IMAGE_ROTATION);

        verify(bioIdWebserviceClient).uploadImage(rotatedImageAsGrayscale, BWS_TOKEN, DESTINATION_DIRECTION, SUCCESSFUL_IMAGE_UPLOADS + 1);
        assertThat(presenter.successfulUploads, is(SUCCESSFUL_IMAGE_UPLOADS + 1));
        assertThat(presenter.onUploadSuccessfulCalled, is(true));
    }

    @Test
    public void onImageCaptured_ifWaitingForImageWithMotion_uploadingInfoWillBeShownDuringUpload() throws Exception {
        mockStateWaitingForImageWithMotion();

        presenter.onImageCaptured(imageAsYUV420, IMAGE_ROTATION);

        InOrder inOrder = inOrder(view);
        inOrder.verify(view).showUploadingImagesInfo();
        inOrder.verify(view).hideMessages();
    }

    @Test
    public void onImageCaptured_ifWaitingForImageWithMotion_loadingIndicatorWillBeShownDuringUpload() throws Exception {
        mockStateWaitingForImageWithMotion();

        presenter.onImageCaptured(imageAsYUV420, IMAGE_ROTATION);

        InOrder inOrder = inOrder(view);
        inOrder.verify(view).showLoadingIndicator();
        inOrder.verify(view).hideLoadingIndicator();
    }

    @Test
    public void onImageCaptured_ifNoMotionWasDetected_stateIsSetToWaitingForImageWithMotion() throws Exception {
        mockStateWaitingForImageWithMotion();
        when(motionDetection.detect(rotatedReferenceImageAsGrayscale, rotatedImageAsGrayscale)).thenReturn(false);

        presenter.onImageCaptured(imageAsYUV420, IMAGE_ROTATION);

        assertThat(presenter.imageDetectionState, is(ImageDetectionState.WAITING_FOR_IMAGE_WITH_MOTION));
        verify(bioIdWebserviceClient, never()).uploadImage(any(GrayscaleImage.class), any(BwsToken.class),
                any(MovementDirection.class), anyInt());
    }

    @Test(expected = RuntimeException.class)
    public void onImageCaptured_ifMotionDetectionAlgorithmFailed_exceptionWillBeRethrown() throws Exception {
        mockStateWaitingForImageWithMotion();
        RuntimeException e = new RuntimeException("motion detection algorithm failed");
        doThrow(e).when(motionDetection).detect(rotatedReferenceImageAsGrayscale, rotatedImageAsGrayscale);

        presenter.onImageCaptured(imageAsYUV420, IMAGE_ROTATION);
    }

    @Test
    public void onImageCaptured_ifImageUploadFailed_failedUploadCounterDoesIncrement() throws Exception {
        presenter.failedUploads = 0;
        mockStateWaitingForReferenceImage();
        doThrow(new NotRecognizedException()).when(bioIdWebserviceClient)
                .uploadImage(any(GrayscaleImage.class), any(BwsToken.class), any(MovementDirection.class), anyInt());

        presenter.onImageCaptured(referenceImageAsYUV420, IMAGE_ROTATION);

        assertThat(presenter.failedUploads, is(1));
    }

    @Test
    public void onImageCaptured_ifStateIsOther_doNothing() throws Exception {
        presenter.onImageCaptured(imageAsYUV420, IMAGE_ROTATION);

        verify(bioIdWebserviceClient, never()).uploadImage(any(GrayscaleImage.class), any(BwsToken.class),
                any(MovementDirection.class), anyInt());
    }

    @Test
    public void onUploadFailed_ifThreeUploadsDidFail_biometricOperationWillBeAborted() throws Exception {
        presenter.failedUploads = 3;

        presenter.onUploadFailed(new NotRecognizedException());

        assertBiometricOperationReset();
        verify(view).showNotRecognizedWarning();  // exception is used as an example, showWarningOrError() is tested separately
        verify(view).navigateBack(false);
    }

    @Test
    public void onUploadFailed_ifThreeOrLessUploadsDidFailAndTheReferenceImageUploadWasSuccessful_decreaseTheCounter()
            throws Exception {
        presenter.failedOperations = 1;
        presenter.successfulUploads = 3;

        presenter.onUploadFailed(new NotRecognizedException());

        assertThat(presenter.successfulUploads, is(2));
    }

    @Test
    public void onUploadFailed_warningIsShown() throws Exception {
        presenter.onUploadFailed(new NotRecognizedException());

        verify(view).showNotRecognizedWarning();  // exception is used as an example, showWarningOrError() is tested separately
    }

    @Test
    public void onUploadFailed_ifLessThanThreeUploadsDidFail_retryCapturingImagePair() throws Exception {
        presenter.currentDirection = MovementDirection.right;
        presenter.destinationDirection = MovementDirection.up;
        presenter.failedUploads = 2;

        presenter.onUploadFailed(new NotRecognizedException());

        assertThat(presenter.currentDirection, is(MovementDirection.right));
        assertThat(presenter.destinationDirection, is(MovementDirection.up));
        assertThat(presenter.imageDetectionState, is(ImageDetectionState.WAITING_FOR_REFERENCE_IMAGE));
    }

    @Test
    public void resetCaptureImagePair_stateResetIsApplied() throws Exception {
        presenter.imageDetectionState = ImageDetectionState.WAITING_FOR_IMAGE_WITH_MOTION;
        presenter.referenceImage = rotatedReferenceImageAsGrayscale;

        presenter.resetCaptureImagePair();

        assertCaptureImagePairReset();
    }

    @Test
    public void resetBiometricOperation_stateResetIsApplied() throws Exception {
        presenter.imageDetectionState = ImageDetectionState.WAITING_FOR_IMAGE_WITH_MOTION;
        presenter.referenceImage = rotatedReferenceImageAsGrayscale;

        presenter.resetBiometricOperation();

        assertBiometricOperationReset();
    }

    @Test
    public void showWarningOrError_showNotRecognizedWarningIsCalled() throws Exception {
        presenter.showWarningOrError(new NotRecognizedException());

        verify(view).showNotRecognizedWarning();
    }

    @Test
    public void showWarningOrError_showChallengeResponseExceptionIsCalled() throws Exception {
        presenter.showWarningOrError(new ChallengeResponseException());

        verify(view).showChallengeResponseWarning();
    }

    @Test
    public void showWarningOrError_showLiveDetectionWarningIsCalled() throws Exception {
        presenter.showWarningOrError(new LiveDetectionException());

        verify(view).showLiveDetectionWarning();
    }

    @Test
    public void showWarningOrError_showNoFaceFoundWarningIsCalled() throws Exception {
        presenter.showWarningOrError(new NoFaceFoundException());

        verify(view).showNoFaceFoundWarning();
    }

    @Test
    public void showWarningOrError_showMultipleFacesFoundWarningIsCalled() throws Exception {
        presenter.showWarningOrError(new MultipleFacesFoundException());

        verify(view).showMultipleFacesFoundWarning();
    }

    @Test
    public void showWarningOrError_showNoConnectionErrorIsCalled() throws Exception {
        presenter.showWarningOrError(new NoConnectionException(null));

        verify(view).showNoConnectionErrorAndNavigateBack();
    }

    @Test
    public void showWarningOrError_showServerErrorErrorIsCalled() throws Exception {
        presenter.showWarningOrError(new ServerErrorException());

        verify(view).showServerErrorAndNavigateBack();
    }

    @Test
    public void showWarningOrError_showWrongCredentialsErrorIsCalled() throws Exception {
        presenter.showWarningOrError(new WrongCredentialsException());

        verify(view).showWrongCredentialsErrorAndNavigateBack();
    }

    @Test
    public void showWarningOrError_showNoEnrollmentErrorIsCalled() throws Exception {
        presenter.showWarningOrError(new NoEnrollmentException());

        verify(view).showNoEnrollmentErrorAndNavigateBack();
    }

    @Test(expected = TechnicalException.class)
    public void showWarningOrError_unhandledExceptionsWillBeRethrown() throws Exception {
        presenter.showWarningOrError(new TechnicalException(""));
    }

    @Test
    public void navigateBackWithDelay_doesNavigateBackWithSuccess() throws Exception {
        presenter.navigateBackWithDelay(true);

        verify(view).navigateBack(true);
    }

    @Test
    public void navigateBackWithDelay_doesNavigateBackWithoutSuccess() throws Exception {
        presenter.navigateBackWithDelay(false);

        verify(view).navigateBack(false);
    }

    private void mockStateWaitingForImageWithFace() {
        presenter.imageDetectionState = ImageDetectionState.WAITING_FOR_IMAGE_WITH_FACE;

        when(imageFormatConverter.yuv420ToRgb(imageAsYUV420)).thenReturn(imageAsRGB);
    }

    private void mockStateWaitingForReferenceImage() {
        presenter.imageDetectionState = ImageDetectionState.WAITING_FOR_REFERENCE_IMAGE;

        when(imageFormatConverter.yuv420ToGrayscaleImage(referenceImageAsYUV420)).thenReturn(referenceImageAsGrayscale);
        when(imageTransformer.rotate(referenceImageAsGrayscale, IMAGE_ROTATION)).thenReturn(rotatedReferenceImageAsGrayscale);

        backgroundHandler.doNothingOnRunWithDelay();
    }

    private void mockStateWaitingForImageWithMotion() {
        presenter.imageDetectionState = ImageDetectionState.WAITING_FOR_IMAGE_WITH_MOTION;
        presenter.referenceImage = rotatedReferenceImageAsGrayscale;

        when(imageFormatConverter.yuv420ToGrayscaleImage(imageAsYUV420)).thenReturn(imageAsGrayscale);
        when(imageTransformer.rotate(imageAsGrayscale, IMAGE_ROTATION)).thenReturn(rotatedImageAsGrayscale);

        when(motionDetection.detect(rotatedReferenceImageAsGrayscale, rotatedImageAsGrayscale)).thenReturn(true);
    }

    private void assertCaptureImagePairReset() {
        verify(backgroundHandler).unsubscribeFromAllBackgroundTasks();
        verify(backgroundHandler).cancelAllScheduledTasks();

        assertThat(presenter.imageDetectionState, is(ImageDetectionState.OTHER));
        assertThat(presenter.currentDirection, is(nullValue()));
        assertThat(presenter.destinationDirection, is(nullValue()));
        assertThat(presenter.referenceImage, is(nullValue()));
        assertThat(presenter.taskIdMotionTimeout, is(nullValue()));
        assertThat(presenter.taskIdFaceTimeout, is(nullValue()));

        verify(view).hideLoadingIndicator();
    }

    private void assertBiometricOperationReset() {
        assertCaptureImagePairReset();

        assertThat(presenter.bwsToken, is(BWS_TOKEN));   // no reset
        assertThat(presenter.successfulUploads, is(0));  // reset to 0
        assertThat(presenter.failedUploads, is(0));      // reset to 0

        verify(view).hideMessages();
    }
}