package com.bioid.authenticator.base.network.bioid.webservice.token;

/**
 * Base class for all tokens which can be used for various biometric operations on the BioID Webservice (BWS).
 */
public abstract class BwsToken {

    private final String token;

    /**
     * Creates a new BwsToken object.
     *
     * @throws NullPointerException if the token is null
     */
    BwsToken(String token) {
        if (token == null) {
            throw new NullPointerException("token can not be null");
        }
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "token='" + token + '\'' +
                '}';
    }

    /**
     * BwsTokens are only equal if there token is equal and they are from the same concrete type.
     * For example a VerificationToken with "ABC" in it is NOT equal to a EnrollmentToken with "ABC" in it because they are not
     * interchangeable in use.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BwsToken bwsToken = (BwsToken) o;

        return token.equals(bwsToken.token);

    }

    @Override
    public int hashCode() {
        return token.hashCode();
    }
}
