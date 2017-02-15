package com.bioid.authenticator.base.network.bioid.webservice;

import android.renderscript.RenderScript;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.util.ArrayMap;

import com.bioid.authenticator.BuildConfig;
import com.bioid.authenticator.base.image.GrayscaleImage;
import com.bioid.authenticator.base.image.ImageFormatConverter;
import com.bioid.authenticator.base.network.HttpRequest;
import com.bioid.authenticator.base.network.HttpRequestHelper;
import com.bioid.authenticator.base.network.NoConnectionException;
import com.bioid.authenticator.base.network.ServerErrorException;
import com.bioid.authenticator.base.network.TechnicalException;
import com.bioid.authenticator.base.network.bioid.webservice.token.BwsToken;
import com.bioid.authenticator.base.network.bioid.webservice.token.EnrollmentToken;
import com.bioid.authenticator.base.network.bioid.webservice.token.VerificationToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.Charset;
import java.util.Map;

/**
 * Client for the BioID Webservice (BWS).
 */
public class BioIdWebserviceClient {

    @SuppressWarnings("WeakerAccess")  // used in bws flavor
    static final String BWS_BASE_URL = String.format("https://%s.bioid.com", BuildConfig.BIOID_BWS_INSTANCE_NAME);

    private static final byte[] DATA_URL_HEADER_PNG_BASE64 = "data:image/png;base64,".getBytes(Charset.forName("UTF-8"));

    @VisibleForTesting
    static final int HTTP_STATUS_WRONG_CREDENTIALS = 401;

    @VisibleForTesting
    static final String JSON_KEY_SUCCESS = "Success";
    @VisibleForTesting
    static final String JSON_KEY_ACCEPTED = "Accepted";
    @VisibleForTesting
    static final String JSON_KEY_ERROR = "Error";

    @VisibleForTesting
    static final String ERROR_CODE_NO_TEMPLATE_AVAILABLE = "NoTemplateAvailable";
    @VisibleForTesting
    static final String ERROR_CODE_CHALLENGE_RESPONSE_FAILED = "ChallengeResponseFailed";
    @VisibleForTesting
    static final String ERROR_CODE_LIVE_DETECTION_FAILED = "LiveDetectionFailed";
    @VisibleForTesting
    static final String ERROR_CODE_NO_FACE = "NoFaceFound";
    @VisibleForTesting
    static final String ERROR_CODE_MULTIPLE_FACES = "MultipleFacesFound";

    @SuppressWarnings("WeakerAccess")  // used in bws flavor
    final HttpRequestHelper httpRequestHelper;
    private final Encoder encoder;
    private final ImageFormatConverter imageFormatConverter;

    /**
     * Creates a new instance of the BioIdWebserviceClient.
     *
     * @param rs RenderScript instance
     */
    public BioIdWebserviceClient(RenderScript rs) {
        this.httpRequestHelper = new HttpRequestHelper();
        this.encoder = new Encoder();
        this.imageFormatConverter = new ImageFormatConverter(rs);
    }

    @VisibleForTesting
    BioIdWebserviceClient(HttpRequestHelper httpRequestHelper, Encoder encoder, ImageFormatConverter imageFormatConverter) {
        this.httpRequestHelper = httpRequestHelper;
        this.encoder = encoder;
        this.imageFormatConverter = imageFormatConverter;
    }

    /**
     * Perform the biometric verification based on the uploaded images.
     *
     * @param verificationToken BWS token for verification
     * @throws NotRecognizedException     if the user has not been recognized
     * @throws LiveDetectionException     if images do not prove that they are recorded from a live person
     * @throws ChallengeResponseException if the images do not fulfill the challenge-response criteria
     * @throws NoEnrollmentException      if the user has not been enrolled
     * @throws NoConnectionException      if no connection could be established
     * @throws ServerErrorException       if the server failed to process the request
     * @throws TechnicalException         if any other technical error occurred
     */
    public void verify(@NonNull VerificationToken verificationToken) {
        try {
            HttpRequest request = createVerificationResultRequest(verificationToken.getToken());
            JSONObject responseBody = httpRequestHelper.asJsonIfOk(request);
            handleBiometricOperationResult(responseBody);
        } catch (HttpRequestHelper.Non200StatusException e) {
            throw new TechnicalException(e);
        }
    }

    @VisibleForTesting
    protected HttpRequest createVerificationResultRequest(@NonNull String token) {
        try {
            return withDefaultTimeout(
                    HttpRequest.get(BWS_BASE_URL + "/extension/verify")
                            .authorization("Bearer " + token)
                            .acceptJson());
        } catch (HttpRequest.HttpRequestException e) {
            throw new NoConnectionException(e);
        }
    }

    /**
     * Perform the biometric enrollment based on the uploaded images.
     *
     * @param enrollmentToken BWS token for enrollment
     * @throws LiveDetectionException     if images do not prove that they are recorded from a live person
     * @throws ChallengeResponseException if the images do not fulfill the challenge-response criteria
     * @throws NoConnectionException      if no connection could be established
     * @throws ServerErrorException       if the server failed to process the request
     * @throws TechnicalException         if any other technical error occurred
     */
    public void enroll(@NonNull EnrollmentToken enrollmentToken) {
        try {
            HttpRequest request = createEnrollmentResultRequest(enrollmentToken.getToken());
            JSONObject responseBody = httpRequestHelper.asJsonIfOk(request);
            handleBiometricOperationResult(responseBody);
        } catch (HttpRequestHelper.Non200StatusException e) {
            throw new TechnicalException(e);
        }
    }

    @VisibleForTesting
    protected HttpRequest createEnrollmentResultRequest(@NonNull String token) {
        try {
            return withDefaultTimeout(
                    HttpRequest.get(BWS_BASE_URL + "/extension/enroll")
                            .authorization("Bearer " + token)
                            .acceptJson());
        } catch (HttpRequest.HttpRequestException e) {
            throw new NoConnectionException(e);
        }
    }

    // used in bws flavor
    @SuppressWarnings("WeakerAccess")
    HttpRequest withDefaultTimeout(HttpRequest request) {
        return request.connectTimeout(4000).readTimeout(6000);
    }

    private void handleBiometricOperationResult(JSONObject json) {
        try {
            boolean success = json.getBoolean(JSON_KEY_SUCCESS);

            if (!success) {

                if (!json.has(JSON_KEY_ERROR)) {
                    throw new NotRecognizedException();
                }

                String error = json.getString(JSON_KEY_ERROR);
                switch (error) {
                    case ERROR_CODE_LIVE_DETECTION_FAILED:
                        throw new LiveDetectionException();
                    case ERROR_CODE_CHALLENGE_RESPONSE_FAILED:
                        throw new ChallengeResponseException();
                    case ERROR_CODE_NO_TEMPLATE_AVAILABLE:
                        throw new NoEnrollmentException();
                    default:
                        throw new TechnicalException("unknown error code: " + error);
                }
            }
        } catch (JSONException e) {
            throw new TechnicalException("missing key on JSON deserialization", e);
        }
    }

    /**
     * Uploads an image for enrollment or verification.
     *
     * @param img       image which should be uploaded
     * @param bwsToken  BWS token for enrollment or verification
     * @param direction specifies the movement direction of the head
     * @param index     index of the uploaded image within a series of uploads
     * @throws NoFaceFoundException        if the uploaded image did not contain a face
     * @throws MultipleFacesFoundException if the uploaded image did contain multiple faces
     * @throws NoConnectionException       if no connection could be established
     * @throws WrongCredentialsException   if the provided bwsToken is invalid or has expired
     * @throws ServerErrorException        if the server failed to process the request
     * @throws TechnicalException          if any other technical error occurred
     */
    public void uploadImage(@NonNull GrayscaleImage img, @NonNull BwsToken bwsToken, @NonNull MovementDirection direction,
                            @IntRange(from = 1) int index) {
        try {
            HttpRequest request = createUploadImageRequest(prepareImage(img), bwsToken.getToken(), direction, index);

            JSONObject responseBody = httpRequestHelper.asJsonIfOk(request);
            handleUploadResult(responseBody);
        } catch (HttpRequestHelper.Non200StatusException e) {
            if (e.getStatus() == HTTP_STATUS_WRONG_CREDENTIALS) {
                throw new WrongCredentialsException();
            } else {
                throw new TechnicalException(e);
            }
        }
    }

    @NonNull
    private byte[] prepareImage(@NonNull GrayscaleImage img) {
        byte[] imgAsPNG = imageFormatConverter.grayscaleImageToPng(img);
        return asDataUrl(imgAsPNG);
    }

    @NonNull
    private byte[] asDataUrl(@NonNull byte[] imgAsPNG) {
        byte[] imgAsBase64 = encoder.encodeAsBase64(imgAsPNG);

        byte[] dataUrl = new byte[DATA_URL_HEADER_PNG_BASE64.length + imgAsBase64.length];
        System.arraycopy(DATA_URL_HEADER_PNG_BASE64, 0, dataUrl, 0, DATA_URL_HEADER_PNG_BASE64.length);
        System.arraycopy(imgAsBase64, 0, dataUrl, DATA_URL_HEADER_PNG_BASE64.length, imgAsBase64.length);
        return dataUrl;
    }

    @VisibleForTesting
    protected HttpRequest createUploadImageRequest(@NonNull byte[] imgAsDataUrl, @NonNull String token,
                                                   @NonNull MovementDirection direction, @IntRange(from = 1) int index) {
        try {
            Map<String, String> queryParameters = new ArrayMap<>(3);
            queryParameters.put("tag", direction.name());
            queryParameters.put("index", Integer.toString(index));
            queryParameters.put("trait", "FACE");

            return HttpRequest.post(BWS_BASE_URL + "/extension/upload", queryParameters, true)
                    .authorization("Bearer " + token)
                    .acceptJson()
                    .contentType("text/plain", "utf-8")
                    .connectTimeout(5000)
                    .readTimeout(25_000)
                    .send(imgAsDataUrl);
        } catch (HttpRequest.HttpRequestException e) {
            throw new NoConnectionException(e);
        }
    }

    private void handleUploadResult(@NonNull JSONObject json) {
        try {
            boolean accepted = json.getBoolean(JSON_KEY_ACCEPTED);
            if (!accepted) {
                String error = json.getString(JSON_KEY_ERROR);
                switch (error) {
                    case ERROR_CODE_NO_FACE:
                        throw new NoFaceFoundException();
                    case ERROR_CODE_MULTIPLE_FACES:
                        throw new MultipleFacesFoundException();
                    default:
                        throw new TechnicalException("unknown error code: " + error);
                }
            }
        } catch (JSONException e) {
            throw new TechnicalException("missing key on JSON deserialization", e);
        }
    }
}
