package com.bioid.authenticator.base.network.bioid.webservice;

import android.support.annotation.NonNull;

import com.bioid.authenticator.base.network.HttpRequest;
import com.bioid.authenticator.base.network.HttpRequestHelper;
import com.bioid.authenticator.base.network.HttpRequestHelper.Non200StatusException;
import com.bioid.authenticator.base.network.NoConnectionException;
import com.bioid.authenticator.base.network.ServerErrorException;
import com.bioid.authenticator.base.network.TechnicalException;
import com.bioid.authenticator.base.network.bioid.webservice.token.BwsTokenFactory;
import com.bioid.authenticator.base.network.bioid.webservice.token.EnrollmentToken;
import com.bioid.authenticator.base.network.bioid.webservice.token.VerificationToken;
import com.bioid.authenticator.testutil.Mocks;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BioIdWebserviceClientExtendedTest {

    private static final String VERIFICATION_TOKEN_CONTENT = "verify token";
    private static final String ENROLLMENT_TOKEN_CONTENT = "enroll token";
    private static final VerificationToken VERIFICATION_TOKEN = Mocks.verificationToken();
    private static final EnrollmentToken ENROLLMENT_TOKEN = Mocks.enrollmentToken();
    private static final String BCID = "bcid";

    // override request creation because HttpRequest creation causes DNS lookup (actually done by URL constructor)
    // this trick makes it easier than using mockito mocks because the request will be modified multiple times
    private class BioIdWebserviceClientExtendedForTest extends BioIdWebserviceClientExtended {

        private String calledWithBcid;
        private String calledWithTask;

        BioIdWebserviceClientExtendedForTest(HttpRequestHelper httpRequestHelper, BwsTokenFactory tokenFactory) {
            // using null dependencies makes sure the base class functionality won't be tested
            super(httpRequestHelper, null, null, tokenFactory);
        }

        @Override
        protected HttpRequest createNewTokenRequest(@NonNull String bcid, @NonNull String task) {
            calledWithBcid = bcid;
            calledWithTask = task;
            return tokenRequest;
        }
    }

    @Mock
    private HttpRequestHelper httpRequestHelper;
    @Mock
    private BwsTokenFactory tokenFactory;
    @Mock
    private HttpRequest tokenRequest;

    private BioIdWebserviceClientExtendedForTest bioIdWebserviceClient;

    @Before
    public void setUp() {
        bioIdWebserviceClient = new BioIdWebserviceClientExtendedForTest(httpRequestHelper, tokenFactory);
    }

    @Test
    public void requestVerificationToken_tokenWillBeReturned() {
        when(httpRequestHelper.asTextIfOk(tokenRequest)).thenReturn(VERIFICATION_TOKEN_CONTENT);
        when(tokenFactory.newVerificationToken(VERIFICATION_TOKEN_CONTENT)).thenReturn(VERIFICATION_TOKEN);

        VerificationToken result = bioIdWebserviceClient.requestVerificationToken(BCID);

        assertThat(bioIdWebserviceClient.calledWithBcid, is(BCID));
        assertThat(bioIdWebserviceClient.calledWithTask, is(BioIdWebserviceClientExtended.VERIFICATION_TASK));
        assertThat(result, is(VERIFICATION_TOKEN));
    }

    @Test(expected = TechnicalException.class)
    public void requestVerificationToken_technicalExceptionWillPassThrough() {
        when(httpRequestHelper.asTextIfOk(tokenRequest)).thenThrow(TechnicalException.class);
        bioIdWebserviceClient.requestVerificationToken(BCID);
    }

    @Test(expected = NoConnectionException.class)
    public void requestVerificationToken_noConnectionExceptionWillPassThrough() {
        when(httpRequestHelper.asTextIfOk(tokenRequest)).thenThrow(NoConnectionException.class);
        bioIdWebserviceClient.requestVerificationToken(BCID);
    }

    @Test(expected = ServerErrorException.class)
    public void requestVerificationToken_serverErrorExceptionWillPassThrough() {
        when(httpRequestHelper.asTextIfOk(tokenRequest)).thenThrow(ServerErrorException.class);
        bioIdWebserviceClient.requestVerificationToken(BCID);
    }

    @Test(expected = TechnicalException.class)
    public void requestVerificationToken_non200StatusCodeLeadsToTechnicalException() {
        when(httpRequestHelper.asTextIfOk(tokenRequest)).thenThrow(Non200StatusException.class);
        bioIdWebserviceClient.requestVerificationToken(BCID);
    }

    @Test
    public void requestEnrollmentToken_tokenWillBeReturned() {
        when(httpRequestHelper.asTextIfOk(tokenRequest)).thenReturn(ENROLLMENT_TOKEN_CONTENT);
        when(tokenFactory.newEnrollmentToken(ENROLLMENT_TOKEN_CONTENT)).thenReturn(ENROLLMENT_TOKEN);

        EnrollmentToken result = bioIdWebserviceClient.requestEnrollmentToken(BCID);

        assertThat(bioIdWebserviceClient.calledWithBcid, is(BCID));
        assertThat(bioIdWebserviceClient.calledWithTask, is(BioIdWebserviceClientExtended.ENROLLMENT_TASK));
        assertThat(result, is(ENROLLMENT_TOKEN));
    }

    @Test(expected = TechnicalException.class)
    public void requestEnrollmentToken_technicalExceptionWillPassThrough() {
        when(httpRequestHelper.asTextIfOk(tokenRequest)).thenThrow(TechnicalException.class);
        bioIdWebserviceClient.requestEnrollmentToken(BCID);
    }

    @Test(expected = NoConnectionException.class)
    public void requestEnrollmentToken_noConnectionExceptionWillPassThrough() {
        when(httpRequestHelper.asTextIfOk(tokenRequest)).thenThrow(NoConnectionException.class);
        bioIdWebserviceClient.requestEnrollmentToken(BCID);
    }

    @Test(expected = ServerErrorException.class)
    public void requestEnrollmentToken_serverErrorExceptionWillPassThrough() {
        when(httpRequestHelper.asTextIfOk(tokenRequest)).thenThrow(ServerErrorException.class);
        bioIdWebserviceClient.requestEnrollmentToken(BCID);
    }

    @Test(expected = TechnicalException.class)
    public void requestEnrollmentToken_non200StatusCodeLeadsToTechnicalException() {
        when(httpRequestHelper.asTextIfOk(tokenRequest)).thenThrow(Non200StatusException.class);
        bioIdWebserviceClient.requestEnrollmentToken(BCID);
    }
}
