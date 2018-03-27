package com.twitter.inject.app.tests;

import javax.inject.Inject;

import com.twitter.util.logging.Logger;

public class SampleJavaAppManager {
  private static final Logger LOG = Logger.apply(SampleJavaAppManager.class);

  private SampleJavaAppService service;

  @Inject
  public SampleJavaAppManager(
      SampleJavaAppService service
  ) {
    this.service = service;
  }

  String start() {
    LOG.info("SampleManager started");
    String response = service.sayHi("yo");
    LOG.info("Service said " + response);
    return response;
  }
}
