package com.twitter.finatra.integration.test;

import org.junit.Assert;
import org.junit.Test;

import com.twitter.finagle.http.Method;
import com.twitter.finagle.http.Methods;
import com.twitter.finagle.http.Request;
import com.twitter.finagle.http.Response;
import com.twitter.finagle.http.Status;
import com.twitter.finatra.http.EmbeddedHttpServer;
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
    public void testAnyEndpoint() {
        Request request1 = RequestBuilder.get("/any");
        Response response1 = server.httpRequest(request1);
        assertEquals(Status.Ok(), response1.status());

        Request request2 = RequestBuilder.post("/any");
        Response response2 = server.httpRequest(request2);
        assertEquals(Status.Ok(), response2.status());

        Request request3 = RequestBuilder.head("/any");
        Response response3 = server.httpRequest(request3);
        assertEquals(Status.Ok(), response3.status());

        Request request4 = RequestBuilder.put("/any");
        Response response4 = server.httpRequest(request4);
        assertEquals(Status.Ok(), response4.status());
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

    @Test
    public void testQueryEndpoint() {
        Request request = RequestBuilder.get("/query?q=FooBar");
        Response response = server.httpRequest(request);
        assertEquals(Status.Ok(), response.status());
        assertEquals(
                "{\"query\":\"FooBar\","
                    + "\"numResults\":\"5\","
                    + "\"results\":"
                    +     "{\"a\":\"1\",\"b\":\"2\",\"c\":\"3\",\"d\":\"4\",\"e\":\"5\"},"
                    + "\"user\":\"Bob\","
                    + "\"timestamp\":\"Thu, 19 May 2016 00:00:00 +00:00\"}",
                response.contentString());
    }

    private void assertMethod(Method httpMethod) {
        String methodName = httpMethod.toString().toLowerCase();
        Request request = RequestBuilder.create(httpMethod, "/" + methodName);
        Response response = server.httpRequest(request);
        assertEquals(Status.Ok(), response.status());
        assertEquals(methodName, response.contentString());
    }
}
