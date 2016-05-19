package com.twitter.hello.server;

import org.junit.Assert;
import org.junit.Test;

import com.twitter.finagle.http.Request;
import com.twitter.finagle.http.Response;
import com.twitter.finagle.http.Status;
import com.twitter.finatra.http.EmbeddedHttpServer;
import com.twitter.finatra.httpclient.RequestBuilder;

public class HelloWorldServerFeatureTest extends Assert {

    private EmbeddedHttpServer server =
            new EmbeddedHttpServer(new HelloWorldServer());

    /** test hello endpoint */
    @Test
    public void testHelloEndpoint() {
        Request request = RequestBuilder.get("/hello?name=Bob");
        Response response = server.httpRequest(request);
        assertEquals(Status.Ok(), response.status());
        assertEquals("Hello Bob", response.contentString());
    }

    /** test goodbye endpoint */
    @Test
    public void testGoodbyeEndpoint() {
        Request request = RequestBuilder.get("/goodbye");
        Response response = server.httpRequest(request);
        assertEquals(Status.Ok(), response.status());
        assertEquals(
                "{\"name\":\"guest\",\"message\":\"cya\",\"code\":123}",
                response.contentString());
    }

    /** test ping endpoint */
    @Test
    public void testPingEndpoint() {
        Request request = RequestBuilder.get("/ping");
        Response response = server.httpRequest(request);
        assertEquals(Status.Ok(), response.status());
        assertEquals("pong", response.contentString());
    }
}
