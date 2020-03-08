package com.twitter.hello.app;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.twitter.inject.app.EmbeddedApp;

public class HelloWorldAppStartupTest extends Assert {
    private HelloWorldApp underlying = new HelloWorldApp(new ArrayList<>());

    private EmbeddedApp app = new EmbeddedApp(underlying);

    @Test
    public void testAppStartup() {
        app.main();

        List<Integer> queue = underlying.getQueue();
        List<Integer> expected = Arrays.asList(1, 2, 3, 4, 5, 6);
        assertEquals(expected, queue);
    }
}
