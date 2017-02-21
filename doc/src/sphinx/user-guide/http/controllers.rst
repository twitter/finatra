Defining HTTP Controllers
=========================

We now want to add the following controller to the `server definition <server.html>`__:

.. code:: scala

    import ExampleService
    import com.twitter.finagle.http.Request
    import com.twitter.finatra.http.Controller
    import javax.inject.Inject

    class ExampleController @Inject()(
      exampleService: ExampleService
    ) extends Controller {

      get("/ping") { request: Request =>
        "pong"
      }

      get("/name") { request: Request =>
        response.ok.body("Bob")
      }

      post("/foo") { request: Request =>
        exampleService.do(request)
        "bar"
      }
    }


The server can now be defined with the controller as follows:

.. code:: scala

    import DoEverythingModule
    import ExampleController
    import com.twitter.finatra.http.routing.HttpRouter
    import com.twitter.finatra.http.{Controller, HttpServer}

    object ExampleServerMain extends ExampleServer

    class ExampleServer extends HttpServer {

      override val modules = Seq(
        DoEverythingModule)

      override def configureHttp(router: HttpRouter): Unit = {
        router.
          add[ExampleController]
      }
    }


Here we are adding *by type* allowing the framework to handle class instantiation.

Controllers and Routing
-----------------------

Routes are defined in a `Sinatra <http://www.sinatrarb.com/>`__-style syntax which consists of an HTTP method, a URL matching pattern and an associated callback function. The callback function can accept either a `c.t.finagle.http.Request <https://github.com/twitter/finagle/blob/develop/finagle-http/src/main/scala/com/twitter/finagle/http/Request.scala>`__ or a custom case-class that declaratively represents the request you wish to accept. In addition, the callback can return any type that can be converted into a `c.t.finagle.http.Response <https://github.com/twitter/finagle/blob/develop/finagle-http/src/main/scala/com/twitter/finagle/http/Response.scala>`__.

When Finatra receives an HTTP request, it will scan all registered controllers **in the order they are added** and dispatch the request to the **first matching** route starting from the top of each controller then invoking the matching route's associated callback function.

That is, routes are matched in the order they are added to the `c.t.finatra.http.routing.HttpRouter <https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/routing/HttpRouter.scala>`__. Thus if you are creating routes with overlapping URIs it is recommended to list the routes in order starting with the "most specific" to the least specific.

In general, however, it is recommended to that you follow `REST <https://en.wikipedia.org/wiki/Representational_state_transfer>`__ conventions if possible, i.e., when deciding which routes to group into a particular controller, group routes related to a single resource into one controller.

Per-Route Stats
^^^^^^^^^^^^^^^

The per-route stating provided by Finatra in the `c.t.finatra.http.filters.StatsFilter <https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/filters/StatsFilter.scala>`__ works best when the above convention is followed.

.. code:: scala

    class GroupsController extends Controller {
      get("/groups/:id") { ... }

      post("/groups") { ... }

      delete("/groups/:id") { ... }
    }


yields the following stats:

::

    route/groups_id/GET/...
    route/groups/POST/...
    route/groups_id/DELETE/...


Alternatively, each route can be assigned a name which will then be used to create stat names.

.. code:: scala

    class GroupsController extends Controller {
      get("/groups/:id", name = "group_by_id") { ... }

      post("/groups", name = "create_group") { ... }

      delete("/groups/:id", name = "delete_group") { ... }
    }


yields:

::

    route/group_by_id/GET/...
    route/create_group/POST/...
    route/delete_group/DELETE/...


Route Matching Patterns:
------------------------

Named Parameters
^^^^^^^^^^^^^^^^

Route patterns may include named parameters. E.g., a defined variable in the route path:

.. code:: scala

    get("/users/:id") { request: Request =>
      "You looked up " + request.params("id")
    }

In the above example, `:id` is considered a "named parameter" of the route and will capture the value in its position in the incoming request URI.

As shown, the incoming value from the request can be obtained from the request parameters map, e.g. `request.params("id")`.

For example, both of the following requests will match the above defined route:

::

    GET /users/1234
    GET /users/5678

Which would produce responses like the following:

::

    ===========================================================================
    HTTP GET /users/1234
    [Header]	Host -> 127.0.0.1:57866
    ===========================================================================
    [Status]	Status(200)
    [Header]	Content-Type -> text/plain; charset=utf-8
    [Header]	Server -> Finatra
    [Header]	Date -> Tue, 31 Jan 2017 00:00:00 GMT
    [Header]	Content-Length -> 18
    You looked up 1234

    ===========================================================================
    HTTP GET /users/5678
    [Header]	Host -> 127.0.0.1:57866
    [Status]	Status(200)
    [Header]	Content-Type -> text/plain; charset=utf-8
    [Header]	Server -> Finatra
    [Header]	Date -> Tue, 31 Jan 2017 00:00:00 GMT
    [Header]	Content-Length -> 18
    You looked up 5678

As `request.params("id")` would capture `1234` in the first request and `5678` in the second.

**Note:** *Both query params and route params are stored in the parameters map of the request.* If a route parameter and a query parameter have the same name, the route parameter always wins.

Therefore, you should ensure your route parameter names do not collide with any query parameter names that you plan to read from the request.

Constant Routes
^^^^^^^^^^^^^^^

A "constant route" is any defined route which *does not* specify a `named parameter <#named-parameters>`__ in its route path. Routing is optimized to do a simple lookup against a "constant route" map whereas
`named parameter <#named-parameters>`__ routes are tried in their defined order for a route which will handle the request.

Wildcard Parameter
^^^^^^^^^^^^^^^^^^

Routes can also contain the wildcard pattern as a `named parameter <#named-parameters>`__, `:*`. The wildcard can only appear once at the end of a pattern and it will capture *all text in its place*.

For example,

.. code:: scala

    get("/files/:*") { request: Request =>
      request.params("*")
    }


Given a request:

::

    GET  /files/abc/123/foo.txt

would produce a response:

::

    ===========================================================================
    HTTP GET /files/abc/123/foo.txt
    [Header]	Host -> 127.0.0.1:58540
    ===========================================================================
    [Status]	Status(200)
    [Header]	Content-Type -> text/plain; charset=utf-8
    [Header]	Server -> Finatra
    [Header]	Date -> Tue, 31 Jan 2017 00:00:00 GMT
    [Header]	Content-Length -> 15
    abc/123/foo.txt

The wildcard named parameter matches everything in its position. In this case: `abc/123/foo.txt`.

Regular Expressions
^^^^^^^^^^^^^^^^^^^

Regular expressions are no longer allowed in string defined paths (since v2).

Route Prefixes
--------------

Finatra provides a simple DSL for adding a common prefix to a set of routes within a Controller. For instance, if you have a group of routes within a controller that should all have a common prefix
you can define them by making use of the `c.t.finatra.http.RouteDSL#prefix` function available in any subclass of `c.t.finatra.http.Controller`, e.g.,

.. code:: scala

    class MyController extends Controller {

      // regular route
      get("/foo") { request: Request =>
        "Hello, world!"
      }

      // set of prefixed routes
      prefix("/2") {
        get("/foo") { request: Request =>
          "Hello, world!"
        }

        post("/bar") { request: Request =>
          response.ok
        }
      }
    }

This definition would produce the following routes:

::

    GET     /foo
    GET     /2/foo
    POST    /2/bar

The input to the `c.t.finatra.http.RouteDSL#prefix` function is a String and how you determine the value of that String is entirely up to you. You could choose to hard code the value like in the
above example, or inject it as a parameter to the Controller, e.g., by using a `flag <../getting-started/flags.html>`__ or a `Binding Annotation <../getting-started/binding_annotations.html>`__ that
looks for a bound String type in the object graph which would allow you provide it in any manner appropriate for your use case.

For example,

.. code:: scala

    class MyController @Inject()(
      @Flag("api.version.prefix") apiVersionPrefix: String, // value from a "api.version.prefix" flag
      @VersionPrefix otherVersionPrefix otherApiVersionPrefix: String // value from a String bound with annotation: @VersionPrefix
    ) extends Controller {
      ...

      prefix(apiVersionPrefix) {
        get("/foo") { request: Request =>
          ...
        }
      }

      prefix(otherVersionPrefix) {
        get("/bar") { request: Request =>
          ...
        }
      }

Things to keep in mind:
^^^^^^^^^^^^^^^^^^^^^^^

-  Routes are always added to the `c.t.finatra.http.routing.HttpRouter <https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/routing/HttpRouter.scala>`__ **in the order defined** in the `Controller <../http/controllers.html#controllers-and-routing>`__ and are scanned in this order as well.
   This remains true even when defined within a `prefix` block. I.e., the `prefix` is merely a convenience for adding a common prefix to a set of routes. You should still be aware of the total order in which your routes are defined in a Controller.
-  You can use the `c.t.finatra.http.RouteDSL#prefix` function multiple times in a Controller with the same or different values.

Trailing Slashes
----------------

If you want to ignore trailing slashes on routes such that `/groups/1` and `groups/1/` are treated to be equivalent, append `/?` to your route URI, e.g.,

.. code:: scala

    get("/groups/:id/?") { request: Request =>
      response.ok(...)
    }


Admin Paths
-----------

All `TwitterServer <https://twitter.github.io/twitter-server/>`__-based servers have an `HTTP Admin Interface <https://twitter.github.io/twitter-server/Features.html#admin-http-interface>`__ which includes a variety of tools for diagnostics, profiling, and more. This admin interface **should not** be exposed outside your data center DMZ.

Any route path starting with `/admin/finatra/` will be included by default on the server's admin interface (accessible via the server's admin port). Other paths can be included on the server's admin interface by setting `admin = true` when defining the route.

These routes **MUST** be `constant routes`_, e.g., routes that do not define `named parameters <#named-parameters>`__.

.. code:: scala

    get("/admin/finatra/users/") { request: Request =>
      userDatabase.getAllUsers(
        request.params("cursor"))
    }

    get("/admin/display/", admin = true) { request: Request =>
      response.ok(...)
    }

    post("/special/route/", admin = true) { request: Request =>
      ...
    }

    // cannot be added to admin index as it uses a named parameter (:id) in the route path
    get("/admin/client/:id", admin = true) { request: Request =>
      response.ok(...)
    }

Some admin routes can additionally be listed in the `TwitterServer <https://twitter.github.io/twitter-server/>`__ `HTTP Admin Interface index <https://twitter.github.io/twitter-server/Admin.html>`__.

To expose your route in the `TwitterServer <https://twitter.github.io/twitter-server/>`__ `HTTP Admin Interface index <https://twitter.github.io/twitter-server/Admin.html>`__, the route path:

-  **MUST** be a `constant path <#constant-routes>`__.
-  **MUST** start with `/admin/`.
-  **MUST NOT** start with `/admin/finatra/`.
-  **MUST** be an HTTP method `GET` or `POST` route.

When defining the route in a Controller, in addition to setting `admin = true` you must also provide a `RouteIndex <https://github.com/twitter/finagle/blob/develop/finagle-http/src/main/scala/com/twitter/finagle/http/Route.scala>`__,
e.g.,

.. code:: scala

    get("/admin/client_id.json",
      admin = true,
      index = Some(
        RouteIndex(
          alias = "Thrift Client Id", 
          group = "Process Info"))) { request: Request =>
      Map("client_id" -> "clientId.1234"))
    }


The route will appear in the left-rail of the `TwitterServer <https://twitter.github.io/twitter-server/>`__ `HTTP Admin Interface <https://twitter.github.io/twitter-server/Admin.html>`__ under the heading specified by the `RouteIndex#group` indexed by `RouteIndex#alias` or the route's path.

If you do not provide a `RouteIndex` the route will not appear in the index but is still reachable on the admin interface.

Admin Path Routing
^^^^^^^^^^^^^^^^^^

**Note**: only admin routes which start with `/admin/finatra/` will be routed to using the server's configured `HttpRouter <https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/routing/HttpRouter.scala>`__. All other admin routes will be routed to by TwitterServer's `AdminHttpServer <https://github.com/twitter/twitter-server/blob/develop/src/main/scala/com/twitter/server/AdminHttpServer.scala#L108>`__ which only supports **exact path matching** and thus why only constant routes are allowed.

Therefore any configuration defined on your server's `HttpRouter <https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/routing/HttpRouter.scala>`__ will thus only apply to admin routes starting with `/admin/finatra`.
And because these routes will use the Finatra `RoutingService <https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/internal/routing/RoutingService.scala>`__ these routes cannot be included in the `TwitterServer <https://twitter.github.io/twitter-server/>`__ `HTTP Admin Interface <https://twitter.github.io/twitter-server/Admin.html>`__ index.