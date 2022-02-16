package com.twitter.inject.app;

import java.util.Collection;
import java.util.Collections;

import com.google.inject.Module;

import com.twitter.inject.modules.StatsReceiverModule;

public class SampleJavaApp extends AbstractApp {
  public SampleJavaApp() {
  }

  private String sampleServiceResponse = "";

  String getSampleServiceResponse() {
    return this.sampleServiceResponse;
  }

  @Override
  public Collection<Module> javaModules() {
    return Collections.singletonList(StatsReceiverModule.get());
  }

  @Override
  public void run() {
    sampleServiceResponse = injector().instance(SampleJavaAppManager.class).start();
  }
}
