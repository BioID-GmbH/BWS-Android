package com.bioid.authenticator.base.network.bioid.webservice.token;

/**
 * EnrollmentToken which can be used for the enrollment process on the BioID Webservice (BWS).
 */
public final class EnrollmentToken extends BwsToken {

    /**
     * Creates a new EnrollmentToken object.
     *
     * @throws NullPointerException if the token is null
     */
    public EnrollmentToken(String token) {
        super(token);
    }
}
