package com.twitter.finatra.thrift.tests.doeverything;

import com.twitter.finatra.thrift.modules.JavaDarkTrafficFilterModule;
import com.twitter.inject.Injector;
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
