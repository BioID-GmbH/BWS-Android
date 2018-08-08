package com.bioid.authenticator.base.network;

import android.support.annotation.NonNull;

import com.bioid.authenticator.base.logging.LoggingHelper;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static junit.framework.Assert.fail;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class HttpRequestHelperTest {

    private static final String JSON_AS_STRING = "{}";

    @Mock
    private LoggingHelper log;
    @Mock
    private JSONObject jsonObject;
    @Mock
    private HttpRequest request;
    @Mock
    private JsonSerializer jsonSerializer;

    private HttpRequestHelper httpRequestHelper;

    @Before
    public void setUp() throws Exception {

        httpRequestHelper = new HttpRequestHelper(log, jsonSerializer) {
            @Override
            protected String getStopwatchSessionId(@NonNull HttpRequest request) {
                return "test";
            }
        };

        when(request.code()).thenReturn(200);
        when(request.body("UTF-8")).thenReturn(JSON_AS_STRING);
        when(jsonSerializer.toJsonObject(JSON_AS_STRING)).thenReturn(jsonObject);
    }

    @Test
    public void testExecute_doesExecuteRequest() {
        httpRequestHelper.execute(request);
        verify(request).code();
    }

    @Test(expected = NoConnectionException.class)
    public void testExecute_throwsExceptionIfNoConnectionCouldBeEstablished() {
        doThrow(HttpRequest.HttpRequestException.class).when(request).code();
        httpRequestHelper.execute(request);
    }

    @Test
    public void testAsTextIfOk_returnsResponseBodyOnStatus200() {
        String result = httpRequestHelper.asTextIfOk(request);
        assertThat(result, is(JSON_AS_STRING));
    }

    @Test(expected = ServerErrorException.class)
    public void testAsTextIfOk_throwsServerErrorExceptionOnStatus500() {
        when(request.code()).thenReturn(500);

        httpRequestHelper.asTextIfOk(request);
    }

    @Test
    public void testAsTextIfOk_throwsExceptionOnNon200Status() {
        when(request.code()).thenReturn(404);

        try {
            httpRequestHelper.asTextIfOk(request);

        } catch (HttpRequestHelper.Non200StatusException e) {
            assertThat(e.getStatus(), is(404));
            return;
        }
        fail("no Non200StatusException thrown");
    }

    @Test(expected = NoConnectionException.class)
    public void testAsTextIfOk_throwsExceptionIfNoConnectionCouldBeEstablished() {
        doThrow(HttpRequest.HttpRequestException.class).when(request).code();
        httpRequestHelper.asTextIfOk(request);
    }

    @Test
    public void testAsJsonIfOk_returnsJsonObjectOnStatus200() {
        JSONObject result = httpRequestHelper.asJsonIfOk(request);
        assertThat(result, is(jsonObject));
    }

    @Test(expected = ServerErrorException.class)
    public void testAsJsonIfOk_throwsServerErrorExceptionOnStatus500() {
        when(request.code()).thenReturn(500);

        httpRequestHelper.asJsonIfOk(request);
    }

    @Test
    public void testAsJsonIfOk_throwsExceptionOnNon200Status() {
        when(request.code()).thenReturn(404);

        try {
            httpRequestHelper.asJsonIfOk(request);

        } catch (HttpRequestHelper.Non200StatusException e) {
            assertThat(e.getStatus(), is(404));
            return;
        }
        fail("no Non200StatusException thrown");
    }

    @Test(expected = NoConnectionException.class)
    public void testAsJsonIfOk_throwsExceptionIfNoConnectionCouldBeEstablished() {
        doThrow(HttpRequest.HttpRequestException.class).when(request).code();
        httpRequestHelper.asJsonIfOk(request);
    }

    @Test(expected = TechnicalException.class)
    public void testAsJsonIfOk_throwsExceptionIfJsonDeserializationFailed() throws Exception {
        doThrow(JSONException.class).when(jsonSerializer).toJsonObject(JSON_AS_STRING);
        httpRequestHelper.asJsonIfOk(request);
    }
}