package com.bioid.authenticator.base.network.bioid.webservice;

import android.graphics.Bitmap;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;

import com.bioid.authenticator.base.image.GrayscaleImage;
import com.bioid.authenticator.base.image.ImageFormatConverter;
import com.bioid.authenticator.base.logging.LoggingHelper;
import com.bioid.authenticator.base.network.HttpRequest;
import com.bioid.authenticator.base.network.HttpRequestHelper;
import com.bioid.authenticator.base.network.NoConnectionException;
import com.bioid.authenticator.base.network.ServerErrorException;
import com.bioid.authenticator.base.network.TechnicalException;
import com.bioid.authenticator.base.network.bioid.webservice.token.BwsToken;
import com.bioid.authenticator.base.network.bioid.webservice.token.EnrollmentToken;
import com.bioid.authenticator.base.network.bioid.webservice.token.VerificationToken;
import com.bioid.authenticator.testutil.Mocks;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BioIdWebserviceClientTest {

    // override request creation because HttpRequest creation causes DNS lookup (actually done by URL constructor)
    // this trick makes it easier than using mockito mocks because the request will be modified multiple times
    @SuppressWarnings("UnusedParameters")
    private class BioIdWebserviceClientForTest extends BioIdWebserviceClient {

        BioIdWebserviceClientForTest(HttpRequestHelper httpRequestHelper, LoggingHelper log, Encoder encoder,
                                     ImageFormatConverter imageFormatConverter) {
            super(httpRequestHelper, log, encoder, imageFormatConverter);
        }

        @Override
        protected HttpRequest createUploadImageRequest(@NonNull byte[] imgAsDataUrl, @NonNull BwsToken token,
                                                       @NonNull MovementDirection direction, @IntRange(from = 1) int index) {
            return uploadImageRequest;
        }

        @Override
        protected HttpRequest createVerificationResultRequest(@NonNull String token) {
            return verificationResultRequest;
        }

        @Override
        protected HttpRequest createEnrollmentResultRequest(@NonNull String token) {
            return enrollmentResultRequest;
        }
    }

    private static final VerificationToken VERIFICATION_TOKEN = Mocks.verificationToken();
    private static final EnrollmentToken ENROLLMENT_TOKEN = Mocks.enrollmentToken();
    private static final MovementDirection DIRECTION = MovementDirection.any;
    private static final int UPLOAD_INDEX = 1;
    private static final byte[] PNG = {1, 2, 3};
    private static final byte[] PNG_AS_BASE64 = {4, 5, 6};

    @Mock
    private HttpRequestHelper httpRequestHelper;
    @Mock
    private LoggingHelper log;
    @Mock
    private Encoder encoder;
    @Mock
    private ImageFormatConverter imageFormatConverter;
    @Mock
    private JSONObject uploadResult;
    @Mock
    private JSONObject verificationResult;
    @Mock
    private JSONObject enrollmentResult;
    @Mock
    private HttpRequest uploadImageRequest;
    @Mock
    private HttpRequest verificationResultRequest;
    @Mock
    private HttpRequest enrollmentResultRequest;
    @Mock
    private GrayscaleImage img;
    @Mock
    private Bitmap bitmap;
    @Mock
    private BwsToken bwsToken;

    private BioIdWebserviceClient bioIdWebserviceClient;

    @Before
    public void setUp() throws Exception {
        bioIdWebserviceClient = new BioIdWebserviceClientForTest(httpRequestHelper, log, encoder, imageFormatConverter);

        when(httpRequestHelper.asJsonIfOk(verificationResultRequest)).thenReturn(verificationResult);
        when(httpRequestHelper.asJsonIfOk(enrollmentResultRequest)).thenReturn(enrollmentResult);
        when(httpRequestHelper.asJsonIfOk(uploadImageRequest)).thenReturn(uploadResult);

        when(imageFormatConverter.bitmapToPng(bitmap)).thenReturn(PNG);

        when(encoder.encodeAsBase64(PNG)).thenReturn(PNG_AS_BASE64);

        when(verificationResult.getBoolean(BioIdWebserviceClient.JSON_KEY_SUCCESS)).thenReturn(true);
        when(enrollmentResult.getBoolean(BioIdWebserviceClient.JSON_KEY_SUCCESS)).thenReturn(true);
        when(uploadResult.getBoolean(BioIdWebserviceClient.JSON_KEY_ACCEPTED)).thenReturn(true);
    }

    @Test
    public void testVerify_doesNothingIfSuccessful() throws Exception {
        verify();
    }

    @Test(expected = NotRecognizedException.class)
    public void testVerify_throwsExceptionIfNotRecognized() throws Exception {
        when(verificationResult.getBoolean(BioIdWebserviceClient.JSON_KEY_SUCCESS)).thenReturn(false);
        when(verificationResult.has(BioIdWebserviceClient.JSON_KEY_ERROR)).thenReturn(false);
        verify();
    }

    @Test(expected = LiveDetectionException.class)
    public void testVerify_throwsExceptionIfNotRecordedFromALivePerson() throws Exception {
        when(verificationResult.getBoolean(BioIdWebserviceClient.JSON_KEY_SUCCESS)).thenReturn(false);
        when(verificationResult.has(BioIdWebserviceClient.JSON_KEY_ERROR)).thenReturn(true);
        when(verificationResult.getString(BioIdWebserviceClient.JSON_KEY_ERROR)).thenReturn(BioIdWebserviceClient.ERROR_CODE_LIVE_DETECTION_FAILED);
        verify();
    }

    @Test(expected = ChallengeResponseException.class)
    public void testVerify_throwsExceptionIfImagesDoNotFulfillChallengeResponseCriteria() throws Exception {
        when(verificationResult.getBoolean(BioIdWebserviceClient.JSON_KEY_SUCCESS)).thenReturn(false);
        when(verificationResult.has(BioIdWebserviceClient.JSON_KEY_ERROR)).thenReturn(true);
        when(verificationResult.getString(BioIdWebserviceClient.JSON_KEY_ERROR)).thenReturn(BioIdWebserviceClient.ERROR_CODE_CHALLENGE_RESPONSE_FAILED);
        verify();
    }

    @Test(expected = NoEnrollmentException.class)
    public void testVerify_throwsExceptionIfUserHasNotBeenEnrolled() throws Exception {
        when(verificationResult.getBoolean(BioIdWebserviceClient.JSON_KEY_SUCCESS)).thenReturn(false);
        when(verificationResult.has(BioIdWebserviceClient.JSON_KEY_ERROR)).thenReturn(true);
        when(verificationResult.getString(BioIdWebserviceClient.JSON_KEY_ERROR)).thenReturn(BioIdWebserviceClient.ERROR_CODE_NO_TEMPLATE_AVAILABLE);
        verify();
    }

    @Test(expected = NoSamplesException.class)
    public void testVerify_throwsExceptionIfNoImagesHaveBeenUploaded() throws Exception {
        doThrow(new HttpRequestHelper.Non200StatusException(BioIdWebserviceClient.HTTP_STATUS_NO_SAMPLES))
                .when(httpRequestHelper).asJsonIfOk(verificationResultRequest);
        verify();
    }

    @Test(expected = NoConnectionException.class)
    public void testVerify_throwsExceptionIfNoConnectionCouldBeEstablished() throws Exception {
        doThrow(NoConnectionException.class).when(httpRequestHelper).asJsonIfOk(verificationResultRequest);
        verify();
    }

    @Test(expected = ServerErrorException.class)
    public void testVerify_throwsExceptionIfServerCouldNotProcessTheRequest() throws Exception {
        doThrow(ServerErrorException.class).when(httpRequestHelper).asJsonIfOk(verificationResultRequest);
        verify();
    }

    @Test(expected = TechnicalException.class)
    public void testVerify_throwsExceptionOnUnhandledResponseCode() throws Exception {
        doThrow(new HttpRequestHelper.Non200StatusException(404)).when(httpRequestHelper).asJsonIfOk(verificationResultRequest);
        verify();
    }

    @Test(expected = TechnicalException.class)
    public void testVerify_throwsExceptionIfRequestUnsuccessful() throws Exception {
        doThrow(TechnicalException.class).when(httpRequestHelper).asJsonIfOk(verificationResultRequest);
        verify();
    }

    @Test(expected = TechnicalException.class)
    public void testVerify_throwsExceptionIfJsonHasMissingKeys() throws Exception {
        doThrow(JSONException.class).when(verificationResult).getBoolean(BioIdWebserviceClient.JSON_KEY_SUCCESS);
        verify();
    }

    @Test
    public void testEnroll_doesNothingIfSuccessful() throws Exception {
        enroll();
    }

    @Test(expected = LiveDetectionException.class)
    public void testEnroll_throwsExceptionIfNotRecordedFromALivePerson() throws Exception {
        when(enrollmentResult.getBoolean(BioIdWebserviceClient.JSON_KEY_SUCCESS)).thenReturn(false);
        when(enrollmentResult.has(BioIdWebserviceClient.JSON_KEY_ERROR)).thenReturn(true);
        when(enrollmentResult.getString(BioIdWebserviceClient.JSON_KEY_ERROR)).thenReturn(BioIdWebserviceClient.ERROR_CODE_LIVE_DETECTION_FAILED);
        enroll();
    }

    @Test(expected = ChallengeResponseException.class)
    public void testEnroll_throwsExceptionIfImagesDoNotFulfillChallengeResponseCriteria() throws Exception {
        when(enrollmentResult.getBoolean(BioIdWebserviceClient.JSON_KEY_SUCCESS)).thenReturn(false);
        when(enrollmentResult.has(BioIdWebserviceClient.JSON_KEY_ERROR)).thenReturn(true);
        when(enrollmentResult.getString(BioIdWebserviceClient.JSON_KEY_ERROR)).thenReturn(BioIdWebserviceClient.ERROR_CODE_CHALLENGE_RESPONSE_FAILED);
        enroll();
    }

    @Test(expected = NoSamplesException.class)
    public void testEnroll_throwsExceptionIfNoImagesHaveBeenUploaded() throws Exception {
        doThrow(new HttpRequestHelper.Non200StatusException(BioIdWebserviceClient.HTTP_STATUS_NO_SAMPLES))
                .when(httpRequestHelper).asJsonIfOk(enrollmentResultRequest);
        enroll();
    }

    @Test(expected = NoConnectionException.class)
    public void testEnroll_throwsExceptionIfNoConnectionCouldBeEstablished() throws Exception {
        doThrow(NoConnectionException.class).when(httpRequestHelper).asJsonIfOk(enrollmentResultRequest);
        enroll();
    }

    @Test(expected = ServerErrorException.class)
    public void testEnroll_throwsExceptionIfServerCouldNotProcessTheRequest() throws Exception {
        doThrow(ServerErrorException.class).when(httpRequestHelper).asJsonIfOk(enrollmentResultRequest);
        enroll();
    }

    @Test(expected = TechnicalException.class)
    public void testEnroll_throwsExceptionOnUnhandledResponseCode() throws Exception {
        doThrow(new HttpRequestHelper.Non200StatusException(404)).when(httpRequestHelper).asJsonIfOk(enrollmentResultRequest);
        enroll();
    }

    @Test(expected = TechnicalException.class)
    public void testEnroll_throwsExceptionIfRequestUnsuccessful() throws Exception {
        doThrow(TechnicalException.class).when(httpRequestHelper).asJsonIfOk(enrollmentResultRequest);
        enroll();
    }

    @Test(expected = TechnicalException.class)
    public void testEnroll_throwsExceptionIfJsonHasMissingKeys() throws Exception {
        doThrow(JSONException.class).when(enrollmentResult).getBoolean(BioIdWebserviceClient.JSON_KEY_SUCCESS);
        enroll();
    }

    @Test
    public void testUploadImage_doesNothingIfUploadSuccessful() throws Exception {
        uploadImage();
    }

    @Test(expected = NoFaceFoundException.class)
    public void testUploadImage_throwsExceptionIfNoFaceFound() throws Exception {
        when(uploadResult.getBoolean(BioIdWebserviceClient.JSON_KEY_ACCEPTED)).thenReturn(false);
        when(uploadResult.getString(BioIdWebserviceClient.JSON_KEY_ERROR)).thenReturn(BioIdWebserviceClient.ERROR_CODE_NO_FACE);
        uploadImage();
    }

    @Test(expected = MultipleFacesFoundException.class)
    public void testUploadImage_throwsExceptionIfMultipleFacesFound() throws Exception {
        when(uploadResult.getBoolean(BioIdWebserviceClient.JSON_KEY_ACCEPTED)).thenReturn(false);
        when(uploadResult.getString(BioIdWebserviceClient.JSON_KEY_ERROR)).thenReturn(BioIdWebserviceClient.ERROR_CODE_MULTIPLE_FACES);
        uploadImage();
    }

    @Test(expected = NoFaceFoundException.class)
    public void testUploadImage_throwsExceptionOnUnknownErrorCode() throws Exception {
        when(uploadResult.getBoolean(BioIdWebserviceClient.JSON_KEY_ACCEPTED)).thenReturn(false);
        when(uploadResult.getString(BioIdWebserviceClient.JSON_KEY_ERROR)).thenReturn("this error code is unknown");
        uploadImage();
    }

    @Test(expected = WrongCredentialsException.class)
    public void testUploadImage_throwsExceptionIfBwsTokenIsInvalidOrHasExpired() throws Exception {
        doThrow(new HttpRequestHelper.Non200StatusException(BioIdWebserviceClient.HTTP_STATUS_WRONG_CREDENTIALS))
                .when(httpRequestHelper).asJsonIfOk(uploadImageRequest);
        uploadImage();
    }

    @Test(expected = NoConnectionException.class)
    public void testUploadImage_throwsExceptionIfNoConnectionCouldBeEstablished() throws Exception {
        doThrow(NoConnectionException.class).when(httpRequestHelper).asJsonIfOk(uploadImageRequest);
        uploadImage();
    }

    @Test(expected = ServerErrorException.class)
    public void testUpload_throwsExceptionIfServerCouldNotProcessTheRequest() throws Exception {
        doThrow(ServerErrorException.class).when(httpRequestHelper).asJsonIfOk(uploadImageRequest);
        uploadImage();
    }

    @Test(expected = TechnicalException.class)
    public void testUploadImage_throwsExceptionOnUnhandledResponseCode() throws Exception {
        doThrow(new HttpRequestHelper.Non200StatusException(404)).when(httpRequestHelper).asJsonIfOk(uploadImageRequest);
        uploadImage();
    }

    @Test(expected = TechnicalException.class)
    public void testUploadImage_throwsExceptionIfRequestUnsuccessful() throws Exception {
        doThrow(TechnicalException.class).when(httpRequestHelper).asJsonIfOk(uploadImageRequest);
        uploadImage();
    }

    @Test(expected = TechnicalException.class)
    public void testUploadImage_throwsExceptionIfJsonHasMissingKeys() throws Exception {
        doThrow(JSONException.class).when(uploadResult).getBoolean(BioIdWebserviceClient.JSON_KEY_ACCEPTED);
        uploadImage();
    }

    private void verify() {
        // actual values are not relevant for test
        bioIdWebserviceClient.verify(VERIFICATION_TOKEN);
    }

    private void enroll() {
        // actual values are not relevant for test
        bioIdWebserviceClient.enroll(ENROLLMENT_TOKEN);
    }

    private void uploadImage() {
        // actual values are not relevant for test
        bioIdWebserviceClient.uploadImage(bitmap, bwsToken, DIRECTION, UPLOAD_INDEX);
    }
}