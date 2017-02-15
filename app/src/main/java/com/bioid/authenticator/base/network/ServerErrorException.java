package com.bioid.authenticator.base.network;

/**
 * Will be thrown if a HTTP request failed because of a internal server error.
 */
public class ServerErrorException extends RuntimeException {
    public ServerErrorException() {
    }
}
