package com.bioid.authenticator.base.network.bioid.webservice.token;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.bioid.authenticator.base.network.bioid.webservice.MovementDirection;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Base class for all tokens which can be used for various biometric operations on the BioID Webservice (BWS).
 * BWS uses JSON Web Tokens(JWT) to represent the issued claims.
 */
@SuppressWarnings("BooleanMethodIsAlwaysInverted")
public abstract class BwsToken {

    private static final int DEFAULT_MAX_TRIES = 3;

    @NonNull
    private final String token;
    private final int task;
    private final int traits;
    private final long expirationTime;
    @Nullable
    final MovementDirection[][] challenges;

    /**
     * Creates a new BwsToken object.
     *
     * @throws NullPointerException     if the token is null
     * @throws IllegalArgumentException if the token is not valid
     */
    BwsToken(@NonNull JwtParser jwtParser, @NonNull String token) {
        this.token = token;

        JSONObject claims = jwtParser.getPayload(token);
        this.task = extractTask(claims);
        this.traits = extractTraits(claims);
        this.expirationTime = extractExpirationTime(claims);
        this.challenges = extractChallenges(claims);
    }

    private int extractTask(@NonNull JSONObject claims) {
        try {
            return claims.getInt("task");
        } catch (JSONException e) {
            throw new IllegalArgumentException("JWT is missing 'task' claim");
        }
    }

    private int extractTraits(@NonNull JSONObject claims) {
        try {
            return claims.getInt("traits");
        } catch (JSONException e) {
            throw new IllegalArgumentException("JWT is missing 'traits' claim");
        }
    }

    private long extractExpirationTime(@NonNull JSONObject claims) {
        try {
            return claims.getLong("exp");
        } catch (JSONException e) {
            throw new IllegalArgumentException("JWT is missing 'exp' claim");
        }
    }

    @Nullable
    private MovementDirection[][] extractChallenges(@NonNull JSONObject claims) {
        try {
            // the property "challenge" is a string which represents a JSON array e.g. "[["up","down"],["left","right"]]"
            JSONArray challengeClaim = new JSONArray(claims.getString("challenge"));

            MovementDirection[][] challenges = new MovementDirection[challengeClaim.length()][];

            for (int i = 0; i < challengeClaim.length(); i++) {
                JSONArray challenge = challengeClaim.getJSONArray(i);
                challenges[i] = new MovementDirection[challenge.length()];

                for (int j = 0; j < challenge.length(); j++) {
                    challenges[i][j] = MovementDirection.valueOf(challenge.getString(j));
                }
            }

            return challenges;

        } catch (JSONException e) {
            // no challenge -> that is ok
            return null;
        }
    }

    /**
     * Does return the raw JWT token.
     */
    @NonNull
    public String getToken() {
        return token;
    }

    /**
     * Does return the maximum tries a user is allowed to perform.
     */
    public int getMaxTries() {
        int maxTries = task & TaskFlag.MaxTriesMask.value;
        return maxTries == 0 ? DEFAULT_MAX_TRIES : maxTries;
    }

    /**
     * Returns the Unix timestamp on after which the token is not valid any more.
     */
    public long getExpirationTime() {
        return expirationTime;
    }

    boolean isVerificationToken() {
        return !(isEnrollmentToken() || isIdentifyToken());
    }

    boolean isEnrollmentToken() {
        return (task & TaskFlag.Enroll.value) == TaskFlag.Enroll.value;
    }

    private boolean isIdentifyToken() {
        return (task & TaskFlag.Identify.value) == TaskFlag.Identify.value;
    }

    public boolean hasFaceTrait() {
        return hasTrait(TraitsFlag.Face);
    }

    public boolean hasPeriocularTrait() {
        return hasTrait(TraitsFlag.Periocular);
    }

    public boolean hasVoiceTrait() {
        return hasTrait(TraitsFlag.Voice);
    }

    private boolean hasTrait(@NonNull TraitsFlag trait) {
        return (traits & trait.value) == trait.value;
    }

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

    @SuppressWarnings("unused")
    private enum TaskFlag {

        Verify(0),
        Identify(0x10),
        Enroll(0x20),
        MaxTriesMask(0x0F),
        LiveDetection(0x100),
        ChallengeResponse(0x200),
        AutoEnroll(0x1000);

        final int value;

        TaskFlag(int value) {
            this.value = value;
        }
    }

    private enum TraitsFlag {

        Face(0x1),
        Periocular(0x2),
        Voice(0x4);

        final int value;

        TraitsFlag(int value) {
            this.value = value;
        }
    }
}
