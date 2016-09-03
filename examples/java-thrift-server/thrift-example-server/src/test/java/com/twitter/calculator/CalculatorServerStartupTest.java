package com.twitter.calculator;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Stage;

import org.junit.Test;

import com.twitter.finatra.thrift.EmbeddedThriftServer;

public class CalculatorServerStartupTest {

    private EmbeddedThriftServer server =
        new EmbeddedThriftServer(
            new CalculatorServer(),
            ImmutableMap.of(),
            Stage.PRODUCTION);

    /** Test server startup */
    @Test
    public void testServerStartup() {
        server.assertHealthy(true);
        server.close();
    }
}
