Finatra
===================================

Finatra is a sinatra clone backed by scala/finagle written by `@capotej`_ and `@twoism`_

Source code: http://github.com/capotej/finatra

.. _@capotej: http://twitter.com/capotej
.. _@twoism: http://twitter.com/twoism

**Latest version: 5.0.0**

Hello World Example:

.. code-block:: scala

   import com.posterous.finatra._

   class MyApp extends FinatraApp {
     get("/") { request =>
       response(body="hello world")
     }
   }

   def main(args: Array[String]) = {
     val myApp = new MyApp
     FinatraServer.register(myApp)
     FinatraServer.start
   }

The Finatra Manual
------------------

.. toctree::
   :maxdepth: 2

   manual/your-first-app
   manual/responses
   manual/params
   manual/templates-and-layouts
   manual/cookies
   manual/headers
   manual/uploads
   manual/testing
   manual/heroku
   manual/blog-example



.. [Mustache.java] https://github.com/spullara/mustache.java