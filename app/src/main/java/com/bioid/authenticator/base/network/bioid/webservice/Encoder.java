package com.bioid.authenticator.base.network.bioid.webservice;

import android.support.annotation.NonNull;
import android.util.Base64;

import java.nio.charset.Charset;

/**
 * Provides methods to encode data.
 */
public class Encoder {

    private static final Charset UTF8 = Charset.forName("UTF-8");

    /**
     * Encodes the given bytes as Base64 (omits all line terminators).
     */
    @NonNull
    byte[] encodeAsBase64(@NonNull byte[] bytes) {
        return Base64.encode(bytes, Base64.NO_WRAP);
    }

    /**
     * Decodes the given Base64 URL compatible string (using UTF-8 only).
     */
    @NonNull
    public String decodeBase64(@NonNull String string) {
        byte[] encoded = string.getBytes(UTF8);
        byte[] decoded = Base64.decode(encoded, Base64.URL_SAFE);
        return new String(decoded, UTF8);
    }
}
