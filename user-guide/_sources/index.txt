User's Guide
============

|Build Status| |Test Coverage| |Project status| |Maven Central| |Gitter|

Finatra builds on `TwitterServer <https://twitter.github.io/twitter-server/>`__ and uses `Finagle <https://twitter.github.io/finagle/guide/>`__, therefore it is *highly recommended* that you familiarize yourself with those frameworks before getting started.

The version of Finatra documented here is **version 2.x**. Version 2.x is a **complete rewrite** over v1.x and as such many things are different.

For high-level information about the changes from v1.x see the blog post `here <https://blog.twitter.com/2015/finatra-20-the-fast-testable-scala-services-framework-that-powers-twitter>`__.

Finatra at its core is agnostic to the type of service or application being created. It can be used to build anything based on `TwitterUtil <https://github.com/twitter/util>`__: `c.t.app.App <https://github.com/twitter/util/blob/develop/util-app/src/main/scala/com/twitter/app/App.scala>`__. 

For servers, Finatra builds on top of the `features <https://twitter.github.io/twitter-server/Features.html>`__ of `TwitterServer <https://twitter.github.io/twitter-server/>`__ (and `Finagle <https://twitter.github.io/finagle>`__) by allowing you to easily define a `Server <https://twitter.github.io/finagle/guide/Servers.html>`__ and controllers (a `Service <https://twitter.github.io/finagle/guide/ServicesAndFilters.html#services>`__-like abstraction) which define and handle endpoints of the Server. You can also compose `Filters <https://twitter.github.io/finagle/guide/ServicesAndFilters.html#filters>`__ either per controller, per route in a controller, or across all controllers.


Getting Started
---------------

- :doc:`getting-started/basics`
- :doc:`getting-started/framework`
- :doc:`getting-started/lifecycle`
- :doc:`getting-started/modules`
- :doc:`getting-started/binding_annotations`
- :doc:`getting-started/flags`
- :doc:`getting-started/futures`
- :doc:`getting-started/examples`

Logging
-------

- :doc:`logging/index`
- :doc:`logging/logback`

HTTP
----

- :doc:`http/server`
- :doc:`http/controllers`
- :doc:`http/requests`
- :doc:`http/responses`
- :doc:`http/filters`
- :doc:`http/exceptions`
- :doc:`http/warmup`

JSON
----

- :doc:`json/index`
- :doc:`json/routing`
- :doc:`json/validations`

Files
-----

- :doc:`files/index`

Thrift
------

- :doc:`thrift/basics`
- :doc:`thrift/server`
- :doc:`thrift/controllers`
- :doc:`thrift/filters`
- :doc:`thrift/exceptions`
- :doc:`thrift/warmup`

Twitter Server
--------------

- :doc:`twitter-server/index`

Testing
-------

- :doc:`testing/index`

V1 Migration FAQ
----------------

- :doc:`v1-migration/index`

Contributing
------------

Finatra is an open source project that welcomes contributions from the greater community. We’re thankful for the many people who have already contributed and if you’re interested, please read the `contributing <https://github.com/twitter/finatra/blob/develop/CONTRIBUTING.md>`__ guidelines.

For support feel free to follow and/or tweet at the `@finatra <https://twitter.com/finatra>`__ Twitter account, post questions to the `Gitter chat room <https://gitter.im/twitter/finatra>`__, or email the finatra-users Google group: `finatra-users@googlegroups.com <mailto:finatra-users@googlegroups.com>`__.


.. Hidden ToC
.. toctree::
   :maxdepth: 2
   :hidden:

   getting-started/basics.rst
   getting-started/framework.rst
   getting-started/lifecycle.rst
   getting-started/modules.rst
   getting-started/binding_annotations.rst
   getting-started/flags.rst
   getting-started/futures.rst
   getting-started/examples.rst
   logging/index.rst
   logging/logback.rst
   http/server.rst
   http/controllers.rst
   http/requests.rst
   http/responses.rst
   http/filters.rst
   http/exceptions.rst
   http/warmup.rst
   json/index.rst
   json/routing.rst
   json/validations.rst
   files/index.rst
   thrift/basics.rst
   thrift/server.rst
   thrift/controllers.rst
   thrift/filters.rst
   thrift/exceptions.rst
   thrift/warmup.rst
   twitter-server/index.rst
   testing/index.rst
   v1-migration/index.rst

.. |Build Status| image:: https://secure.travis-ci.org/twitter/finatra.png?branch=develop 
   :target: http://travis-ci.org/twitter/finatra?branch=develop
.. |Test Coverage| image:: http://codecov.io/github/twitter/finatra/coverage.svg?branch=develop 
   :target: http://codecov.io/github/twitter/finatra?branch=develop
.. |Project status| image:: https://img.shields.io/badge/status-active-brightgreen.svg 
   :target: https://github.com/twitter/finatra#status
.. |Maven Central| image:: https://maven-badges.herokuapp.com/maven-central/com.twitter/finatra-http_2.12/badge.svg
.. |Gitter| image:: https://badges.gitter.im/Join%20Chat.svg 
   :target: https://gitter.im/twitter/finatra
   
