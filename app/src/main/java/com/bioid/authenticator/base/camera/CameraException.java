package com.bioid.authenticator.base.camera;

/**
 * Exception which is thrown if any camera related task did fail because of a technical reason.
 */
public class CameraException extends RuntimeException {

    public CameraException(String detailMessage) {
        super(detailMessage);
    }

    public CameraException(Throwable throwable) {
        super(throwable);
    }
}
