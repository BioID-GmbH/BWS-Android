package com.bioid.authenticator.base.network.bioid.webservice;

/**
 * Will be thrown if the provided images do not prove that they are recorded from a live person.
 */
public class LiveDetectionException extends RuntimeException {

    public LiveDetectionException() {
    }
}
