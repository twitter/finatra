package com.twitter.finatra.thrift.tests.doeverything;

import com.twitter.app.Flag;
import com.twitter.app.Flaggable;
import com.twitter.finagle.ThriftMux;
import com.twitter.finagle.tracing.NullTracer$;
import com.twitter.finatra.thrift.AbstractThriftServer;
import com.twitter.finatra.thrift.filters.AccessLoggingFilter;
import com.twitter.finatra.thrift.filters.LoggingMDCFilter;
import com.twitter.finatra.thrift.filters.StatsFilter;
import com.twitter.finatra.thrift.filters.ThriftMDCFilter;
import com.twitter.finatra.thrift.filters.TraceIdMDCFilter;
import com.twitter.finatra.thrift.routing.ThriftRouter;
import com.twitter.util.NullMonitor$;

public class DoEverythingJavaThriftServer extends AbstractThriftServer {
    private final Flag<String> magicNumFlag = flag().create(
        "magicNum",
        "26",
        "Magic number",
        Flaggable.ofString());

    @Override
    public String name() {
        return "example-java-server";
    }

    @Override
    public ThriftMux.Server configureThriftServer(ThriftMux.Server server) {
        return server
            .withMonitor(NullMonitor$.MODULE$)
            .withTracer(NullTracer$.MODULE$);
    }

    @Override
    public void configureThrift(ThriftRouter router) {
        router
            .filter(LoggingMDCFilter.class)
            .filter(TraceIdMDCFilter.class)
            .filter(ThriftMDCFilter.class)
            .filter(AccessLoggingFilter.class)
            .filter(StatsFilter.class)
            .add(DoEverythingJavaThriftController.class);
    }

}
