package com.twitter.inject.thrift.integration.doeverything;

import com.twitter.finagle.thrift.ClientId;
import com.twitter.inject.utils.Handler;
import com.twitter.util.logging.Logger;

public class DoEverythingJavaThriftWarmupHandler extends Handler {
  private static final Logger LOG = Logger.apply(DoEverythingJavaThriftWarmupHandler.class);
  private static final ClientId CLIENT_ID = ClientId.apply("client123");

  @Override
  public void handle() {
    try {
      LOG.info("Performing warm up with ClientId: " + CLIENT_ID.name() + "...");
      // NOTE: this is not necessarily easy to do just yet with generated Java.
    } catch (Throwable t) {
      LOG.error(t.getMessage(), t);
    }
    LOG.info("Warm up done.");
  }
}
