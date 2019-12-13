package com.twitter.inject.thrift.integration.doeverything;

import com.twitter.inject.Injector;
import com.twitter.inject.thrift.modules.JavaDarkTrafficFilterModule;

import com.twitter.util.Function;

public class DoEverythingJavaDarkTrafficFilterModule extends JavaDarkTrafficFilterModule {
  @Override
  public String label() {
    return "DoEverythingJavaThriftServer";
  }

  @Override
  public Function<byte[], Object> enableSampling(Injector injector) {
    return new Function<byte[], Object>() {
      @Override
      public Object apply(byte[] request) {
        return Boolean.TRUE;
      }
    };
  }
}
