package com.bioid.authenticator.base.network.bioid.webservice;

import android.renderscript.RenderScript;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.util.ArrayMap;

import com.bioid.authenticator.BuildConfig;
import com.bioid.authenticator.base.image.ImageFormatConverter;
import com.bioid.authenticator.base.network.HttpRequest;
import com.bioid.authenticator.base.network.HttpRequestHelper;
import com.bioid.authenticator.base.network.NoConnectionException;
import com.bioid.authenticator.base.network.ServerErrorException;
import com.bioid.authenticator.base.network.TechnicalException;
import com.bioid.authenticator.base.network.bioid.webservice.token.EnrollmentToken;
import com.bioid.authenticator.base.network.bioid.webservice.token.VerificationToken;

import java.util.Map;

/**
 * Extended {@link BioIdWebserviceClient} which offers more functionality needed in the BWS flavor.
 */
public class BioIdWebserviceClientExtended extends BioIdWebserviceClient {

    private static final String CONTENT_TYPE_TEXT = "text/plain";

    @VisibleForTesting
    static final String VERIFICATION_TASK = "verify";
    @VisibleForTesting
    static final String ENROLLMENT_TASK = "enroll";

    /**
     * Creates a new instance of the BioIdWebserviceClientExtended.
     *
     * @param rs RenderScript instance
     */
    public BioIdWebserviceClientExtended(RenderScript rs) {
        super(rs);
    }

    @VisibleForTesting
    BioIdWebserviceClientExtended(HttpRequestHelper httpRequestHelper, Encoder encoder, ImageFormatConverter imageFormatConverter) {
        super(httpRequestHelper, encoder, imageFormatConverter);
    }

    /**
     * Requests a new verification token from BWS.
     * The token can be used to perform a user verification.
     *
     * @param bcid Biometric Class ID (BCID) of the user for whom the token shall be issued.
     * @return token for biometric verification
     * @throws NoConnectionException if no connection could be established
     * @throws ServerErrorException  if the server failed to process the request
     * @throws TechnicalException    if any other technical error occurred
     */
    @NonNull
    public VerificationToken requestVerificationToken(@NonNull String bcid) {
        try {
            HttpRequest request = createNewTokenRequest(bcid, VERIFICATION_TASK);
            String responseBody = httpRequestHelper.asTextIfOk(request);
            return new VerificationToken(responseBody);
        } catch (HttpRequestHelper.Non200StatusException e) {
            throw new TechnicalException(e);
        }
    }

    /**
     * Requests a new enrollment token from BWS.
     * The token can be used to perform a user enrollment.
     *
     * @param bcid Biometric Class ID (BCID) of the user for whom the token shall be issued.
     * @return token for biometric enrollment
     * @throws NoConnectionException if no connection could be established
     * @throws ServerErrorException  if the server failed to process the request
     * @throws TechnicalException    if any other technical error occurred
     */
    @NonNull
    public EnrollmentToken requestEnrollmentToken(@NonNull String bcid) {
        try {
            HttpRequest request = createNewTokenRequest(bcid, ENROLLMENT_TASK);
            String responseBody = httpRequestHelper.asTextIfOk(request);
            return new EnrollmentToken(responseBody);
        } catch (HttpRequestHelper.Non200StatusException e) {
            throw new TechnicalException(e);
        }
    }

    @VisibleForTesting
    protected HttpRequest createNewTokenRequest(@NonNull String bcid, @NonNull String task) {
        try {
            Map<String, String> queryParameters = new ArrayMap<>(3);
            queryParameters.put("id", BuildConfig.BIOID_APP_ID);
            queryParameters.put("bcid", bcid);
            queryParameters.put("task", task);

            return withDefaultTimeout(
                    HttpRequest.get(BWS_BASE_URL + "/extension/token", queryParameters, true)
                            .basic(BuildConfig.BIOID_APP_ID, BuildConfig.BIOID_APP_SECRET)
                            .accept(CONTENT_TYPE_TEXT));
        } catch (HttpRequest.HttpRequestException e) {
            throw new NoConnectionException(e);
        }
    }
}
