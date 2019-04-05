package com.twitter.finatra.thrift.tests;

import java.util.Collections;
import java.util.Map;

import scala.reflect.ClassTag$;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Stage;

import org.apache.thrift.TApplicationException;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;

import com.twitter.doeverything.thriftjava.Answer;
import com.twitter.doeverything.thriftjava.DoEverything;
import com.twitter.doeverything.thriftjava.Question;
import com.twitter.finagle.RequestException;
import com.twitter.finagle.http.Response;
import com.twitter.finagle.http.Status;
import com.twitter.finatra.thrift.EmbeddedThriftServer;
import com.twitter.finatra.thrift.tests.doeverything.DoEverythingJavaThriftServer;
import com.twitter.finatra.thrift.tests.doeverything.exceptions.TestChannelException;
import com.twitter.util.Await;
import com.twitter.util.Duration;
import com.twitter.util.Future;

public class DoEverythingJavaThriftServerFeatureTest extends Assert {

    private static final EmbeddedThriftServer SERVER =
        new EmbeddedThriftServer(
            new DoEverythingJavaThriftServer(),
            Collections.singletonMap("magicNum", "57"),
            Stage.DEVELOPMENT,
            true);
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
    @SuppressWarnings("unchecked")
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
        assertEquals("com.twitter.finatra.thrift.filters.LoggingMDCFilter"
                + ".andThen(com.twitter.finatra.thrift.filters.TraceIdMDCFilter)"
                + ".andThen(com.twitter.finatra.thrift.filters.ThriftMDCFilter)"
                + ".andThen(com.twitter.finatra.thrift.filters.AccessLoggingFilter)"
                + ".andThen(com.twitter.finatra.thrift.filters.StatsFilter)"
                + ".andThen(com.twitter.finatra.thrift.filters.ExceptionMappingFilter)",
            filters.asText());
        JsonNode methods = thriftNode.get("methods");

        assertTrue(methods.size() > 0);

        Map<String, Object> methodsAsMap = objectMapper.convertValue(methods, Map.class);

        for (Map.Entry<String, Object> entry : methodsAsMap.entrySet()) {
            Map<String, Object> data = (Map<String, Object>) entry.getValue();
            assertTrue(data.containsKey("service_name"));
            assertTrue(data.containsKey("class"));
        }
    }

    /** test uppercase endpoint */
    @Test
    public void testUppercase() throws Exception {
        assertEquals("HI", await(THRIFT_CLIENT.uppercase("Hi")));
    }

    /** test uppercase endpoint fails */
    @Test
    @SuppressWarnings("unchecked")
    public void testUppercaseFailure() throws Exception {
        try {
            await(THRIFT_CLIENT.uppercase("fail"));
            fail("Expected exception " + Exception.class + " never thrown");
        } catch (Exception e) {
            // expected
            assertEquals(TApplicationException.class.getName(), e.getClass().getName());
        }
    }

    /** test echo endpoint */
    @Test
    public void testEcho() throws Exception {
        assertEquals("hello", await(THRIFT_CLIENT.echo("hello")));
    }

    /** test echo endpoint */
    @Test
    public void testEchoTApplicationException() throws Exception {
      // echo method doesn't define throws ClientError Exception
      // we should receive TApplicationException
      try {
        await(THRIFT_CLIENT.echo("clientError"));
        fail("Expected exception " + TApplicationException.class + " never thrown");
      } catch (Exception e) {
          // expected
          assertEquals(TApplicationException.class.getName(), e.getClass().getName());
          assertTrue(e.getMessage().contains("client error"));
      }
    }

    /** test echo2 endpoint */
    @Test
    @SuppressWarnings("unchecked")
    public void testEcho2() {
      // should be caught by FinatraJavaThriftExceptionMapper
      try {
        await(THRIFT_CLIENT.echo2("clientError"));
        fail("Expected exception " + Exception.class + " never thrown");
      } catch (Exception e) {
        // expected
        assertEquals(TApplicationException.class.getName(), e.getClass().getName());
        assertTrue(e.getMessage().contains("client error"));
      }
    }

    /** RequestException mapping */
    @Test
    public void testRequestExceptionMapping() {
        try {
            await(THRIFT_CLIENT.echo2("requestException"));
            fail("Expected exception " + Exception.class + " never thrown");
        } catch (Exception e) {
            // expected
            assertEquals(TApplicationException.class.getName(), e.getClass().getName());
            assertTrue(e.getMessage().contains(RequestException.class.getName()));
        }
    }

    /** TimeoutException mapping */
    @Test
    public void testTimeoutExceptionMapping() {
        try {
            await(THRIFT_CLIENT.echo2("timeoutException"));
            fail("Expected exception " + Exception.class + " never thrown");
        } catch (Exception e) {
            // expected
            assertEquals(TApplicationException.class.getName(), e.getClass().getName());
            assertTrue(e.getMessage().contains("timeout exception"));
        }
    }

    /** BarExceptionMapper Mapping */
    @Test
    public void testBarExceptionMapping() throws Exception {
        // should be caught by BarExceptionMapper
        assertEquals("BarException caught", await(THRIFT_CLIENT.echo2("barException")));
    }

    /** FooExceptionMapper Mapping */
    @Test
    public void testFooExceptionMapping() throws Exception {
        // should be caught by FooExceptionMapper
        assertEquals("FooException caught", await(THRIFT_CLIENT.echo2("fooException")));
    }

    /** UnhandledSourcedException Mapping */
    @Test
    public void testUnhandledSourcedExceptionMapping() throws Exception {
        try {
            await(THRIFT_CLIENT.echo2("unhandledSourcedException"));
            fail("Expected exception " + Exception.class + " never thrown");
        } catch (Exception e) {
            // expected
            assertEquals(TApplicationException.class.getName(), e.getClass().getName());
            assertTrue(e.getMessage().contains(TestChannelException.class.getName()));
        }
    }

    /** UnhandledException Mapping */
    @Test
    public void testUnhandledExceptionMapping() throws Exception {
        try {
            await(THRIFT_CLIENT.echo2("unhandledException"));
            fail("Expected exception " + Exception.class + " never thrown");
        } catch (Exception e) {
            // expected
            assertEquals(TApplicationException.class.getName(), e.getClass().getName());
            assertTrue(e.getMessage().contains("unhandled exception"));
        }
    }

    /** UnhandledThrowable Mapping */
    @Test
    public void testUnhandledThrowableMapping() throws Exception {
        try {
            await(THRIFT_CLIENT.echo2("unhandledThrowable"));
            fail("Expected exception " + Exception.class + " never thrown");
        } catch (Exception e) {
            // expected
            assertEquals(TApplicationException.class.getName(), e.getClass().getName());
            assertTrue(e.getMessage().contains("unhandled throwable"));
        }
    }

    /** test magicNum endpoint */
    @Test
    public void testMagicNum() throws Exception {
        assertEquals("57", await(THRIFT_CLIENT.magicNum()));
    }

    /** test moreThanTwentyTwoArgs endpoint */
    @Test
    public void testMoreThanTwentyTwoArgs() throws Exception {
        assertEquals("handled", await(THRIFT_CLIENT.moreThanTwentyTwoArgs(
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
            await(THRIFT_CLIENT.ask(new Question("What is the meaning of life?")));
        assertEquals(
                "The answer to the question: `What is the meaning of life?` is 42.",
                answer.getText());
    }

    /** test ask endpoint */
    @Test
    public void testAskFail() throws Exception {
      final Answer answer =
          await(THRIFT_CLIENT.ask(new Question("fail")));
      assertEquals(
          "DoEverythingException caught",
          answer.getText());
    }

    private <T> T await(Future<T> future) throws Exception {
      return Await.result(future, Duration.fromSeconds(2));
    }
}
