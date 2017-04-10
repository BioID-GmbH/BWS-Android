package com.bioid.authenticator.base.network.bioid.webservice.token;

import com.bioid.authenticator.BuildConfig;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(RobolectricTestRunner.class)  // using Robolectric because the Encoder uses Android classes
@Config(constants = BuildConfig.class)
public class JwtParserTest {

    // generated using https://jwt.io/
    @SuppressWarnings("SpellCheckingInspection")
    private static final String JWT = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWV9.TJVA95OrM7E2cBab30RMHrHDcEfxjoYZgeFONFh7HgQ";

    private final JwtParser jwtParser = new JwtParser();

    @Test
    public void getPayload_doesExtractThePayloadFromTheJwt() throws Exception {
        JSONObject result = jwtParser.getPayload(JWT);

        assertThat(result.getString("sub"), is("1234567890"));
        assertThat(result.getString("name"), is("John Doe"));
        assertThat(result.getBoolean("admin"), is(true));
    }

    @Test(expected = IllegalArgumentException.class)
    public void getPayload_throwsExceptionIfJwtIsNotParsable() throws Exception {
        jwtParser.getPayload("NOT_A_JWT");
    }
}