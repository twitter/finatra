package com.twitter.finatra.http;

public interface JavaCallback<RequestType, ResponseType> {

  /** Functional callback interface for Java HTTP routes */
  ResponseType apply(RequestType request);
}
