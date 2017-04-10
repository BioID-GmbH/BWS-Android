package com.bioid.authenticator.base.network.bioid.webservice.token;

import android.support.annotation.NonNull;

/**
 * Factory to create {@link BwsToken} instances.
 * <p>
 * This is primarily useful for testing because the constructors are validating the token.
 */
public class BwsTokenFactory {

    private final JwtParser jwtParser = new JwtParser();

    /**
     * Creates a new {@link EnrollmentToken}.
     *
     * @throws NullPointerException     if the token is null
     * @throws IllegalArgumentException if the token is not valid
     */
    public VerificationToken newVerificationToken(@NonNull String token) {
        return new VerificationToken(jwtParser, token);
    }

    /**
     * Creates a new {@link EnrollmentToken}.
     *
     * @throws NullPointerException     if the token is null
     * @throws IllegalArgumentException if the token is not valid
     */
    public EnrollmentToken newEnrollmentToken(@NonNull String token) {
        return new EnrollmentToken(jwtParser, token);
    }
}
