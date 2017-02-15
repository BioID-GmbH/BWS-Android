package com.bioid.authenticator.base.network.bioid.webservice;

/**
 * Will be thrown if the provided images do not fulfill the challenge-response criteria.
 */
public class ChallengeResponseException extends RuntimeException {

    public ChallengeResponseException() {
    }
}
