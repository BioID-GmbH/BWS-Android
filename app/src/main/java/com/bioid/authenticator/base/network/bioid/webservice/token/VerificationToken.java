package com.bioid.authenticator.base.network.bioid.webservice.token;

import android.support.annotation.NonNull;

import com.bioid.authenticator.base.network.bioid.webservice.MovementDirection;

import java.util.Arrays;

/**
 * VerificationToken which can be used for the verification process on the BioID Webservice (BWS).
 */
public class VerificationToken extends BwsToken {

    // use BwsTokenFactory instead
    VerificationToken(JwtParser jwtParser, String token) {
        super(jwtParser, token);
        validate();
    }

    private void validate() {
        if (!isVerificationToken()) {
            throw new IllegalArgumentException("this token is not intended for verification");
        }
    }

    /**
     * Returns true if Challenge Response should be used for the verification.
     */
    public boolean isChallengeResponse() {
        return challenges != null;
    }

    @NonNull
    public MovementDirection[][] getChallenges() {
        if (challenges == null) {
            throw new IllegalStateException("token does not support challenge-response");
        }
        return challenges;
    }

    @Override
    public String toString() {
        return "VerificationToken{" +
                "token='" + getToken() + '\'' +
                ", expirationTime=" + getExpirationTime() +
                ", maxTries=" + getMaxTries() +
                ", hasFaceTrait=" + hasFaceTrait() +
                ", hasPeriocularTrait=" + hasPeriocularTrait() +
                ", hasVoiceTrait=" + hasVoiceTrait() +
                ", challenges=" + Arrays.deepToString(challenges) +
                '}';
    }
}
