package com.bioid.authenticator.base.network.bioid.webservice.token;

import android.support.annotation.NonNull;

import com.bioid.authenticator.base.network.bioid.webservice.Encoder;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * A simple parser for JSON Web Tokens (JWT).
 * <p>
 * Simple because this parser does not support all JWT capabilities.
 */
class JwtParser {

    private final Encoder encoder;

    JwtParser() {
        this.encoder = new Encoder();
    }

    /**
     * Parses the encoded JWT and does return the payload as JSON object.
     *
     * @param jwt base64 encoded JWT
     * @return the JWT payload interpreted as JSON object
     * @throws NullPointerException     if the JWT is null
     * @throws IllegalArgumentException if the JWT could not be parsed
     */
    @NonNull
    JSONObject getPayload(@NonNull String jwt) {
        String payloadEncoded = extractPayload(jwt);
        String payloadDecoded = encoder.decodeBase64(payloadEncoded);
        return parseAsJSON(payloadDecoded);
    }

    @NonNull
    private String extractPayload(@NonNull String jwt) {

        // JWT consists of "HEADER.PAYLOAD.SIGNATURE"
        String[] split = jwt.split("\\.");

        if (split.length != 3) {
            throw new IllegalArgumentException("non parsable JWT: " + jwt);
        }

        return split[1];
    }

    @NonNull
    private JSONObject parseAsJSON(@NonNull String json) {
        try {
            return new JSONObject(json);
        } catch (JSONException e) {
            throw new IllegalArgumentException("JWT payload is no JSON object: " + json);
        }
    }
}
