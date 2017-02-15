package com.bioid.authenticator.base.network;

/**
 * Will be thrown if a HTTP request failed because no connection could be established.
 */
public class NoConnectionException extends RuntimeException {

    public NoConnectionException(Throwable throwable) {
        super(throwable);
    }
}
