package com.bioid.authenticator.base.network.bioid.webservice;

import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.util.ArrayMap;

import com.bioid.authenticator.BuildConfig;
import com.bioid.authenticator.base.image.Yuv420Image;
import com.bioid.authenticator.base.logging.LoggingHelper;
import com.bioid.authenticator.base.logging.LoggingHelperFactory;
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

    private static final String MIME_TYPE_PNG = "image/png";
    private static final Charset UTF_8 = Charset.forName("UTF-8");

    @VisibleForTesting
    static final int HTTP_STATUS_NO_SAMPLES = 400;
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
    private final LoggingHelper log;
    private final Encoder encoder;

    /**
     * Creates a new instance of the BioIdWebserviceClient.
     */
    public BioIdWebserviceClient() {
        this.httpRequestHelper = new HttpRequestHelper();
        this.log = LoggingHelperFactory.create(BioIdWebserviceClient.class);
        this.encoder = new Encoder();
    }

    @VisibleForTesting
    BioIdWebserviceClient(HttpRequestHelper httpRequestHelper, LoggingHelper log, Encoder encoder) {
        this.httpRequestHelper = httpRequestHelper;
        this.log = log;
        this.encoder = encoder;
    }

    /**
     * Perform the biometric verification based on the uploaded images.
     *
     * @param verificationToken BWS token for verification
     * @throws NotRecognizedException     if the user has not been recognized
     * @throws LiveDetectionException     if images do not prove that they are recorded from a live person
     * @throws ChallengeResponseException if the images do not fulfill the challenge-response criteria
     * @throws NoEnrollmentException      if the user has not been enrolled
     * @throws NoSamplesException         if no valid images have been uploaded
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
            if (e.getStatus() == HTTP_STATUS_NO_SAMPLES) {
                throw new NoSamplesException();
            }
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
     * @throws NoSamplesException         if no valid images have been uploaded
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
            if (e.getStatus() == HTTP_STATUS_NO_SAMPLES) {
                throw new NoSamplesException();
            }
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
     * @param img       which should be uploaded
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
    public void uploadImage(@NonNull Yuv420Image img, @NonNull BwsToken bwsToken, @NonNull MovementDirection direction,
                            @IntRange(from = 1) int index) {
        try {
            HttpRequest request = createUploadImageRequest(prepareImage(img), bwsToken, direction, index);

            JSONObject responseBody = httpRequestHelper.asJsonIfOk(request);
            handleImageUploadResult(responseBody);
        } catch (HttpRequestHelper.Non200StatusException e) {
            if (e.getStatus() == HTTP_STATUS_WRONG_CREDENTIALS) {
                throw new WrongCredentialsException();
            } else {
                throw new TechnicalException(e);
            }
        }
    }

    @NonNull
    private byte[] prepareImage(@NonNull Yuv420Image img) {
        byte[] imgAsPNG = img.asPNG();
        return asDataUrl(MIME_TYPE_PNG, imgAsPNG);
    }

    @NonNull
    @SuppressWarnings("SameParameterValue")
    private byte[] asDataUrl(@NonNull String mimeType, @NonNull byte[] data) {
        byte[] dataUrlHeader = ("data:" + mimeType + ";base64,").getBytes(UTF_8);
        byte[] dataAsBase64 = encoder.encodeAsBase64(data);

        byte[] dataUrl = new byte[dataUrlHeader.length + dataAsBase64.length];
        System.arraycopy(dataUrlHeader, 0, dataUrl, 0, dataUrlHeader.length);
        System.arraycopy(dataAsBase64, 0, dataUrl, dataUrlHeader.length, dataAsBase64.length);
        return dataUrl;
    }

    @VisibleForTesting
    protected HttpRequest createUploadImageRequest(@NonNull byte[] imgAsDataUrl, @NonNull BwsToken token,
                                                   @NonNull MovementDirection direction, @IntRange(from = 1) int index) {
        try {
            Map<String, String> queryParameters = new ArrayMap<>(3);
            queryParameters.put("tag", direction.name());
            queryParameters.put("index", Integer.toString(index));
            queryParameters.put("trait", getTraitParamForImageUpload(token));

            return HttpRequest.post(BWS_BASE_URL + "/extension/upload", queryParameters, true)
                    .authorization("Bearer " + token.getToken())
                    .acceptJson()
                    .contentType("text/plain", "utf-8")
                    .connectTimeout(5000)
                    .readTimeout(25_000)
                    .send(imgAsDataUrl);
        } catch (HttpRequest.HttpRequestException e) {
            throw new NoConnectionException(e);
        }
    }

    @NonNull
    private String getTraitParamForImageUpload(@NonNull BwsToken token) {
        if (token.hasFaceTrait() && !token.hasPeriocularTrait()) {
            return Trait.Face.name();
        } else if (!token.hasFaceTrait() && token.hasPeriocularTrait()) {
            return Trait.Periocular.name();
        } else {
            return Trait.Face.name() + ", " + Trait.Periocular.name();
        }
    }

    private void handleImageUploadResult(@NonNull JSONObject json) {
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
                        log.w("mapped quality check error '%s' to '%s'", error, ERROR_CODE_NO_FACE);
                        throw new NoFaceFoundException();
                }
            }
        } catch (JSONException e) {
            throw new TechnicalException("missing key on JSON deserialization", e);
        }
    }
}
