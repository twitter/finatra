.. _files:

Working With Files
==================

Finatra provides basic file server support which is not meant for high traffic file serving. Do not use the file server
for production apps requiring a robust high performance file serving solution.

File Locations
--------------

By default, files are served from the root of the classpath. You can use the flag `-doc.root` to customize the classpath
root for finding files. To serve files from the local filesystem, set the flag `-local.doc.root` to the location of the
file serving directory.

.. important::

    It is an **error** to attempt to set both the `-doc.root` and the `-local.doc.root` flags. 

Either:

-  do nothing to load resources from the classpath root **or**
-  configure a classpath "namespace" by setting the `-doc.root` flag **or**
-  load files from the local filesystem directory location specified by the `-local.doc.root` flag.

.. tip::

    Note that setting Java System Property `-Denv=env` is **no longer required nor supported**. 

    Setting the `-local.doc.root` flag will trigger the same `localFileMode` behavior from Finatra v1.x.

Additionally, it is recommend to use local filesystem serving *only during testing* and **not in production**. It is recommended that you include files to be served as classpath resources in production.

For changes from Finatra v1.x static files behavior see the `Static Files <../v1-migration/index.html#static-files>`__ section in the `Version 1 Migration Guide <../v1-migration/index.html>`__.

To set a flag value, pass the flag and its value as an argument to your server:

.. code:: bash

    $ java -jar finatra-http-server-assembly-2.0.0.jar -doc.root=/namespace

For more information on using and setting command-line flags see `Flags <../getting-started/flags.html#passing-flag-values-as-command-line-arguments>`__.

File Serving Examples
---------------------

.. code:: scala

    get("/file") { request: Request =>
      response.ok.file("/file123.txt")
    }

    get("/:*") { request: Request =>
      response.ok.fileOrIndex(
        request.params("*"),
        "index.html")
    }

See the `test class <https://github.com/twitter/finatra/blob/develop/http/src/test/scala/com/twitter/finatra/http/tests/integration/doeverything/test/DoEverythingServerFeatureTest.scala>`__ for more examples.
