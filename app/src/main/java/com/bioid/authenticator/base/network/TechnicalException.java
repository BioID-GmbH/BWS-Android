package com.bioid.authenticator.base.network;

/**
 * Will be thrown if a HTTP request failed because of any technical reason.
 */
public class TechnicalException extends RuntimeException {

    public TechnicalException(String detailMessage) {
        super(detailMessage);
    }

    public TechnicalException(Throwable throwable) {
        super(throwable);
    }

    public TechnicalException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }
}
