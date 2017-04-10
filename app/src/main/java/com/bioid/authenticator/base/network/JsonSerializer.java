package com.bioid.authenticator.base.network;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * JSON (De-)Serializer.
 */
class JsonSerializer {

    /**
     * Deserializes the given string representing a JSON object.
     */
    JSONObject toJsonObject(String s) throws JSONException {
        return new JSONObject(s);
    }
}
