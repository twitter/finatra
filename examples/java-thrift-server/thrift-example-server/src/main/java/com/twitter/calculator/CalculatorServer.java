package com.twitter.calculator;

import java.util.Collection;

import com.google.common.collect.ImmutableList;
import com.google.inject.Module;

import com.twitter.calculator.modules.ModeModule;
import com.twitter.finatra.thrift.AbstractThriftServer;
import com.twitter.finatra.thrift.filters.AccessLoggingFilter;
import com.twitter.finatra.thrift.filters.LoggingMDCFilter;
import com.twitter.finatra.thrift.filters.StatsFilter;
import com.twitter.finatra.thrift.filters.ThriftMDCFilter;
import com.twitter.finatra.thrift.filters.TraceIdMDCFilter;
import com.twitter.finatra.thrift.routing.JavaThriftRouter;

class CalculatorServer extends AbstractThriftServer {

    @Override
    public Collection<Module> javaModules() {
        return ImmutableList.<Module>of(new ModeModule());
    }

    @Override
    public void configureThrift(JavaThriftRouter router) {
        router
            .filter(LoggingMDCFilter.class)
            .filter(TraceIdMDCFilter.class)
            .filter(ThriftMDCFilter.class)
            .filter(AccessLoggingFilter.class)
            .filter(StatsFilter.class)
            .add(CalculatorController.class);
    }
}
