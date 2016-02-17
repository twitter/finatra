package com.twitter.finatra.http;

import com.twitter.finagle.http.Request;

public interface JavaCallback {

    /**
     * Handle the HTTP Request
     * @param request
     * @return
     */
    Object handle(Request request);
}
