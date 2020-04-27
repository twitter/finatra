package com.twitter.finatra.example;

import java.util.Collections;

import com.google.inject.Stage;

import org.junit.Test;

import com.twitter.finatra.thrift.EmbeddedThriftServer;

public class CalculatorServerStartupTest {

    private EmbeddedThriftServer server =
        new EmbeddedThriftServer(
            new CalculatorServer(),
            Collections.emptyMap(),
            Stage.PRODUCTION);

    /** Test server startup */
    @Test
    public void testServerStartup() {
        server.assertHealthy(true);
        server.close();
    }
}
