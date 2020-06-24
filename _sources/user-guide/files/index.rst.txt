.. _files:

Working With Files
==================

Finatra provides basic file server support which is not meant for high traffic file serving. Do not use the file server
for production apps requiring a robust high performance file serving solution.

File Locations
--------------

By default, files are served from the root of the classpath. That is, by default the framework attempts to load files
as `classpath resources <https://docs.oracle.com/javase/8/docs/technotes/guides/lang/resources.html>`_. As such, you 
may want to familiarize yourself with the basic workings of the Java `classpath <https://en.wikipedia.org/wiki/Classpath_(Java)>`_. 
You can use the flag `-doc.root` to customize the classpath root (i.e., "namespace") for finding files. 

To serve files from the local filesystem, set the flag `-local.doc.root` to the location of the file serving directory.

.. important::

    It is an **error** to attempt to set both the `-doc.root` and the `-local.doc.root` flags. 

Either:

-  do nothing to load resources from the classpath root **or**
-  configure a classpath "namespace" by setting the `-doc.root` flag **or**
-  load files from the local filesystem directory location specified by the `-local.doc.root` flag.

Additionally, it is recommend to use local filesystem serving *only during testing* and **not in production**. It is recommended that you include files to be served as `classpath resources <https://docs.oracle.com/javase/8/docs/technotes/guides/lang/resources.html>`_ in production.

To set a flag value, pass the flag and its value as an argument to your server:

.. code:: bash

    $ java -jar finatra-http-server-assembly-2.0.0.jar -doc.root=/namespace

For more information on using and setting command-line flags see `Flags <../getting-started/flags.html#passing-flag-values-as-command-line-arguments>`__.

|FileResolver|_
---------------

Files are loaded by the |FileResolver|_. 

In HTTP `servers <../http/server.html>`_, the `FileResolverModule <https://github.com/twitter/finatra/blob/develop/utils/src/main/scala/com/twitter/finatra/modules/FileResolverModule.scala>`_ provides an instance
of the |FileResolver|_ as a `framework module <../http/server.html#framework-modules>`_ (`source <https://github.com/twitter/finatra/blob/e9aa2dacd4ba2efd2db380135a10cf8ad10ee2b5/http/src/main/scala/com/twitter/finatra/http/servers.scala#L426>`_).

File Serving Examples
---------------------

File serving is most commonly done through an HTTP `server <../http/server.html>`_ to serve assets. Thus we'll show examples assuming an HTTP controller. For utility, the framework provides methods to return the contents of a file as the body of an HTTP response through the
`ResponseBuilder <../http/responses.html#responsebuilder>`_.

.. code:: scala

    import ExampleService
    import com.twitter.finagle.http.Request
    import com.twitter.finatra.http.Controller
    import javax.inject.Inject

    class ExampleController @Inject()(
      exampleService: ExampleService
    ) extends Controller {

      get("/file") { _: Request =>
        response.ok.file("/file123.txt")
      }

      get("/:*") { request: Request =>
        response.ok.fileOrIndex(
          request.params("*"),
          "index.html")
      }
    }


In the first route definiton, we use the `ResponseBuilder <../http/responses.html#responsebuilder>`_ of the HTTP Controller to return an HTTP `200 - OK` response with a body rendered from the contents of the file `file123.txt`. In this case, if the file cannot be found an HTTP `404 - NOT FOUND` response `will be returned <https://github.com/twitter/finatra/blob/e9aa2dacd4ba2efd2db380135a10cf8ad10ee2b5/http/src/main/scala/com/twitter/finatra/http/response/EnrichedResponse.scala#L638>`_. See the methods of the `c.t.finatra.http.response.EnrichedResponse <https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/response/EnrichedResponse.scala>`_ for more details on returning file responses.

In the second route definition, we use the `ResponseBuilder <../http/responses.html#responsebuilder>`_ to return an HTTP `200 - OK` response with a body rendered from either the filename represented by the incoming wildcard path parameter (`:*`), or if not found the contents of the `index.html` file. 

Specifically, this means that given an incoming URI of `/foo.html`, this route will attempt to load the `foo.html` file and if it 
could not would instead load `index.html` and render a response with a body of the resolved file contents. Likewise, an incoming URI of `/foo/bar/file.html` will attempt to resolve the file at that path, `foo/bar/file.html` otherwise it will return the contents of the 
`index.html` file.

.. important::

    Routes are matched in the order they are defined, thus this route SHOULD be LAST as it is a "catch-all" and routes should be 
    defined in order of most-specific to least-specific.

This can be useful for building "single-page" `web applications <https://en.wikipedia.org/wiki/Single-page_application>`_. See the 
`web-dashboard <https://github.com/twitter/finatra/tree/develop/examples/advanced/web-dashboard>`_ project for a runnable example.

Or see the local file system `test class <https://github.com/twitter/finatra/blob/develop/http/src/test/scala/com/twitter/finatra/http/tests/integration/fileserver/LocalFileServerFeatureTest.scala>`__ for more general file resolving examples.

.. |FileResolver| replace:: `c.t.finatra.utils.FileResolver`
.. _FileResolver: https://github.com/twitter/finatra/blob/e9aa2dacd4ba2efd2db380135a10cf8ad10ee2b5/utils/src/main/scala/com/twitter/finatra/utils/FileResolver.scala#L21

