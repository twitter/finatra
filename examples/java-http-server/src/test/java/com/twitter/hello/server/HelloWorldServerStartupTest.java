package com.twitter.hello.server;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Stage;

import org.junit.Test;

import com.twitter.finatra.http.EmbeddedHttpServer;

public class HelloWorldServerStartupTest {

    private EmbeddedHttpServer server =
        new EmbeddedHttpServer(
            new HelloWorldServer(),
            ImmutableMap.of(),
            Stage.PRODUCTION);

    @Test
    public void testServerStartup() {
        server.assertHealthy(true);
        server.close();
    }
}
