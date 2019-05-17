package com.twitter.hello.server;

import java.util.Collections;

import com.google.inject.Stage;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.twitter.finagle.http.Request;
import com.twitter.finagle.http.Response;
import com.twitter.finagle.http.Status;
import com.twitter.finatra.http.EmbeddedHttpServer;
import com.twitter.finatra.httpclient.RequestBuilder;
import com.twitter.inject.annotations.Flags;

public class HelloWorldServerFeatureTest extends Assert {

    private static final EmbeddedHttpServer SERVER = setup();

    private static EmbeddedHttpServer setup() {
        EmbeddedHttpServer server = new EmbeddedHttpServer(
            new HelloWorldServer(),
            Collections.emptyMap(),
            Stage.DEVELOPMENT);

        server.bindClass(Integer.class, Flags.named("magic.number"), 42);
        server.bindClass(Integer.class, Flags.named("module.magic.number"), 9999);
        return server;
    }

    @BeforeClass
    public static void setUp() throws Exception {
        SERVER.start();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        SERVER.close();
    }

    @After
    public void printStats() throws Exception {
        SERVER.printStats(true);
        SERVER.clearStats();
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

    /** test echo endpoint */
    @Test
    public void testEchoEndpoint() {
        Request request = RequestBuilder.get("/echo?q=hello%20world");
        Response response = SERVER.httpRequest(request);
        assertEquals(Status.Ok(), response.status());
        assertEquals("hello world", response.contentString());
    }

    /** test exception endpoint */
    @Test
    public void testExceptionEndpoint() {
        Request request = RequestBuilder.get("/exception");
        Response response = SERVER.httpRequest(request);
        assertEquals(Status.InternalServerError(), response.status());
        assertEquals("error processing request", response.contentString());
    }

    /** test magicNum endpoint */
    @Test
    public void testMagicNumEndpoint() {
        Request request = RequestBuilder.get("/magicNum");
        Response response = SERVER.httpRequest(request);
        assertEquals(Status.Ok(), response.status());
        assertEquals("42", response.contentString());
    }

    /** test prefixed magicNum endpoint */
    @Test
    public void testPrefixedMagicNumEndpoint() {
        Request request = RequestBuilder.get("/v2/magicNum");
        Response response = SERVER.httpRequest(request);
        assertEquals(Status.Ok(), response.status());
        assertEquals("42", response.contentString());
    }

    /** test moduleMagicNum endpoint */
    @Test
    public void testModuleMagicNumEndpoint() {
        Request request = RequestBuilder.get("/moduleMagicNum");
        Response response = SERVER.httpRequest(request);
        assertEquals(Status.Ok(), response.status());
        assertEquals("9999", response.contentString());
    }

    /** test moduleMagicFloatNum endpoint; no override binding */
    @Test
    public void testModuleMagicFloatingNumEndpoint() {
        Request request = RequestBuilder.get("/moduleMagicFloatNum");
        Response response = SERVER.httpRequest(request);
        assertEquals(Status.Ok(), response.status());
        assertEquals("3.1459", response.contentString());
    }

    /** test anyMethod endpoint */
    @Test
    public void testAnyMethodEndpoint() {
        Request request = RequestBuilder.get("/any/method");
        Response response = SERVER.httpRequest(request);
        assertEquals(Status.Ok(), response.status());
        assertEquals("GET", response.contentString());

        request = RequestBuilder.post("/any/method");
        response = SERVER.httpRequest(request);
        assertEquals(Status.Ok(), response.status());
        assertEquals("POST", response.contentString());

        request = RequestBuilder.head("/any/method");
        response = SERVER.httpRequest(request);
        assertEquals(Status.Ok(), response.status());
        assertEquals("", response.contentString());

        request = RequestBuilder.trace("/any/method");
        response = SERVER.httpRequest(request);
        assertEquals(Status.Ok(), response.status());
        assertEquals("", response.contentString());
    }
}
