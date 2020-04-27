package com.twitter.finatra.example;

import java.util.*;

import org.junit.Assert;
import org.junit.Test;

import com.twitter.app.FlagUsageError;
import com.twitter.inject.app.EmbeddedApp;

public class HelloWorldAppStartupTest extends Assert {
    // Note: it is important to not reuse a stateful application between
    // test cases as each test case runs the application again and reuse can
    // lead to non-deterministic tests.
    private EmbeddedApp app() {
        return new EmbeddedApp(new HelloWorldApp(new ArrayList<>()));
    }
    private EmbeddedApp app(HelloWorldApp underlying) {
        return new EmbeddedApp(underlying);
    }

    @Test
    public void testHelp() {
        Map<String, Object> flags = new HashMap<String, Object>();
        flags.put("help", "true");
        try {
            // help always terminates with a non-zero exit-code.
            app().main(flags);
            fail();
        } catch (Exception e) {
            // expected
            assertTrue(e instanceof FlagUsageError);
        }
    }

    @Test
    public void testAppStartup() {
        Map<String, Object> flags = new HashMap<String, Object>();
        flags.put("username", "Jane");

        HelloWorldApp underlying = new HelloWorldApp(new ArrayList<>());
        app(underlying).main(flags);

        List<Integer> queue = underlying.getQueue();
        List<Integer> expected = Arrays.asList(1, 2, 3, 4, 5, 6);
        assertEquals(expected, queue);
    }
}
