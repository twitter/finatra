package com.twitter.finatra.http.tests.integration.doeverything.main;

import javax.inject.Inject;

import scala.runtime.AbstractFunction0;
import scala.runtime.BoxedUnit;

import com.twitter.finagle.http.Method;
import com.twitter.finagle.http.Request;
import com.twitter.finagle.http.RouteIndex;
import com.twitter.finatra.http.AbstractController;
import com.twitter.util.Future;

/**
 * Test all HTTP methods
 */
public class DoEverythingJavaController extends AbstractController {
    private final HelloService helloService;

    @Inject
    public DoEverythingJavaController(HelloService helloService) {
        this.helloService = helloService;
    }

    /** Define routes */
    public void configureRoutes() {
        get("/hello", (Request request) -> helloService.hi(request.getParam("name")));

        get("/goodbye", (Request request) -> new GoodbyeResponse("guest", "cya", 123));

        get(
            "/bar/baz/foo",
            "foo_get",
            false,
            null,
            (Request request) -> new GoodbyeResponse("guest", "cya", 123));

        get("/admin/foo", true, (Request request) -> "admin route no index");

        get("/admin/finatra/implied", (Request request) -> "implied admin route no index");

        get(
            "/admin/clients",
            true,
            RouteIndex.apply(
                "Special",
                "Finatra",
                scala.Option.apply(null),
                Method.Get()),
            (Request request) -> "admin route with index");

        get("/query", request ->
            helloService.computeQueryResult(request.getParam("q")));

        post("/post",  (Request request) -> "post");

        post(
            "/admin/bar",
            true,
            RouteIndex.apply(
                "Special",
                "Finatra",
                scala.Option.apply(null),
                Method.Post()),
            (Request request) -> "post");

        put("/put",  (Request request) -> "put");

        delete("/delete",  (Request request) -> "delete");

        delete(
            "/admin/delete",
            true,
            RouteIndex.apply(
                    "Special",
                    "Finatra",
                    scala.Option.apply(null),
                    Method.Post()),
            (Request request) -> "delete");

        options("/options",  (Request request) -> "options");

        patch("/patch",  (Request request) -> "patch");

        head("/head",  (Request request) -> "head");

        any("/any",  (Request request) -> "any");

        get("/exception", (Request request) ->
            new DoEverythingJavaException("error processing request"));

        get("/futureException", (Request request) ->
            Future.exception(new DoEverythingJavaException("error processing request")));

        get("/futureThrowException", (Request request) ->
            Future.apply(
                new AbstractFunction0<BoxedUnit>() {
                    @Override
                    public BoxedUnit apply() {
                        throw new DoEverythingJavaException("error processing request");
                    }
                }));
    }
}
