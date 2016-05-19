package com.twitter.finatra.integration.main;

import java.util.HashMap;
import java.util.Map;

import com.twitter.util.Future;

public class HelloService {
    public String hi(String name) {
        return "Hello " + name;
    }

    public Future<Map<String, Object>> computeQueryResult(String query) {
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
