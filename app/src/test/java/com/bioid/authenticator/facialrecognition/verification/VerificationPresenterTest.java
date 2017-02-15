package com.bioid.authenticator.facialrecognition.verification;

import android.annotation.SuppressLint;
import android.content.Context;

import com.bioid.authenticator.base.image.Yuv420Image;
import com.bioid.authenticator.base.logging.LoggingHelper;
import com.bioid.authenticator.base.network.bioid.webservice.BioIdWebserviceClient;
import com.bioid.authenticator.base.network.bioid.webservice.MovementDirection;
import com.bioid.authenticator.base.network.bioid.webservice.token.VerificationToken;
import com.bioid.authenticator.base.network.bioid.webservice.token.VerificationTokenProvider;
import com.bioid.authenticator.base.threading.BackgroundHandler;
import com.bioid.authenticator.facialrecognition.FacialRecognitionContract;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class VerificationPresenterTest {

    private static final VerificationToken VERIFICATION_TOKEN = new VerificationToken("verification");

    @Mock
    private Context ctx;
    @Mock
    private LoggingHelper log;
    @Mock
    private FacialRecognitionContract.View view;
    @Spy
    private SynchronousBackgroundHandler backgroundHandler;
    @Mock
    private VerificationTokenProvider tokenProvider;
    @Mock
    private BioIdWebserviceClient bioIdWebserviceClient;

    @Mock
    private Yuv420Image image;

    @InjectMocks
    private VerificationPresenterForTest presenter;

    @SuppressWarnings({"SameParameterValue", "unused"})
    @SuppressLint("MissingSuperCall")
    private static class VerificationPresenterForTest extends VerificationPresenter {

        private boolean detectFaceCalled = false;
        private boolean resetBiometricOperationCalled = false;
        private RuntimeException showWarningCalledWith = null;
        private MovementDirection captureImagePairCalledWithFirstParam = null;
        private MovementDirection captureImagePairCalledWithSecondParam = null;

        private VerificationPresenterForTest(Context ctx, LoggingHelper log, FacialRecognitionContract.View view,
                                             BackgroundHandler backgroundHandler, VerificationTokenProvider tokenProvider,
                                             BioIdWebserviceClient bioIdWebserviceClient) {
            super(ctx, log, view, backgroundHandler, tokenProvider, bioIdWebserviceClient);

            this.bwsToken = VERIFICATION_TOKEN;
        }

        @Override
        protected void resetBiometricOperation() {
            resetBiometricOperationCalled = true;
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
        protected void captureImagePair(MovementDirection currentDirection, MovementDirection destinationDirection) {
            super.captureImagePair(currentDirection, destinationDirection);
            captureImagePairCalledWithFirstParam = currentDirection;
            captureImagePairCalledWithSecondParam = destinationDirection;
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

        private int getFailedOperations() {
            return this.failedOperations;
        }

        private void setFailedOperations(int failedOperations) {
            this.failedOperations = failedOperations;
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

        assertThat(presenter.captureImagePairCalledWithFirstParam, is(MovementDirection.any));
        assertThat(presenter.captureImagePairCalledWithSecondParam, is(MovementDirection.any));
    }

    @Test
    public void onNoFaceDetected_captureImagePairSessionTriggered() {
        presenter.onNoFaceDetected();

        assertThat(presenter.captureImagePairCalledWithFirstParam, is(MovementDirection.any));
        assertThat(presenter.captureImagePairCalledWithSecondParam, is(MovementDirection.any));
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
    public void onUploadSuccessful_verifyingInfoIsShown() throws Exception {
        presenter.onUploadSuccessful();

        verify(view).showVerifyingInfo();
    }

    @Test
    public void onUploadSuccessful_resetBiometricOperation() throws Exception {
        presenter.onUploadSuccessful();

        assertThat(presenter.resetBiometricOperationCalled, is(true));
    }

    @Test
    public void onUploadSuccessful_ifVerificationWasSuccessful_successWillBeShownBeforeNavigatingBack() throws Exception {
        presenter.onUploadSuccessful();

        verify(view).showVerificationSuccess();
        verify(view).navigateBack(true);
    }

    @Test
    public void onUploadSuccessful_ifVerificationWasNotSuccessful_warningWillBeShown() throws Exception {
        RuntimeException e = new RuntimeException("verification not successful");
        doThrow(e).when(bioIdWebserviceClient).verify(VERIFICATION_TOKEN);

        presenter.onUploadSuccessful();

        assertThat(presenter.showWarningCalledWith, is(e));
    }

    @Test
    public void onUploadSuccessful_ifVerificationWasNotSuccessful_operationWillBeRestarted() throws Exception {
        RuntimeException e = new RuntimeException("verification not successful");
        doThrow(e).when(bioIdWebserviceClient).verify(VERIFICATION_TOKEN);

        presenter.onUploadSuccessful();

        assertThat(presenter.captureImagePairCalledWithFirstParam, is(MovementDirection.any));
        assertThat(presenter.captureImagePairCalledWithSecondParam, is(MovementDirection.any));
    }

    @Test
    public void onUploadSuccessful_ifVerificationWasNotSuccessful_failedOperationCounterIsIncremented() throws Exception {
        RuntimeException e = new RuntimeException("verification not successful");
        doThrow(e).when(bioIdWebserviceClient).verify(VERIFICATION_TOKEN);

        presenter.onUploadSuccessful();

        assertThat(presenter.getFailedOperations(), is(1));
    }

    @Test
    public void onUploadSuccessful_ifVerificationWasNotSuccessfulForTheThirdTime_navigateBackWithoutSuccess() throws Exception {
        RuntimeException e = new RuntimeException("verification not successful");
        doThrow(e).when(bioIdWebserviceClient).verify(VERIFICATION_TOKEN);
        presenter.setFailedOperations(2);

        presenter.onUploadSuccessful();

        verify(view).navigateBack(false);
    }

    @Test
    public void onUploadSuccessful_ifFirstImageOfPairWasUploaded_waitForSecondImageUploadToComplete() throws Exception {
        presenter.setSuccessfulUploads(3);  // one pair already uploaded + reference image of second pair

        presenter.onUploadSuccessful();

        verify(bioIdWebserviceClient, never()).verify(any(VerificationToken.class));
    }
}
