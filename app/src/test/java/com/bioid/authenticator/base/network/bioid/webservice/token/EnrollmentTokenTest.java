package com.bioid.authenticator.base.network.bioid.webservice.token;

import com.bioid.authenticator.BuildConfig;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)  // using Robolectric for org.json.JSONObject
@Config(constants = BuildConfig.class)
public class EnrollmentTokenTest {

    private static final String TOKEN = "token";
    private static final long EXPIRY_TIME = 1000;
    private static final JSONObject VERIFY_CLAIMS;
    private static final JSONObject ENROLLMENT_CLAIMS;

    static {
        try {
            VERIFY_CLAIMS = new JSONObject();
            VERIFY_CLAIMS.put("task", 261);
            VERIFY_CLAIMS.put("exp", EXPIRY_TIME);

            ENROLLMENT_CLAIMS = new JSONObject();
            ENROLLMENT_CLAIMS.put("task", 293);
            ENROLLMENT_CLAIMS.put("exp", EXPIRY_TIME);
        } catch (JSONException e) {
            throw new IllegalStateException("invalid test data", e);
        }
    }

    private JwtParser jwtParser;

    @Before
    public void setUp() throws Exception {
        jwtParser = mock(JwtParser.class);
    }

    @Test
    public void constructor_tokenIsStored() throws Exception {
        when(jwtParser.getPayload(TOKEN)).thenReturn(ENROLLMENT_CLAIMS);

        EnrollmentToken token = new EnrollmentToken(jwtParser, TOKEN);

        assertThat(token.getToken(), is(TOKEN));
    }

    @Test
    public void constructor_expiryTimeClaimWasExtractedCorrectly() throws Exception {
        when(jwtParser.getPayload(TOKEN)).thenReturn(ENROLLMENT_CLAIMS);

        EnrollmentToken token = new EnrollmentToken(jwtParser, TOKEN);

        assertThat(token.getExpirationTime(), is(EXPIRY_TIME));
    }

    @Test
    public void constructor_maxTriesClaimWasExtractedCorrectly() throws Exception {
        when(jwtParser.getPayload(TOKEN)).thenReturn(ENROLLMENT_CLAIMS);

        EnrollmentToken token = new EnrollmentToken(jwtParser, TOKEN);

        assertThat(token.getMaxTries(), is(5));
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_passingInAVerificationTokenThrowsException() throws Exception {
        when(jwtParser.getPayload(TOKEN)).thenReturn(VERIFY_CLAIMS);

        new EnrollmentToken(jwtParser, TOKEN);
    }
}