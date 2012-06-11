Finatra
===================================

Finatra is a sinatra clone backed by scala/finagle written by @capotej and @twoism

Features

* The routing DSL you've come to know and love

* Asynchronous, uses Finagle-HTTP/Netty

* Multipart file upload/form handling

* Modular app support

* A testing helper

* Built in static file server (note: not designed for huge files(>100mb))

* Mustache template support through [Mustache.java]_


The finatra Manual

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



Indices and tables
==================

* :ref:`genindex`
* :ref:`modindex`
* :ref:`search`




.. [Mustache.java] https://github.com/spullara/mustache.java