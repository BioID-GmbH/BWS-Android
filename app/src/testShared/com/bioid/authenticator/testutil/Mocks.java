package com.bioid.authenticator.testutil;

import android.util.Size;

import com.bioid.authenticator.base.network.bioid.webservice.MovementDirection;
import com.bioid.authenticator.base.network.bioid.webservice.token.EnrollmentToken;
import com.bioid.authenticator.base.network.bioid.webservice.token.VerificationToken;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Provides helper functions to create mock objects for testing.
 */
public final class Mocks {

    private static final MovementDirection[][] CHALLENGES = new MovementDirection[][]{
            {MovementDirection.left, MovementDirection.right, MovementDirection.up},
            {MovementDirection.right, MovementDirection.left, MovementDirection.up},
            {MovementDirection.up, MovementDirection.down, MovementDirection.right}
    };

    /**
     * The android.jar for unit tests does not implement Size.
     */
    public static Size size(int width, int height) {
        Size mock = mock(Size.class);
        when(mock.getWidth()).thenReturn(width);
        when(mock.getHeight()).thenReturn(height);
        return mock;
    }

    public static VerificationToken verificationToken() {
        return mock(VerificationToken.class);
    }

    public static VerificationToken verificationTokenWithChallenges() {
        VerificationToken mock = mock(VerificationToken.class);
        when(mock.isChallengeResponse()).thenReturn(true);
        when(mock.getChallenges()).thenReturn(CHALLENGES);
        return mock;
    }

    public static EnrollmentToken enrollmentToken() {
        return mock(EnrollmentToken.class);
    }
}
