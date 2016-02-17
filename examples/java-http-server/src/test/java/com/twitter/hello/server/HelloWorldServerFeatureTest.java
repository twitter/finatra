package com.twitter.hello.server;

import org.junit.Assert;
import org.junit.Test;

import com.twitter.finagle.http.Request;
import com.twitter.finagle.http.Response;
import com.twitter.finagle.http.Status;
import com.twitter.finatra.http.test.EmbeddedHttpServer;
import static com.twitter.finatra.httpclient.RequestBuilder.*;

public class HelloWorldServerFeatureTest extends Assert {

    private EmbeddedHttpServer server =
            new EmbeddedHttpServer(new HelloWorldServer());

    @Test
    public void testHelloEndpoint() {
        Request request = get("/hello?name=Bob");
        Response response = server.httpRequest(request);
        assertEquals(Status.Ok(), response.status());
        assertEquals("Hello Bob", response.contentString());
    }

    @Test
    public void testGoodbyeEndpoint() {
        Request request = get("/goodbye");
        Response response = server.httpRequest(request);
        assertEquals(Status.Ok(), response.status());
        assertEquals("{\"name\":\"guest\",\"message\":\"cya\",\"code\":123}", response.contentString());
    }
}
