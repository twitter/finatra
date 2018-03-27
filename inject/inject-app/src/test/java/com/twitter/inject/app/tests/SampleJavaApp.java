package com.twitter.inject.app.tests;

import java.util.Collection;
import java.util.Collections;

import com.google.inject.Module;

import com.twitter.inject.app.AbstractApp;
import com.twitter.inject.modules.LoggerModule$;

public class SampleJavaApp extends AbstractApp {

  private String sampleServiceResponse = "";

  public String getSampleServiceResponse() {
    return this.sampleServiceResponse;
  }

  @Override
  public Collection<Module> javaModules() {
    return Collections.singletonList(
        LoggerModule$.MODULE$);
  }

  @Override
  public void run() {
    sampleServiceResponse = injector().instance(SampleJavaAppManager.class).start();
  }
}
