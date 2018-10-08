package com.twitter.calculator;

import java.util.Collection;

import com.google.common.collect.ImmutableList;
import com.google.inject.Module;

import com.twitter.calculator.modules.ModeModule;
import com.twitter.finatra.thrift.AbstractThriftServer;
import com.twitter.finatra.thrift.filters.AccessLoggingFilter;
import com.twitter.finatra.thrift.filters.ClientIdWhitelistFilter;
import com.twitter.finatra.thrift.filters.LoggingMDCFilter;
import com.twitter.finatra.thrift.filters.StatsFilter;
import com.twitter.finatra.thrift.filters.ThriftMDCFilter;
import com.twitter.finatra.thrift.filters.TraceIdMDCFilter;
import com.twitter.finatra.thrift.modules.ClientIdWhitelistModule$;
import com.twitter.finatra.thrift.routing.ThriftRouter;

class CalculatorServer extends AbstractThriftServer {

    @Override
    public Collection<Module> javaModules() {
        return ImmutableList.<Module>of(
            ClientIdWhitelistModule$.MODULE$,
            new ModeModule());
    }

    @Override
    public void configureThrift(ThriftRouter router) {
        router
            .filter(LoggingMDCFilter.class)
            .filter(TraceIdMDCFilter.class)
            .filter(ThriftMDCFilter.class)
            .filter(AccessLoggingFilter.class)
            .filter(StatsFilter.class)
            .filter(ClientIdWhitelistFilter.class)
            .add(CalculatorController.class);
    }
}
