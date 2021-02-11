package com.twitter.finatra.http.tests.integration.doeverything.main;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import com.twitter.finagle.http.Request;
import com.twitter.finagle.http.Response;
import com.twitter.finatra.http.response.ResponseBuilder;
import com.twitter.util.Future;

public class HelloService {
    private final ResponseBuilder response;

    @Inject
    public HelloService(ResponseBuilder response) {
        this.response = response;
    }

    /** returns an http Response */
    Response hi(Request request) {
        return hi(request, true);
    }

    /** returns an http Response */
    Response hi(Request request, Boolean verifyHeaders) {
        if (verifyHeaders) {
            assert request.headerMap().getAll("test").mkString("").equals("156");
        }
        return response.ok("Hello " + request.getParam("name"));
    }

    /** returns a Goodbye response */
    GoodbyeResponse goodbye(Request request) {
        return new GoodbyeResponse("guest", "cya", 123);
    }

    /** returns a Future Map[String, Object] */
    Future<Map<String, Object>> computeQueryResult(Request request) {
        String query = request.getParam("q");
        Map<String, String> results = new HashMap<String, String>();
        results.put("a", "1");
        results.put("b", "2");
        results.put("c", "3");
        results.put("d", "4");
        results.put("e", "5");

        Map<String, Object> result = new HashMap<String, Object>();
        result.put("query", query);
        result.put("numResults", "5");
        result.put("results", results);
        result.put("user", "Bob");
        result.put("timestamp", "Thu, 19 May 2016 00:00:00 +00:00");
        return Future.value(result);
    }
}
