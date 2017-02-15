package com.bioid.authenticator.base.network.bioid.webservice;

import android.support.annotation.NonNull;
import android.util.Base64;

/**
 * Provides methods to encode data.
 */
class Encoder {

    /**
     * Encodes the given bytes as Base64 (omits all line terminators).
     */
    @NonNull
    byte[] encodeAsBase64(@NonNull byte[] bytes) {
        return Base64.encode(bytes, Base64.NO_WRAP);
    }
}
