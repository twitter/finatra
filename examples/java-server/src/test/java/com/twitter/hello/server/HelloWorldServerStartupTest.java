package com.twitter.hello.server;

import java.util.Collections;

import com.google.inject.Stage;

import org.junit.Test;

import com.twitter.inject.server.EmbeddedTwitterServer;

public class HelloWorldServerStartupTest {

    private EmbeddedTwitterServer server =
        new EmbeddedTwitterServer(
            new HelloWorldServer(),
            Collections.emptyMap(),
            Stage.PRODUCTION);

    @Test
    public void testServerStartup() {
        server.assertHealthy(true);
        server.close();
    }
}
