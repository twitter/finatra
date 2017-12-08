package com.twitter.hello.server;

import java.util.Collections;

import com.google.inject.Stage;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.twitter.finagle.http.Request;
import com.twitter.finagle.http.Response;
import com.twitter.finagle.http.Status;
import com.twitter.finatra.http.EmbeddedHttpServer;
import com.twitter.finatra.httpclient.RequestBuilder;

public class HelloWorldServerFeatureTest extends Assert {

    private static final EmbeddedHttpServer SERVER =
        new EmbeddedHttpServer(
            new HelloWorldServer(),
            Collections.emptyMap(),
            Stage.DEVELOPMENT);

    @BeforeClass
    public static void setUp() throws Exception {
        SERVER.start();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        SERVER.close();
    }

    /** test hello endpoint */
    @Test
    public void testHelloEndpoint() {
        Request request = RequestBuilder.get("/hello?name=Bob");
        Response response = SERVER.httpRequest(request);
        assertEquals(Status.Ok(), response.status());
        assertEquals("Hello Bob", response.contentString());
    }

    /** test goodbye endpoint */
    @Test
    public void testGoodbyeEndpoint() {
        Request request = RequestBuilder.get("/goodbye");
        Response response = SERVER.httpRequest(request);
        assertEquals(Status.Ok(), response.status());
        assertEquals(
            "{\"name\":\"guest\",\"message\":\"cya\",\"code\":123}",
            response.contentString());
    }

    /** test ping endpoint */
    @Test
    public void testPingEndpoint() {
        Request request = RequestBuilder.get("/ping");
        Response response = SERVER.httpRequest(request);
        assertEquals(Status.Ok(), response.status());
        assertEquals("pong", response.contentString());
    }

    /** test exception endpoint */
    @Test
    public void testExceptionEndpoint() {
        Request request = RequestBuilder.get("/exception");
        Response response = SERVER.httpRequest(request);
        assertEquals(Status.InternalServerError(), response.status());
        assertEquals("error processing request", response.contentString());
    }
}
