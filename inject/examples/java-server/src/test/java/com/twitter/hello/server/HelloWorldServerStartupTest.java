package com.twitter.hello.server;

import org.junit.Test;

import com.twitter.inject.server.EmbeddedTwitterServer;

public class HelloWorldServerStartupTest {

    private EmbeddedTwitterServer server =
            new EmbeddedTwitterServer(new HelloWorldServer());

    @Test
    public void testServerStartup() {
        server.assertHealthy(true);
        server.close();
    }
}
