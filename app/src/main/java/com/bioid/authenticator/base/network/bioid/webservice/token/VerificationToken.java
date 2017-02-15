package com.bioid.authenticator.base.network.bioid.webservice.token;

/**
 * VerificationToken which can be used for the verification process on the BioID Webservice (BWS).
 */
public final class VerificationToken extends BwsToken {

    /**
     * Creates a new VerificationToken object.
     *
     * @throws NullPointerException if the token is null
     */
    public VerificationToken(String token) {
        super(token);
    }
}
