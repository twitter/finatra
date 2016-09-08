package com.twitter.hello.server;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Stage;

import org.junit.Test;

import com.twitter.inject.server.EmbeddedTwitterServer;

public class HelloWorldServerStartupTest {

    private EmbeddedTwitterServer server =
        new EmbeddedTwitterServer(
            new HelloWorldServer(),
            ImmutableMap.of(),
            Stage.PRODUCTION);

    @Test
    public void testServerStartup() {
        server.assertHealthy(true);
        server.close();
    }
}
