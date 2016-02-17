package com.twitter.finatra.integration.main;

import javax.inject.Inject;

import com.twitter.finagle.http.Request;
import com.twitter.finatra.http.JavaCallback;
import com.twitter.finatra.http.JavaController;

/**
 * Test all HTTP methods
 */
public class DoEverythingJavaController extends JavaController {

    @Inject
    private HelloService helloService;

    /** Define routes */
    public void configureRoutes() {
        get("/hello", new JavaCallback() {
            public Object handle(Request request) {
                return helloService.hi(request.getParam("name"));
            }
        });

        get("/goodbye", new JavaCallback() {
            public Object handle(Request request) {
                return new GoodbyeResponse("guest", "cya", 123);
            }
        });

        post("/post", new JavaCallback() {
            public Object handle(Request request) {
                return "post";
            }
        });

        put("/put", new JavaCallback() {
            public Object handle(Request request) {
                return "put";
            }
        });

        delete("/delete", new JavaCallback() {
            public Object handle(Request request) {
                return "delete";
            }
        });

        options("/options", new JavaCallback() {
            public Object handle(Request request) {
                return "options";
            }
        });

        patch("/patch", new JavaCallback() {
            public Object handle(Request request) {
                return "patch";
            }
        });

        head("/head", new JavaCallback() {
            public Object handle(Request request) {
                return "head";
            }
        });
    }
}
