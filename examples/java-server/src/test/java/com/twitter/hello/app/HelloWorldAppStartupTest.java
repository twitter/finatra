package com.twitter.hello.app;

import org.junit.Test;

import com.twitter.inject.app.EmbeddedApp;

public class HelloWorldAppStartupTest {

    private EmbeddedApp app =
            new EmbeddedApp(new HelloWorldApp());

    @Test
    public void testAppStartup() {
        app.main();
    }
}
