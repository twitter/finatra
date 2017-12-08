package com.twitter.finatra.thrift.tests;

import java.util.Collections;

import com.google.inject.Stage;

import org.junit.Test;

import com.twitter.finatra.thrift.EmbeddedThriftServer;
import com.twitter.finatra.thrift.tests.doeverything.DoEverythingJavaThriftServer;

public class DoEverythingJavaThriftServerStartupTest {

    private EmbeddedThriftServer server =
        new EmbeddedThriftServer(
            new DoEverythingJavaThriftServer(),
            Collections.emptyMap(),
            Stage.PRODUCTION);

    /** Test server startup */
    @Test
    public void testServerStartup() {
        server.assertHealthy(true);
        server.close();
    }
}
