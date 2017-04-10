package com.bioid.authenticator.facialrecognition.verification;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;

import com.bioid.authenticator.base.logging.LoggingHelper;
import com.bioid.authenticator.base.network.ServerErrorException;
import com.bioid.authenticator.base.network.bioid.webservice.BioIdWebserviceClient;
import com.bioid.authenticator.base.network.bioid.webservice.LiveDetectionException;
import com.bioid.authenticator.base.network.bioid.webservice.MovementDirection;
import com.bioid.authenticator.base.network.bioid.webservice.NoFaceFoundException;
import com.bioid.authenticator.base.network.bioid.webservice.token.VerificationToken;
import com.bioid.authenticator.base.network.bioid.webservice.token.VerificationTokenProvider;
import com.bioid.authenticator.base.threading.BackgroundHandler;
import com.bioid.authenticator.facialrecognition.FacialRecognitionContract;
import com.bioid.authenticator.facialrecognition.MotionDetection;
import com.bioid.authenticator.testutil.Mocks;
import com.bioid.authenticator.testutil.SynchronousBackgroundHandler;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class VerificationPresenterTest {

    private static final VerificationToken VERIFICATION_TOKEN = Mocks.verificationToken();
    private static final VerificationToken VERIFICATION_TOKEN_WITH_CHALLENGE = Mocks.verificationTokenWithChallenges();

    @Mock
    private Context ctx;
    @Mock
    private LoggingHelper log;
    @Mock
    private FacialRecognitionContract.View view;
    @Spy
    private SynchronousBackgroundHandler backgroundHandler;
    @Mock
    private MotionDetection motionDetection;
    @Mock
    private VerificationTokenProvider tokenProvider;
    @Mock
    private BioIdWebserviceClient bioIdWebserviceClient;

    @InjectMocks
    private VerificationPresenterForTest presenter;

    @SuppressWarnings({"SameParameterValue", "unused"})
    @SuppressLint("MissingSuperCall")
    private static class VerificationPresenterForTest extends VerificationPresenter {

        private boolean executeVerify = false;
        private boolean executeReset = false;
        private boolean verifyCalled = false;
        private boolean detectFaceCalled = false;
        private boolean resetBiometricOperationCalled = false;
        private RuntimeException showWarningCalledWith = null;
        private int captureImagePairCalledWithFirstParam = -1;
        private MovementDirection captureImagePairCalledWithSecondParam = null;
        private MovementDirection captureImagePairCalledWithThirdParam = null;

        private VerificationPresenterForTest(Context ctx, LoggingHelper log, FacialRecognitionContract.View view,
                                             BackgroundHandler backgroundHandler, MotionDetection motionDetection,
                                             VerificationTokenProvider tokenProvider, BioIdWebserviceClient bioIdWebserviceClient) {
            super(ctx, log, view, backgroundHandler, motionDetection, tokenProvider, bioIdWebserviceClient);

            this.bwsToken = VERIFICATION_TOKEN;
        }

        @Override
        protected void resetBiometricOperation() {
            resetBiometricOperationCalled = true;
            if (executeReset) {
                super.resetBiometricOperation();
            }
        }

        @Override
        protected void showWarningOrError(RuntimeException e) {
            showWarningCalledWith = e;
        }

        @Override
        protected void detectFace() {
            detectFaceCalled = true;
        }

        @Override
        protected void captureImagePair(int index,
                                        @NonNull MovementDirection currentDirection, @NonNull MovementDirection destinationDirection) {
            super.captureImagePair(index, currentDirection, destinationDirection);
            captureImagePairCalledWithFirstParam = index;
            captureImagePairCalledWithSecondParam = currentDirection;
            captureImagePairCalledWithThirdParam = destinationDirection;
        }

        private VerificationToken getBwsToken() {
            return bwsToken;
        }

        private void setBwsToken(VerificationToken bwsToken) {
            this.bwsToken = bwsToken;
        }

        private void setSuccessfulUploads(int successfulUploads) {
            this.successfulUploads = successfulUploads;
        }

        private int getSuccessfulUploads() {
            return this.successfulUploads;
        }

        private void setFailedUploads(int failedUploads) {
            this.failedUploads = failedUploads;
        }

        private int getFailedUploads() {
            return this.failedUploads;
        }

        private int getFailedOperations() {
            return this.failedOperations;
        }

        private void setFailedOperations(int failedOperations) {
            this.failedOperations = failedOperations;
        }

        @Override
        void verify() {
            verifyCalled = true;
            if (executeVerify) {
                super.verify();
            }
        }
    }

    @Before
    public void setUp() throws Exception {
        when(tokenProvider.requestVerificationToken(ctx)).thenReturn(VERIFICATION_TOKEN);
    }

    @Test
    public void startBiometricOperation_initialisationInfoIsShownWhileFetchingTheToken() throws Exception {
        presenter.startBiometricOperation();

        InOrder messageOrder = inOrder(view);
        messageOrder.verify(view).showInitialisationInfo();
        messageOrder.verify(view).hideMessages();
    }

    @Test
    public void startBiometricOperation_tokenIsFetchedAndSet() throws Exception {
        presenter.setBwsToken(null);
        presenter.startBiometricOperation();

        assertThat(presenter.getBwsToken(), is(VERIFICATION_TOKEN));
    }

    @Test
    public void startBiometricOperation_detectFaceIsCalled() throws Exception {
        presenter.startBiometricOperation();

        assertThat(presenter.detectFaceCalled, is(true));
    }

    @Test
    public void onFaceDetected_captureImagePairSessionTriggered() {
        presenter.onFaceDetected();

        assertThat(presenter.captureImagePairCalledWithFirstParam, is(0));
        assertThat(presenter.captureImagePairCalledWithSecondParam, is(MovementDirection.any));
        assertThat(presenter.captureImagePairCalledWithThirdParam, is(MovementDirection.any));
    }

    @Test
    public void onFaceDetected_withChallenge_captureImagePairSessionTriggered() {
        presenter.setBwsToken(VERIFICATION_TOKEN_WITH_CHALLENGE);

        presenter.onFaceDetected();

        assertThat(presenter.nextPairForChallenge, is(1));
        assertThat(presenter.captureImagePairCalledWithFirstParam, is(0));
        assertThat(presenter.captureImagePairCalledWithSecondParam, is(MovementDirection.any));
        assertThat(presenter.captureImagePairCalledWithThirdParam, is(MovementDirection.left));
    }

    @Test
    public void onNoFaceDetected_captureImagePairSessionTriggered() {
        presenter.onNoFaceDetected();

        assertThat(presenter.captureImagePairCalledWithFirstParam, is(0));
        assertThat(presenter.captureImagePairCalledWithSecondParam, is(MovementDirection.any));
        assertThat(presenter.captureImagePairCalledWithThirdParam, is(MovementDirection.any));
    }

    @Test
    public void onNoFaceDetected_withChallenge_captureImagePairSessionTriggered() {
        presenter.setBwsToken(VERIFICATION_TOKEN_WITH_CHALLENGE);

        presenter.onNoFaceDetected();

        assertThat(presenter.nextPairForChallenge, is(1));
        assertThat(presenter.captureImagePairCalledWithFirstParam, is(0));
        assertThat(presenter.captureImagePairCalledWithSecondParam, is(MovementDirection.any));
        assertThat(presenter.captureImagePairCalledWithThirdParam, is(MovementDirection.left));
    }

    @Test
    public void startBiometricOperation_ifTokenRequestFailed_resetBiometricOperation() throws Exception {
        doThrow(RuntimeException.class).when(tokenProvider).requestVerificationToken(ctx);

        presenter.startBiometricOperation();

        assertThat(presenter.resetBiometricOperationCalled, is(true));
    }

    @Test
    public void startBiometricOperation_ifTokenRequestFailed_warningIsShown() throws Exception {
        RuntimeException e = new RuntimeException("token request failed");
        doThrow(e).when(tokenProvider).requestVerificationToken(ctx);

        presenter.startBiometricOperation();

        assertThat(presenter.showWarningCalledWith, is(e));
    }

    @Test(expected = IllegalStateException.class)
    public void promptForProcessExplanationAccepted_shouldNotBeCalled() throws Exception {
        presenter.promptForProcessExplanationAccepted();
    }

    @Test(expected = IllegalStateException.class)
    public void promptForProcessExplanationRejected_shouldNotBeCalled() throws Exception {
        presenter.promptForProcessExplanationRejected();
    }

    @Test(expected = IllegalStateException.class)
    public void promptToTurn90DegreesAccepted_throwsException() throws Exception {
        presenter.promptToTurn90DegreesAccepted();
    }

    @Test(expected = IllegalStateException.class)
    public void promptToTurn90DegreesRejected_throwsException() throws Exception {
        presenter.promptToTurn90DegreesRejected();
    }

    @Test
    public void onImageWithMotionProcessed_movementIndicatorWillBeHidden() throws Exception {
        presenter.onImageWithMotionProcessed();

        verify(view).hideMovementIndicator();
    }

    @Test
    public void onImageWithMotionProcessed_withChallenge_movementIndicatorWillBeHiddenOnLastImagePair() throws Exception {
        presenter.setBwsToken(VERIFICATION_TOKEN_WITH_CHALLENGE);
        presenter.nextPairForChallenge = 3;

        presenter.onImageWithMotionProcessed();

        verify(view).hideMovementIndicator();
    }

    @Test
    public void onImageWithMotionProcessed_withChallenge_movementIndicatorWillNotBeHiddenIfChallengeRequiresMoreImages()
            throws Exception {
        presenter.setBwsToken(VERIFICATION_TOKEN_WITH_CHALLENGE);
        presenter.nextPairForChallenge = 1;

        presenter.onImageWithMotionProcessed();

        verify(view, never()).hideMovementIndicator();
    }

    @Test
    public void onImageWithMotionProcessed_withChallenge_nextImagePairOfChallengeWillBeCaptured() throws Exception {
        presenter.setBwsToken(VERIFICATION_TOKEN_WITH_CHALLENGE);
        presenter.nextPairForChallenge = 1;

        presenter.onImageWithMotionProcessed();

        assertThat(presenter.captureImagePairCalledWithFirstParam, is(2));  // "any" and "left" are already uploaded
        assertThat(presenter.captureImagePairCalledWithSecondParam, is(MovementDirection.right));
        assertThat(presenter.captureImagePairCalledWithThirdParam, is(MovementDirection.up));
    }

    @Test
    public void onImageWithMotionProcessed_withChallenge_nextPairForChallengeWillSetToNextPair() throws Exception {
        presenter.setBwsToken(VERIFICATION_TOKEN_WITH_CHALLENGE);
        presenter.nextPairForChallenge = 1;

        presenter.onImageWithMotionProcessed();

        assertThat(presenter.nextPairForChallenge, is(3));
    }

    @Test
    public void onUploadSuccessful_counterIsIncremented() throws Exception {
        presenter.setSuccessfulUploads(10);

        presenter.onUploadSuccessful();

        assertThat(presenter.getSuccessfulUploads(), is(11));
    }

    @Test
    public void onUploadSuccessful_ifFirstImageOfPairWasUploaded_waitForSecondImageUploadToComplete() throws Exception {
        presenter.setSuccessfulUploads(2);  // one pair already uploaded + reference image of second pair

        presenter.onUploadSuccessful();

        assertThat(presenter.verifyCalled, is(false));
    }

    @Test
    public void onUploadSuccessful_verifyWillBeCalled() throws Exception {
        presenter.setSuccessfulUploads(1);

        presenter.onUploadSuccessful();

        assertThat(presenter.verifyCalled, is(true));
    }

    @Test
    public void onUploadSuccessful_withChallengeResponse_ifLastImageOfChallengeWasUploaded_verifyWillBeCalled() throws Exception {
        presenter.setBwsToken(VERIFICATION_TOKEN_WITH_CHALLENGE);
        presenter.setSuccessfulUploads(3);  // mocked challenge: left, right, up

        presenter.onUploadSuccessful();

        assertThat(presenter.verifyCalled, is(true));
    }

    @Test
    public void onUploadSuccessful_withChallengeResponse_ifNotLastImageOfChallengeWasUploaded_verifyNotWillBeCalled()
            throws Exception {
        presenter.setBwsToken(VERIFICATION_TOKEN_WITH_CHALLENGE);
        presenter.setSuccessfulUploads(1);  // mocked challenge: left, right, up

        presenter.onUploadSuccessful();

        assertThat(presenter.verifyCalled, is(false));
    }

    @Test
    public void verify_verifyingInfoIsShown() throws Exception {
        presenter.executeVerify = true;
        presenter.verify();

        verify(view).showVerifyingInfo();
    }

    @Test
    public void verify_resetBiometricOperation() throws Exception {
        presenter.executeVerify = true;
        presenter.verify();

        assertThat(presenter.resetBiometricOperationCalled, is(true));
    }

    @Test
    public void verify_ifVerificationWasSuccessful_successWillBeShownBeforeNavigatingBack() throws Exception {
        presenter.executeVerify = true;
        presenter.verify();

        verify(view).showVerificationSuccess();
        verify(view).navigateBack(true);
    }

    @Test
    public void verify_ifVerificationWasNotSuccessful_warningWillBeShown() throws Exception {
        RuntimeException e = new RuntimeException("verification not successful");
        doThrow(e).when(bioIdWebserviceClient).verify(VERIFICATION_TOKEN);
        presenter.executeVerify = true;

        presenter.verify();

        assertThat(presenter.showWarningCalledWith, is(e));
    }

    @Test
    public void verify_ifVerificationWasNotSuccessful_operationWillBeRestarted() throws Exception {
        RuntimeException e = new RuntimeException("verification not successful");
        doThrow(e).when(bioIdWebserviceClient).verify(VERIFICATION_TOKEN);
        presenter.setFailedOperations(0);
        when(VERIFICATION_TOKEN.getMaxTries()).thenReturn(3);
        presenter.executeVerify = true;

        presenter.verify();

        assertThat(presenter.captureImagePairCalledWithFirstParam, is(0));
        assertThat(presenter.captureImagePairCalledWithSecondParam, is(MovementDirection.any));
        assertThat(presenter.captureImagePairCalledWithThirdParam, is(MovementDirection.any));
    }

    @Test
    public void verify_ifVerificationWasNotSuccessful_withChallenge_operationWillBeRestartedWithNextChallenge() throws Exception {
        presenter.setBwsToken(VERIFICATION_TOKEN_WITH_CHALLENGE);
        RuntimeException e = new RuntimeException("verification not successful");
        doThrow(e).when(bioIdWebserviceClient).verify(VERIFICATION_TOKEN_WITH_CHALLENGE);
        presenter.setFailedOperations(0);
        when(VERIFICATION_TOKEN_WITH_CHALLENGE.getMaxTries()).thenReturn(3);
        presenter.executeVerify = true;

        presenter.verify();

        assertThat(presenter.nextPairForChallenge, is(1));
        assertThat(presenter.captureImagePairCalledWithFirstParam, is(0));
        assertThat(presenter.captureImagePairCalledWithSecondParam, is(MovementDirection.any));
        assertThat(presenter.captureImagePairCalledWithThirdParam, is(MovementDirection.right));
    }

    @Test
    public void verify_ifVerificationWasNotSuccessful_failedOperationCounterIsIncremented() throws Exception {
        RuntimeException e = new RuntimeException("verification not successful");
        doThrow(e).when(bioIdWebserviceClient).verify(VERIFICATION_TOKEN);
        presenter.setFailedOperations(0);
        when(VERIFICATION_TOKEN.getMaxTries()).thenReturn(3);
        presenter.executeVerify = true;

        presenter.verify();


        assertThat(presenter.getFailedOperations(), is(1));
    }

    @Test
    public void verify_ifVerificationWasNotSuccessful_maxTriesAreExceeded_navigateBackWithoutSuccess() throws Exception {
        RuntimeException e = new RuntimeException("verification not successful");
        doThrow(e).when(bioIdWebserviceClient).verify(VERIFICATION_TOKEN);
        presenter.setFailedOperations(1);
        when(VERIFICATION_TOKEN.getMaxTries()).thenReturn(2);
        presenter.executeVerify = true;

        presenter.verify();

        verify(view).navigateBack(false);
    }

    @Test
    public void onUploadFailed_noFaceFoundErrorWillBeSuppressedWithinChallengeResponse() throws Exception {
        presenter.setBwsToken(VERIFICATION_TOKEN_WITH_CHALLENGE);
        presenter.setSuccessfulUploads(10);
        presenter.setFailedUploads(10);

        presenter.onUploadFailed(new NoFaceFoundException());

        assertThat(presenter.getSuccessfulUploads(), is(11));
        assertThat(presenter.getFailedUploads(), is(10));
    }

    @Test
    public void onUploadFailed_liveDetectionErrorWillBeSuppressedWithinChallengeResponse() throws Exception {
        presenter.setBwsToken(VERIFICATION_TOKEN_WITH_CHALLENGE);
        presenter.setSuccessfulUploads(10);
        presenter.setFailedUploads(10);

        presenter.onUploadFailed(new LiveDetectionException());

        assertThat(presenter.getSuccessfulUploads(), is(11));
        assertThat(presenter.getFailedUploads(), is(10));
    }

    @Test
    public void onUploadFailed_otherErrorsWillNotBeSuppressedWithinChallengeResponse() throws Exception {
        presenter.setBwsToken(VERIFICATION_TOKEN_WITH_CHALLENGE);
        presenter.setSuccessfulUploads(10);
        presenter.setFailedUploads(10);

        presenter.onUploadFailed(new ServerErrorException());

        assertThat(presenter.getSuccessfulUploads(), is(10));
        assertThat(presenter.getFailedUploads(), is(11));
    }

    @Test
    public void resetBiometricOperation_doesResetValues() throws Exception {
        presenter.nextPairForChallenge = 42;
        presenter.executeReset = true;

        presenter.resetBiometricOperation();

        assertThat(presenter.nextPairForChallenge, is(0));
    }
}
