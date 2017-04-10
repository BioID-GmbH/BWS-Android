package com.bioid.authenticator.base.network.bioid.webservice.token;

/**
 * EnrollmentToken which can be used for the enrollment process on the BioID Webservice (BWS).
 */
public class EnrollmentToken extends BwsToken {

    // use BwsTokenFactory instead
    EnrollmentToken(JwtParser jwtParser, String token) {
        super(jwtParser, token);
        validate();
    }

    private void validate() {
        if (!isEnrollmentToken()) {
            throw new IllegalArgumentException("this token is not intended for enrollment");
        }
    }

    @Override
    public String toString() {
        return "EnrollmentToken{" +
                "token='" + getToken() + '\'' +
                ", expirationTime=" + getExpirationTime() +
                ", maxTries=" + getMaxTries() +
                '}';
    }
}
