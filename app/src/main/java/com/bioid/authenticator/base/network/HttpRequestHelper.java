package com.bioid.authenticator.base.network;

import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import com.bioid.authenticator.base.logging.LoggingHelper;
import com.bioid.authenticator.base.logging.LoggingHelperFactory;
import com.bioid.authenticator.base.network.HttpRequest.HttpRequestException;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Contains utility methods for {@link HttpRequest} usage.
 */
public class HttpRequestHelper {

    private static final String UTF8 = "UTF-8";

    private final LoggingHelper log;
    private final JsonSerializer jsonSerializer;

    public HttpRequestHelper() {
        this.log = LoggingHelperFactory.create(HttpRequestHelper.class);
        this.jsonSerializer = new JsonSerializer();
    }

    @VisibleForTesting
    HttpRequestHelper(LoggingHelper log, JsonSerializer jsonSerializer) {
        this.log = log;
        this.jsonSerializer = jsonSerializer;
    }

    /**
     * Executes the given request and returns the HTTP response body as UTF-8 text on status code 200.
     *
     * @throws Non200StatusException if the HTTP status code was not 200
     * @throws NoConnectionException if no connection could be established
     * @throws ServerErrorException  if the server failed to process the request
     * @throws TechnicalException    if any other technical error occurred
     */
    @NonNull
    public String asTextIfOk(@NonNull HttpRequest request) {
        String stopwatchSessionId = log.startStopwatch(getStopwatchSessionId(request));

        try {
            switch (request.code()) {
                case 200:
                    return request.body(UTF8);
                case 500:
                    throw new ServerErrorException();
                default:
                    throw new Non200StatusException(request.code());
            }
        } catch (HttpRequestException e) {
            throw new NoConnectionException(e);

        } finally {
            log.stopStopwatch(stopwatchSessionId);
        }
    }

    @VisibleForTesting
    protected String getStopwatchSessionId(@NonNull HttpRequest request) {
        return request.method() + " " + request.url().getPath() + " (" + SystemClock.elapsedRealtimeNanos() + ")";
    }

    /**
     * Executes the given request and returns the HTTP response body as JSON object on status code 200.
     *
     * @throws Non200StatusException if the HTTP status code was not 200
     * @throws NoConnectionException if no connection could be established
     * @throws ServerErrorException  if the server failed to process the request
     * @throws TechnicalException    if any other technical error occurred
     */
    @NonNull
    public JSONObject asJsonIfOk(@NonNull HttpRequest request) {
        String body = asTextIfOk(request);
        return parseJsonBody(body);
    }

    private JSONObject parseJsonBody(@NonNull String body) {
        try {
            return jsonSerializer.toJsonObject(body);
        } catch (JSONException e) {
            throw new TechnicalException("response body could not be interpreted as JSON object", e);
        }
    }

    /**
     * Will be thrown if a HTTP request completed but not with status code 200.
     */
    public static final class Non200StatusException extends RuntimeException {

        private final int status;

        public Non200StatusException(int status) {
            super("HTTP request completed with status code " + status);
            this.status = status;
        }

        /**
         * Returns the HTTP status code.
         */
        public int getStatus() {
            return status;
        }
    }
}
