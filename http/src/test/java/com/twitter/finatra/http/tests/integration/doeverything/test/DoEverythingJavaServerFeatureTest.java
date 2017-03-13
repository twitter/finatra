package com.twitter.finatra.http.tests.integration.doeverything.test;

import java.util.Collection;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Stage;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.twitter.finagle.http.HttpMuxer$;
import com.twitter.finagle.http.Method;
import com.twitter.finagle.http.Request;
import com.twitter.finagle.http.Response;
import com.twitter.finagle.http.Status;
import com.twitter.finatra.http.EmbeddedHttpServer;
import com.twitter.finatra.http.tests.integration.doeverything.main.DoEverythingJavaServer;
import com.twitter.finatra.httpclient.RequestBuilder;
import com.twitter.server.AdminHttpServer;

public class DoEverythingJavaServerFeatureTest extends Assert {

    private static final EmbeddedHttpServer SERVER =
        new EmbeddedHttpServer(
            new DoEverythingJavaServer(),
            ImmutableMap.of(),
            Stage.DEVELOPMENT);

    @BeforeClass
    public static void setUp() throws Exception {
        SERVER.start();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        SERVER.close();
    }

    @Test
    public void testHelloEndpoint() {
        Request request = RequestBuilder.get("/hello?name=Bob");
        Response response = SERVER.httpRequest(request);
        assertEquals(Status.Ok(), response.status());
        assertEquals("Hello Bob", response.contentString());
    }

    @Test
    public void testHeadersEndpoint() {
        Request request = RequestBuilder.get("/headers");
        Response response = SERVER.httpRequest(request);
        assertEquals(Status.Ok(), response.status());
        assertEquals("12", response.contentString());
    }

    @Test
    public void testNonInjectedHelloEndpoint() {
        Request request = RequestBuilder.get("/nonInjected/hello?name=Bob");
        Response response = SERVER.httpRequest(request);
        assertEquals(Status.Ok(), response.status());
        assertEquals("Hello Bob", response.contentString());
    }

    @Test
    public void testAdminFinatraEndpoint() {
        Request request = RequestBuilder.get("/admin/finatra/implied");
        Response response = SERVER.httpRequest(request);
        assertEquals(Status.Ok(), response.status());
        assertEquals("implied admin route no index", response.contentString());
    }

    @Test
    public void testAnyEndpoint() {
        Request request1 = RequestBuilder.get("/any");
        Response response1 = SERVER.httpRequest(request1);
        assertEquals(Status.Ok(), response1.status());

        Request request2 = RequestBuilder.post("/any");
        Response response2 = SERVER.httpRequest(request2);
        assertEquals(Status.Ok(), response2.status());

        Request request3 = RequestBuilder.head("/any");
        Response response3 = SERVER.httpRequest(request3);
        assertEquals(Status.Ok(), response3.status());

        Request request4 = RequestBuilder.put("/any");
        Response response4 = SERVER.httpRequest(request4);
        assertEquals(Status.Ok(), response4.status());
    }

    @Test
    public void testGoodbyeEndpoint() {
        Request request = RequestBuilder.get("/goodbye");
        Response response = SERVER.httpRequest(request);
        assertEquals(Status.Ok(), response.status());
        assertEquals(
                "{\"name\":\"guest\",\"message\":\"cya\",\"code\":123}",
                response.contentString());
    }

    @Test
    public void testHeadEndpoint() {
        Request request = RequestBuilder.head("/head");
        Response response = SERVER.httpRequest(request);
        assertEquals(Status.Ok(), response.status());
        assertEquals("", response.contentString()); //HEAD requests cannot have bodies
    }

    @Test
    public void testOtherEndpoints() {
        assertMethod(Method.Put());
        assertMethod(Method.Patch());
        assertMethod(Method.Delete());
        assertMethod(Method.Options());
    }

    @Test
    public void testQueryEndpoint() {
        Request request = RequestBuilder.get("/query?q=FooBar");
        Response response = SERVER.httpRequest(request);
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

    @Test
    public void testExceptionEndpoint() {
        Request request = RequestBuilder.get("/exception");
        Response response = SERVER.httpRequest(request);
        assertEquals(Status.InternalServerError(), response.status());
        assertEquals("error processing request", response.contentString());
    }

    @Test
    public void testFutureExceptionEndpoint() {
        Request request = RequestBuilder.get("/futureException");
        Response response = SERVER.httpRequest(request);
        assertEquals(Status.InternalServerError(), response.status());
        assertEquals("error processing request", response.contentString());
    }

    @Test
    public void testFutureThrowExceptionEndpoint() {
        Request request = RequestBuilder.get("/futureThrowException");
        Response response = SERVER.httpRequest(request);
        assertEquals(Status.InternalServerError(), response.status());
        assertEquals("error processing request", response.contentString());
    }

    @Test
    public void testAdminEndpoints() {
        Collection<AdminHttpServer.Route> routes =
                scala.collection.JavaConverters$.MODULE$.
                        asJavaCollectionConverter(SERVER.adminHttpServerRoutes()).
                        asJavaCollection();
        assertTrue(adminRoutePathExists(routes, "/admin/clients", Method.Get(), true));
        assertTrue(adminRoutePathExists(routes, "/admin/bar", Method.Post(), true));
        assertTrue(adminRoutePathExists(routes, "/admin/foo", Method.Get(), false));
        assertTrue(adminRoutePathExists(routes, "/admin/delete", Method.Delete(), false));

        assertTrue(HttpMuxer$.MODULE$.patterns().contains("/admin/finatra/"));
    }

    private Boolean adminRoutePathExists(
        Collection<AdminHttpServer.Route> routes,
        String path,
        Method method,
        Boolean includeInIndex) {
        Boolean result = false;
        for (AdminHttpServer.Route route : routes) {
            if (route.path().equalsIgnoreCase(path)
                    && route.method() == method
                    && route.includeInIndex() == includeInIndex) {
                result = true;
                break;
            }
        }
        return result;
    }

    private void assertMethod(Method httpMethod) {
        String methodName = httpMethod.toString().toLowerCase();
        Request request = RequestBuilder.create(httpMethod, "/" + methodName);
        Response response = SERVER.httpRequest(request);
        assertEquals(Status.Ok(), response.status());
        assertEquals(methodName, response.contentString());
    }
}
