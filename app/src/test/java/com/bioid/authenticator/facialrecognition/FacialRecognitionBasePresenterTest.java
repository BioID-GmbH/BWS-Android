package com.bioid.authenticator.facialrecognition;

import android.content.Context;

import com.bioid.authenticator.base.image.Yuv420Image;
import com.bioid.authenticator.base.logging.LoggingHelper;
import com.bioid.authenticator.base.network.NoConnectionException;
import com.bioid.authenticator.base.network.ServerErrorException;
import com.bioid.authenticator.base.network.TechnicalException;
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
import com.bioid.authenticator.base.network.bioid.webservice.token.VerificationToken;
import com.bioid.authenticator.base.threading.BackgroundHandler;
import com.bioid.authenticator.facialrecognition.FacialRecognitionBasePresenter.ImageDetectionState;
import com.bioid.authenticator.facialrecognition.FacialRecognitionBasePresenter.PermissionState;
import com.bioid.authenticator.testutil.Mocks;
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

    private static final VerificationToken BWS_TOKEN = Mocks.verificationToken();
    private static final int INDEX = 4;
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
    private FaceDetection faceDetection;
    @Mock
    private MotionDetection motionDetection;
    @Mock
    private BioIdWebserviceClient bioIdWebserviceClient;
    @Mock
    private Yuv420Image img;

    private FacialRecognitionBasePresenterForTest presenter;

    private static class FacialRecognitionBasePresenterForTest extends FacialRecognitionBasePresenter<VerificationToken> {

        private boolean startBiometricOperationCalled = false;
        private boolean onFaceDetectedCalled = false;
        private boolean onNoFaceDetectedCalled = false;
        private boolean onUploadSuccessfulCalled = false;
        private boolean onImageWithMotionProcessedCalled = false;
        private boolean disableMotionTimeout = true;

        private FacialRecognitionBasePresenterForTest(Context ctx, LoggingHelper log, FacialRecognitionContract.View view,
                                                      BackgroundHandler backgroundHandler, FaceDetection faceDetection,
                                                      MotionDetection motionDetection, BioIdWebserviceClient bioIdWebserviceClient) {
            super(ctx, log, view, backgroundHandler, faceDetection, motionDetection, bioIdWebserviceClient);

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
        void setupMotionTimeout() {
            if (!disableMotionTimeout) {
                super.setupMotionTimeout();
            }
        }

        @Override
        protected void onImageWithMotionProcessed() {
            onImageWithMotionProcessedCalled = true;
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
    public void setUp() {
        presenter = new FacialRecognitionBasePresenterForTest(ctx, log, view, backgroundHandler, faceDetection, motionDetection,
                bioIdWebserviceClient);
    }

    @Test
    public void onResume_ifPermissionStateIsUnknown_permissionWillBeRequested() {
        presenter.onResume();

        verify(view).requestCameraPermission();
        assertThat(presenter.permissionState, is(PermissionState.REQUESTING_PERMISSION));
    }

    @Test
    public void onResume_ifPermissionStateIsDenied_showCameraPermissionErrorAndNavigateBackWillBeCalled() {
        presenter.permissionState = PermissionState.PERMISSION_DENIED;

        presenter.onResume();

        verify(view).showCameraPermissionErrorAndNavigateBack();
    }

    @Test
    public void onResume_ifPermissionStateIsAnyOtherState_nothingWillHappen() {
        presenter.permissionState = PermissionState.REQUESTING_PERMISSION;

        presenter.onResume();

        verify(view, never()).requestCameraPermission();
        verify(view, never()).showCameraPermissionErrorAndNavigateBack();
        assertThat(presenter.permissionState, is(PermissionState.REQUESTING_PERMISSION));
    }

    @Test
    public void onPause_ifPermissionIsGranted_permissionStateIsResetToUnknown() {
        presenter.permissionState = PermissionState.PERMISSION_GRANTED;

        presenter.onPause();

        assertThat(presenter.permissionState, is(PermissionState.UNKNOWN));
    }

    @Test
    public void onPause_biometricOperationResetWillBeApplied() {
        presenter.onPause();

        assertBiometricOperationReset();
    }

    @Test
    public void onPause_previewWillBeStopped() {
        presenter.onPause();

        verify(view).stopPreview();
    }

    @Test
    public void onCameraPermissionGranted_permissionStateIsSetToGranted() {
        presenter.onCameraPermissionGranted();

        assertThat(presenter.permissionState, is(PermissionState.PERMISSION_GRANTED));
    }

    @Test
    public void onCameraPermissionGranted_previewWillBeStarted() {
        presenter.onCameraPermissionGranted();

        verify(view).startPreview();
    }

    @Test
    public void onCameraPermissionGranted_startBiometricOperationWillBeCalled() {
        presenter.onCameraPermissionGranted();

        assertThat(presenter.startBiometricOperationCalled, is(true));
    }

    @Test
    public void onCameraPermissionDenied_permissionStateIsSetToDenied() {
        presenter.onCameraPermissionDenied();

        assertThat(presenter.permissionState, is(PermissionState.PERMISSION_DENIED));
    }

    @Test
    public void onCameraPermissionDenied_showCameraPermissionErrorAndNavigateBackWillBeCalled() {
        presenter.onCameraPermissionDenied();

        verify(view).showCameraPermissionErrorAndNavigateBack();
    }

    @Test
    public void detectFace_ifFaceDetectionIsNotOperationalCallNoFaceDetected() {
        when(faceDetection.isOperational()).thenReturn(false);

        presenter.detectFace();

        assertThat(presenter.onNoFaceDetectedCalled, is(true));
    }

    @Test
    public void detectFace_ifFaceDetectionIsNotOperationalNoTimeoutWillBeStarted() {
        when(faceDetection.isOperational()).thenReturn(false);

        presenter.detectFace();

        assertThat(presenter.taskIdFaceTimeout, is(not(SynchronousBackgroundHandler.TASK_ID)));
    }

    @Test
    public void detectFace_findFaceInfoWillBeShown() {
        when(faceDetection.isOperational()).thenReturn(true);

        presenter.detectFace();

        verify(view).showFindFaceInfo();
    }

    @Test
    public void detectFace_stateIsSetToWaitingForImageWithFace() {
        when(faceDetection.isOperational()).thenReturn(true);

        presenter.detectFace();

        assertThat(presenter.imageDetectionState, is(ImageDetectionState.WAITING_FOR_IMAGE_WITH_FACE));
    }

    @Test
    public void onImageCaptured_ifWaitingForReferenceImage_taskIdForFaceTimeoutWillBeSet() {
        when(faceDetection.isOperational()).thenReturn(true);

        presenter.detectFace();

        assertThat(presenter.taskIdFaceTimeout, is(SynchronousBackgroundHandler.TASK_ID));
    }

    @Test
    public void detectFace_ifFaceDetectionTimeoutOccursOnNoFaceDetectedWillBeCalled() {
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
    public void captureImagePair_movementInstructionsAreShown() {
        presenter.captureImagePair(INDEX, CURRENT_DIRECTION, DESTINATION_DIRECTION);

        verify(view).showMovementInfo(DESTINATION_DIRECTION);
        verify(view).showMovementIndicator(DESTINATION_DIRECTION);
    }

    @Test
    public void captureImagePair_nowWaitingForReferenceImage() {
        presenter.captureImagePair(INDEX, CURRENT_DIRECTION, DESTINATION_DIRECTION);

        assertThat(presenter.imageDetectionState, is(ImageDetectionState.WAITING_FOR_REFERENCE_IMAGE));
    }

    @Test
    public void onImageCaptured_ifWaitingForImageWithFace_findFaceInfoMessageWillBeHidden() {
        when(faceDetection.containsFace(img)).thenReturn(true);
        mockStateWaitingForImageWithFace();

        presenter.onImageCaptured(img);

        verify(view).hideMessages();
    }

    @Test
    public void onImageCaptured_ifWaitingForImageWithFace_onFaceDetectedWillBeCalled() {
        when(faceDetection.containsFace(img)).thenReturn(true);
        mockStateWaitingForImageWithFace();

        presenter.onImageCaptured(img);

        assertThat(presenter.onFaceDetectedCalled, is(true));
    }

    @Test
    public void onImageCaptured_ifWaitingForImageWithFace_faceDetectionTimeoutIsCanceled() {
        when(faceDetection.containsFace(img)).thenReturn(true);
        mockStateWaitingForImageWithFace();

        presenter.onImageCaptured(img);

        verify(backgroundHandler).cancelScheduledTask(TASK_ID_FACE_TIMEOUT);
    }

    @Test
    public void onImageCaptured_ifNoFaceWasDetected_stateIsResetToWaitingForImageWithFace() {
        when(faceDetection.containsFace(img)).thenReturn(false);
        mockStateWaitingForImageWithFace();

        presenter.onImageCaptured(img);

        assertThat(presenter.imageDetectionState, is(ImageDetectionState.WAITING_FOR_IMAGE_WITH_FACE));
    }

    @Test
    public void onImageCaptured_ifWaitingForReferenceImage_motionDetectionTemplateWillBeCreated() {
        mockStateWaitingForReferenceImage();

        presenter.onImageCaptured(img);

        verify(motionDetection).createTemplate(img);
    }

    @Test
    public void onImageCaptured_ifWaitingForReferenceImage_stateIsSetToWaitingForImageWithMotion() {
        mockStateWaitingForReferenceImage();

        presenter.onImageCaptured(img);

        assertThat(presenter.imageDetectionState, is(ImageDetectionState.WAITING_FOR_IMAGE_WITH_MOTION));
    }

    @Test
    public void onImageCaptured_ifWaitingForReferenceImage_imageWillBeUploaded() {
        mockStateWaitingForReferenceImage();

        presenter.onImageCaptured(img);

        verify(bioIdWebserviceClient).uploadImage(img, BWS_TOKEN, CURRENT_DIRECTION, INDEX);
        assertThat(presenter.onUploadSuccessfulCalled, is(true));
    }

    @Test
    public void onImageCaptured_ifWaitingForImageWithMotion_stateIsSetToOther() {
        mockStateWaitingForImageWithMotion();

        presenter.onImageCaptured(img);

        assertThat(presenter.imageDetectionState, is(ImageDetectionState.OTHER));
    }

    @Test
    public void onImageCaptured_ifWaitingForImageWithMotion_motionDetectionTimeoutIsCanceled() {
        mockStateWaitingForImageWithMotion();

        presenter.onImageCaptured(img);

        verify(backgroundHandler).cancelScheduledTask(TASK_ID_MOTION_TIMEOUT);
    }

    @Test
    public void onImageCaptured_ifWaitingForImageWithMotion_movementInstructionTextWillBeHidden() {
        mockStateWaitingForImageWithMotion();

        presenter.onImageCaptured(img);

        verify(view, times(2)).hideMessages();  // counting two invocation because the uploading images message will also be hidden
    }

    @Test
    public void onImageCaptured_ifWaitingForImageWithMotion_onImageWithMotionProcessedWasCalled() {
        mockStateWaitingForImageWithMotion();

        presenter.onImageCaptured(img);

        assertThat(presenter.onImageWithMotionProcessedCalled, is(true));
    }

    @Test
    public void onImageCaptured_ifWaitingForImageWithMotion_imageWillBeUploaded() {
        mockStateWaitingForImageWithMotion();

        presenter.onImageCaptured(img);

        verify(bioIdWebserviceClient).uploadImage(img, BWS_TOKEN, DESTINATION_DIRECTION, INDEX + 1);
        assertThat(presenter.onUploadSuccessfulCalled, is(true));
    }

    @Test
    public void onImageCaptured_ifWaitingForImageWithMotion_uploadingInfoWillBeShownDuringUpload() {
        mockStateWaitingForImageWithMotion();

        presenter.onImageCaptured(img);

        InOrder inOrder = inOrder(view);
        inOrder.verify(view).showUploadingImagesInfo();
        inOrder.verify(view).hideMessages();
    }

    @Test
    public void onImageCaptured_ifWaitingForImageWithMotion_loadingIndicatorWillBeShownDuringUpload() {
        mockStateWaitingForImageWithMotion();

        presenter.onImageCaptured(img);

        InOrder inOrder = inOrder(view);
        inOrder.verify(view).showLoadingIndicator();
        inOrder.verify(view).hideLoadingIndicator();
    }

    @Test
    public void onImageCaptured_ifNoMotionWasDetected_stateIsSetToWaitingForImageWithMotion() {
        mockStateWaitingForImageWithMotion();
        when(motionDetection.detect(img)).thenReturn(false);

        presenter.onImageCaptured(img);

        assertThat(presenter.imageDetectionState, is(ImageDetectionState.WAITING_FOR_IMAGE_WITH_MOTION));
        verify(bioIdWebserviceClient, never()).uploadImage(any(Yuv420Image.class), any(BwsToken.class),
                any(MovementDirection.class), anyInt());
    }

    @Test
    public void onImageCaptured_ifImageUploadFailed_failedUploadCounterDoesIncrement() {
        presenter.failedUploads = 0;
        mockStateWaitingForReferenceImage();
        doThrow(new NotRecognizedException()).when(bioIdWebserviceClient)
                .uploadImage(any(Yuv420Image.class), any(BwsToken.class), any(MovementDirection.class), anyInt());

        presenter.onImageCaptured(img);

        assertThat(presenter.failedUploads, is(1));
    }

    @Test
    public void onImageCaptured_ifStateIsOther_doNothing() {
        presenter.onImageCaptured(img);

        verify(bioIdWebserviceClient, never()).uploadImage(any(Yuv420Image.class), any(BwsToken.class),
                any(MovementDirection.class), anyInt());
    }

    @Test
    public void setupMotionTimeout_taskIdForMotionTimeoutWillBeSet() {
        presenter.disableMotionTimeout = false;
        backgroundHandler.doNothingOnRunWithDelay();

        presenter.setupMotionTimeout();

        assertThat(presenter.taskIdMotionTimeout, is(SynchronousBackgroundHandler.TASK_ID));
    }

    @Test
    public void setupMotionTimeout_ifMotionDetectionTimeoutOccurs_showsMotionDetectionWarning() {
        presenter.disableMotionTimeout = false;
        mockStateWaitingForReferenceImage();

        presenter.setupMotionTimeout();
        presenter.taskIdMotionTimeout = null;  // would normally be unset by delayed runnable if execution is asynchronous

        assertBiometricOperationReset();
        verify(view).showMotionDetectionWarning();
    }

    @Test
    public void onUploadFailed_ifThreeUploadsDidFail_biometricOperationWillBeAborted() {
        presenter.failedUploads = 3;

        presenter.onUploadFailed(new NotRecognizedException());

        assertBiometricOperationReset();
        verify(view).showNotRecognizedWarning();  // exception is used as an example, showWarningOrError() is tested separately
        verify(view).navigateBack(false);
    }

    @Test
    public void onUploadFailed_ifThreeOrLessUploadsDidFailAndTheReferenceImageUploadWasSuccessful_decreaseTheCounter() {
        presenter.failedOperations = 1;
        presenter.successfulUploads = 3;

        presenter.onUploadFailed(new NotRecognizedException());

        assertThat(presenter.successfulUploads, is(2));
    }

    @Test
    public void onUploadFailed_warningIsShown() {
        presenter.onUploadFailed(new NotRecognizedException());

        verify(view).showNotRecognizedWarning();  // exception is used as an example, showWarningOrError() is tested separately
    }

    @Test
    public void onUploadFailed_ifLessThanThreeUploadsDidFail_retryCapturingImagePair() {
        presenter.index = 42;
        presenter.currentDirection = MovementDirection.right;
        presenter.destinationDirection = MovementDirection.up;
        presenter.failedUploads = 1;

        presenter.onUploadFailed(new NotRecognizedException());

        assertThat(presenter.index, is(42));
        assertThat(presenter.currentDirection, is(MovementDirection.right));
        assertThat(presenter.destinationDirection, is(MovementDirection.up));
        assertThat(presenter.imageDetectionState, is(ImageDetectionState.WAITING_FOR_REFERENCE_IMAGE));
    }

    @Test
    public void resetCaptureImagePair_stateResetIsApplied() {
        presenter.imageDetectionState = ImageDetectionState.WAITING_FOR_IMAGE_WITH_MOTION;

        presenter.resetCaptureImagePair();

        assertCaptureImagePairReset();
    }

    @Test
    public void resetBiometricOperation_stateResetIsApplied() {
        presenter.imageDetectionState = ImageDetectionState.WAITING_FOR_IMAGE_WITH_MOTION;

        presenter.resetBiometricOperation();

        assertBiometricOperationReset();
    }

    @Test
    public void showWarningOrError_showNotRecognizedWarningIsCalled() {
        presenter.showWarningOrError(new NotRecognizedException());

        verify(view).showNotRecognizedWarning();
    }

    @Test
    public void showWarningOrError_showChallengeResponseExceptionIsCalled() {
        presenter.showWarningOrError(new ChallengeResponseException());

        verify(view).showChallengeResponseWarning();
    }

    @Test
    public void showWarningOrError_showLiveDetectionWarningIsCalled() {
        presenter.showWarningOrError(new LiveDetectionException());

        verify(view).showLiveDetectionWarning();
    }

    @Test
    public void showWarningOrError_showNoFaceFoundWarningIsCalled() {
        presenter.showWarningOrError(new NoFaceFoundException());

        verify(view).showNoFaceFoundWarning();
    }

    @Test
    public void showWarningOrError_showMultipleFacesFoundWarningIsCalled() {
        presenter.showWarningOrError(new MultipleFacesFoundException());

        verify(view).showMultipleFacesFoundWarning();
    }

    @Test
    public void showWarningOrError_showNoSamplesWarningIsCalled() {
        presenter.showWarningOrError(new NoSamplesException());

        verify(view).showNoSamplesWarning();
    }

    @Test
    public void showWarningOrError_showNoConnectionErrorIsCalled() {
        presenter.showWarningOrError(new NoConnectionException(null));

        verify(view).showNoConnectionErrorAndNavigateBack();
    }

    @Test
    public void showWarningOrError_showServerErrorErrorIsCalled() {
        presenter.showWarningOrError(new ServerErrorException());

        verify(view).showServerErrorAndNavigateBack();
    }

    @Test
    public void showWarningOrError_showDeviceNotRegisteredErrorIsCalled() {
        presenter.showWarningOrError(new DeviceNotRegisteredException());

        verify(view).showDeviceNotRegisteredErrorAndNavigateBack();
    }

    @Test
    public void showWarningOrError_showWrongCredentialsErrorIsCalled() {
        presenter.showWarningOrError(new WrongCredentialsException());

        verify(view).showWrongCredentialsErrorAndNavigateBack();
    }

    @Test
    public void showWarningOrError_showNoEnrollmentErrorIsCalled() {
        presenter.showWarningOrError(new NoEnrollmentException());

        verify(view).showNoEnrollmentErrorAndNavigateBack();
    }

    @Test(expected = TechnicalException.class)
    public void showWarningOrError_unhandledExceptionsWillBeRethrown() {
        presenter.showWarningOrError(new TechnicalException(""));
    }

    @Test
    public void navigateBackWithDelay_doesNavigateBackWithSuccess() {
        presenter.navigateBackWithDelay(true);

        verify(view).navigateBack(true);
    }

    @Test
    public void navigateBackWithDelay_doesNavigateBackWithoutSuccess() {
        presenter.navigateBackWithDelay(false);

        verify(view).navigateBack(false);
    }

    private void mockStateWaitingForImageWithFace() {
        presenter.index = INDEX;
        presenter.imageDetectionState = ImageDetectionState.WAITING_FOR_IMAGE_WITH_FACE;
    }

    private void mockStateWaitingForReferenceImage() {
        presenter.index = INDEX;
        presenter.imageDetectionState = ImageDetectionState.WAITING_FOR_REFERENCE_IMAGE;
    }

    private void mockStateWaitingForImageWithMotion() {
        presenter.index = INDEX;
        presenter.imageDetectionState = ImageDetectionState.WAITING_FOR_IMAGE_WITH_MOTION;
        when(motionDetection.detect(img)).thenReturn(true);
    }

    private void assertCaptureImagePairReset() {
        verify(backgroundHandler).unsubscribeFromAllBackgroundTasks();
        verify(backgroundHandler).cancelAllScheduledTasks();

        verify(motionDetection).resetTemplate();

        assertThat(presenter.imageDetectionState, is(ImageDetectionState.OTHER));
        assertThat(presenter.index, is(0));
        assertThat(presenter.currentDirection, is(nullValue()));
        assertThat(presenter.destinationDirection, is(nullValue()));
        assertThat(presenter.taskIdMotionTimeout, is(nullValue()));
        assertThat(presenter.taskIdFaceTimeout, is(nullValue()));

        verify(view).hideLoadingIndicator();
        verify(view).hideMovementIndicator();
    }

    private void assertBiometricOperationReset() {
        assertCaptureImagePairReset();

        assertThat(presenter.bwsToken, is(BWS_TOKEN));   // no reset
        assertThat(presenter.successfulUploads, is(0));  // reset to 0
        assertThat(presenter.failedUploads, is(0));      // reset to 0

        verify(view).hideMessages();
    }
}