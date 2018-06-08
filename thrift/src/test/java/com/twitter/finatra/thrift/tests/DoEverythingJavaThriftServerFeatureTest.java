package com.twitter.finatra.thrift.tests;

import java.util.Collections;

import scala.reflect.ClassTag$;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Stage;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;

import com.twitter.doeverything.thriftjava.Answer;
import com.twitter.doeverything.thriftjava.DoEverything;
import com.twitter.doeverything.thriftjava.Question;
import com.twitter.finagle.http.Response;
import com.twitter.finagle.http.Status;
import com.twitter.finatra.thrift.EmbeddedThriftServer;
import com.twitter.finatra.thrift.tests.doeverything.DoEverythingJavaThriftServer;
import com.twitter.util.Await;

public class DoEverythingJavaThriftServerFeatureTest extends Assert {

    private static final EmbeddedThriftServer SERVER =
        new EmbeddedThriftServer(
            new DoEverythingJavaThriftServer(),
            Collections.singletonMap("magicNum", "57"),
            Stage.DEVELOPMENT);
    private static final DoEverything.ServiceIface THRIFT_CLIENT =
            SERVER.thriftClient(
                "client123",
                ClassTag$.MODULE$.apply(DoEverything.ServiceIface.class));

    @AfterClass
    public static void tearDown() throws Exception {
        SERVER.close();
    }

    /** Test that methods are correctly added to the Registry */
    @Test
    public void testRegistryEntries() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();

        Response response = SERVER.httpGetAdmin(
            "/admin/registry.json",
            null,
            null,
            true,
            Status.Ok(),
            null,
            null);
        JsonNode json = objectMapper.readValue(response.getContentString(), JsonNode.class);
        JsonNode thriftNode = json.path("registry").path("library").path("finatra").path("thrift");
        JsonNode filters = thriftNode.get("filters");
        assertEquals(
            "com.twitter.finatra.thrift.filters.LoggingMDCFilter"
                + ".andThen(com.twitter.finatra.thrift.filters.TraceIdMDCFilter)"
                + ".andThen(com.twitter.finatra.thrift.filters.ThriftMDCFilter)"
                + ".andThen(com.twitter.finatra.thrift.filters.AccessLoggingFilter)"
                + ".andThen(com.twitter.finatra.thrift.filters.StatsFilter)",
            filters.asText());
        JsonNode methods = thriftNode.get("methods");

        assertEquals(6, methods.size());
    }

    /** test uppercase endpoint */
    @Test
    public void testUppercase() throws Exception {
        assertEquals("HI", Await.result(THRIFT_CLIENT.uppercase("Hi")));
    }

    /** test uppercase endpoint fails */
    @Test
    public void testUppercaseFailure() throws Exception {
        try {
            Await.result(THRIFT_CLIENT.uppercase("fail"));
            fail("Expected exception " + Exception.class + " never thrown");
        } catch (Exception e) {
            // expected
        }
    }

    /** test echo endpoint */
    @Test
    public void testEcho() throws Exception {
        assertEquals("hello", Await.result(THRIFT_CLIENT.echo("hello")));
    }

    /** test magicNum endpoint */
    @Test
    public void testMagicNum() throws Exception {
        assertEquals("57", Await.result(THRIFT_CLIENT.magicNum()));
    }

    /** test moreThanTwentyTwoArgs endpoint */
    @Test
    public void testMoreThanTwentyTwoArgs() throws Exception {
        assertEquals("handled", Await.result(THRIFT_CLIENT.moreThanTwentyTwoArgs(
            "one",
            "two",
            "three",
            "four",
            "five",
            "six",
            "seven",
            "eight",
            "nine",
            "ten",
            "eleven",
            "twelve",
            "thirteen",
            "fourteen",
            "fifteen",
            "sixteen",
            "seventeen",
            "eighteen",
            "nineteen",
            "twenty",
            "twentyone",
            "twentytwo",
            "twentythree")));
    }

    /** test ask endpoint */
    @Test
    public void testAsk() throws Exception {
        final Answer answer =
                Await.result(
                        THRIFT_CLIENT.ask(
                                new Question("What is the meaning of life?")));
        assertEquals(
                "The answer to the question: `What is the meaning of life?` is 42.",
                answer.getText());
    }
}
