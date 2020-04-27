package com.twitter.finatra.example;

import java.util.Collections;

import com.google.inject.Stage;

import org.junit.Test;

import com.twitter.finatra.http.EmbeddedHttpServer;

public class HelloWorldServerStartupTest {

    private EmbeddedHttpServer server =
        new EmbeddedHttpServer(
            new HelloWorldServer(),
            Collections.emptyMap(),
            Stage.PRODUCTION);

    @Test
    public void testServerStartup() {
        server.assertHealthy(true);
        server.close();
    }
}
