package com.bioid.authenticator.base.network.bioid.webservice.token;

import android.content.Context;
import android.support.annotation.NonNull;

import com.bioid.authenticator.base.network.bioid.webservice.BioIdWebserviceClientExtended;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BwsTokenProviderTest {

    private static final String BCID = "123";
    private static final VerificationToken VERIFICATION_TOKEN = new VerificationToken("verify");
    private static final EnrollmentToken ENROLLMENT_TOKEN = new EnrollmentToken("enroll");

    @Mock
    private Context ctx;
    @Mock
    private BioIdWebserviceClientExtended bioIdWebserviceClient;

    private BwsTokenProvider bwsTokenProvider = new BwsTokenProviderForTest();

    private class BwsTokenProviderForTest extends BwsTokenProvider {

        private BwsTokenProviderForTest() {
            super(BCID);
        }

        @Override
        BioIdWebserviceClientExtended createBwsClient(@NonNull Context ctx) {
            return bioIdWebserviceClient;
        }
    }

    @Test
    public void requestVerificationToken() throws Exception {
        when(bioIdWebserviceClient.requestVerificationToken(BCID)).thenReturn(VERIFICATION_TOKEN);

        VerificationToken token = bwsTokenProvider.requestVerificationToken(ctx);

        assertThat(token, is(VERIFICATION_TOKEN));
    }

    @Test
    public void requestEnrollmentToken() throws Exception {
        when(bioIdWebserviceClient.requestEnrollmentToken(BCID)).thenReturn(ENROLLMENT_TOKEN);

        EnrollmentToken token = bwsTokenProvider.requestEnrollmentToken(ctx);

        assertThat(token, is(ENROLLMENT_TOKEN));
    }
}
