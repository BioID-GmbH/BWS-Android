package com.bioid.authenticator.base.network.bioid.webservice.token;

import com.bioid.authenticator.BuildConfig;
import com.bioid.authenticator.base.network.bioid.webservice.MovementDirection;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static com.bioid.authenticator.base.network.bioid.webservice.MovementDirection.down;
import static com.bioid.authenticator.base.network.bioid.webservice.MovementDirection.left;
import static com.bioid.authenticator.base.network.bioid.webservice.MovementDirection.right;
import static com.bioid.authenticator.base.network.bioid.webservice.MovementDirection.up;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)  // using Robolectric for org.json.JSONObject
@Config(constants = BuildConfig.class)
public class VerificationTokenTest {

    private static final String TOKEN = "token";
    private static final long EXPIRY_TIME = 1000;
    private static final JSONObject ENROLLMENT_CLAIMS;
    private static final String CHALLENGES = "[[\"right\",\"left\"],[\"up\",\"down\"]]";
    private static final MovementDirection[][] CHALLENGES_PARSED = new MovementDirection[][]{
            new MovementDirection[]{right, left},
            new MovementDirection[]{up, down}
    };
    private static final JSONObject VERIFY_CLAIMS_WITH_CHALLENGE_RESPONSE;
    private static final JSONObject VERIFY_CLAIMS_WITHOUT_CHALLENGE_RESPONSE;

    static {
        try {
            ENROLLMENT_CLAIMS = new JSONObject();
            ENROLLMENT_CLAIMS.put("task", 293);
            ENROLLMENT_CLAIMS.put("exp", EXPIRY_TIME);

            VERIFY_CLAIMS_WITH_CHALLENGE_RESPONSE = new JSONObject();
            VERIFY_CLAIMS_WITH_CHALLENGE_RESPONSE.put("task", 261);
            VERIFY_CLAIMS_WITH_CHALLENGE_RESPONSE.put("exp", EXPIRY_TIME);
            VERIFY_CLAIMS_WITH_CHALLENGE_RESPONSE.put("challenge", CHALLENGES);

            VERIFY_CLAIMS_WITHOUT_CHALLENGE_RESPONSE = new JSONObject();
            VERIFY_CLAIMS_WITHOUT_CHALLENGE_RESPONSE.put("task", 773);
            VERIFY_CLAIMS_WITHOUT_CHALLENGE_RESPONSE.put("exp", EXPIRY_TIME);
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
        when(jwtParser.getPayload(TOKEN)).thenReturn(VERIFY_CLAIMS_WITH_CHALLENGE_RESPONSE);

        VerificationToken token = new VerificationToken(jwtParser, TOKEN);

        assertThat(token.getToken(), is(TOKEN));
    }

    @Test
    public void constructor_expiryTimeClaimWasExtractedCorrectly() throws Exception {
        when(jwtParser.getPayload(TOKEN)).thenReturn(VERIFY_CLAIMS_WITH_CHALLENGE_RESPONSE);

        VerificationToken token = new VerificationToken(jwtParser, TOKEN);

        assertThat(token.getExpirationTime(), is(EXPIRY_TIME));
    }

    @Test
    public void constructor_maxTriesClaimWasExtractedCorrectly() throws Exception {
        when(jwtParser.getPayload(TOKEN)).thenReturn(VERIFY_CLAIMS_WITH_CHALLENGE_RESPONSE);

        VerificationToken token = new VerificationToken(jwtParser, TOKEN);

        assertThat(token.getMaxTries(), is(5));
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_passingInAEnrollmentTokenThrowsException() throws Exception {
        when(jwtParser.getPayload(TOKEN)).thenReturn(ENROLLMENT_CLAIMS);

        new VerificationToken(jwtParser, TOKEN);
    }

    @Test
    public void isChallengeResponse_returnsFalse() throws Exception {
        when(jwtParser.getPayload(TOKEN)).thenReturn(VERIFY_CLAIMS_WITHOUT_CHALLENGE_RESPONSE);

        VerificationToken token = new VerificationToken(jwtParser, TOKEN);

        assertThat(token.isChallengeResponse(), is(false));
    }

    @Test
    public void isChallengeResponse_returnsTrue() throws Exception {
        when(jwtParser.getPayload(TOKEN)).thenReturn(VERIFY_CLAIMS_WITH_CHALLENGE_RESPONSE);

        VerificationToken token = new VerificationToken(jwtParser, TOKEN);

        assertThat(token.isChallengeResponse(), is(true));
    }

    @Test
    public void getChallenges_returnsChallenges() throws Exception {
        when(jwtParser.getPayload(TOKEN)).thenReturn(VERIFY_CLAIMS_WITH_CHALLENGE_RESPONSE);

        VerificationToken token = new VerificationToken(jwtParser, TOKEN);

        assertThat(token.getChallenges(), is(CHALLENGES_PARSED));
    }

    @Test(expected = IllegalStateException.class)
    public void getChallenges_throwsExceptionIfTokenIsNotForChallengeResponse() throws Exception {
        when(jwtParser.getPayload(TOKEN)).thenReturn(VERIFY_CLAIMS_WITHOUT_CHALLENGE_RESPONSE);

        new VerificationToken(jwtParser, TOKEN).getChallenges();
    }
}