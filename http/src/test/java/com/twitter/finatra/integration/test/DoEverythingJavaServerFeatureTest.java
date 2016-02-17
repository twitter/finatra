package com.twitter.finatra.integration.test;

import org.junit.Assert;
import org.junit.Test;

import com.twitter.finagle.http.Method;
import com.twitter.finagle.http.Methods;
import com.twitter.finagle.http.Request;
import com.twitter.finagle.http.Response;
import com.twitter.finagle.http.Status;
import com.twitter.finatra.http.test.EmbeddedHttpServer;
import com.twitter.finatra.httpclient.RequestBuilder;

import com.twitter.finatra.integration.main.DoEverythingJavaServer;

public class DoEverythingJavaServerFeatureTest extends Assert {

    private EmbeddedHttpServer server =
            new EmbeddedHttpServer(new DoEverythingJavaServer());

    @Test
    public void testHelloEndpoint() {
        Request request = RequestBuilder.get("/hello?name=Bob");
        Response response = server.httpRequest(request);
        assertEquals(Status.Ok(), response.status());
        assertEquals("Hello Bob", response.contentString());
    }

    @Test
    public void testGoodbyeEndpoint() {
        Request request = RequestBuilder.get("/goodbye");
        Response response = server.httpRequest(request);
        assertEquals(Status.Ok(), response.status());
        assertEquals(
                "{\"name\":\"guest\",\"message\":\"cya\",\"code\":123}",
                response.contentString());
    }

    @Test
    public void testHeadEndpoint() {
        Request request = RequestBuilder.head("/head");
        Response response = server.httpRequest(request);
        assertEquals(Status.Ok(), response.status());
        assertEquals("", response.contentString()); //HEAD requests cannot have bodies
    }

    @Test
    public void testOtherEndpoints() {
        assertMethod(Methods.PUT);
        assertMethod(Methods.PATCH);
        assertMethod(Methods.DELETE);
        assertMethod(Methods.OPTIONS);
    }

    private void assertMethod(Method httpMethod) {
        String methodName = httpMethod.toString().toLowerCase();
        Request request = RequestBuilder.create(httpMethod, "/" + methodName);
        Response response = server.httpRequest(request);
        assertEquals(Status.Ok(), response.status());
        assertEquals(methodName, response.contentString());
    }
}
