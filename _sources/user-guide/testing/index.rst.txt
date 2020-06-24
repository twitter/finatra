.. _testing:

.. figure:: https://imgs.xkcd.com/comics/exploits_of_a_mom.png
   :alt: Source: https://xkcd.com

   Source: https://xkcd.com

Testing Features
================

Finatra provides the following testing features:

-  the ability to start a locally running server, issue requests, and assert responses.
-  the ability to easily replace class instances throughout the object graph.
-  the ability to retrieve instances in the object graph to perform assertions on them.
-  the ability to write powerful tests without deploying test code to production.

Types of Tests
--------------

What are we talking about when we talk about *testing*? At a high-level the philosophy of testing
in Finatra revolves around the following definitions:

- `Feature Tests <feature_tests.html>`__ - the most powerful tests enabled by Finatra. These tests
  allow for verification of the feature requirements of the service by exercising its external
  interface. Finatra supports both `black-box testing <https://en.wikipedia.org/wiki/Black-box_testing>`__
  and `white-box testing <https://en.wikipedia.org/wiki/White-box_testing>`__ against a locally
  running version of a server. Classes can be selectively swapped out with dummy implementations or
  mocks inserted, and internal state asserted. See an example feature test `here <https://github.com/twitter/finatra/blob/a79f56347acd2194e415975ab125e2509c8e91e5/examples/http-server/src/test/scala/com/twitter/hello/HelloWorldFeatureTest.scala>`__.

  .. note::
    It is worth noting that versions of these `Feature Tests <feature_tests.html>`__ could be re-used
    for regression testing as part of larger `system tests <https://en.wikipedia.org/wiki/System_testing>`__
    which could be run post-deploy for deploy verification and certification.
- `Integration Tests <integration_tests>`__ - similar to `Feature Tests <feature_tests.html>`__, but
  the entire service is not started. Instead, a list of `modules <../getting-started/modules.html>`__ are loaded with method calls and
  assertions are performed at the class-level. You can see an example integration test `here <https://github.com/twitter/finatra/blob/develop/http/src/test/scala/com/twitter/finatra/http/tests/marshalling/CallbackConverterIntegrationTest.scala>`__.
- `Unit Tests <https://en.wikipedia.org/wiki/Unit_testing>`__ - these are tests generally of a single
  class and since constructor injection is used throughout the framework, Finatra stays out of your
  way.

`ScalaTest <https://www.scalatest.org/>`__
------------------------------------------

The Finatra testing framework uses the Twitter recommended ScalaTest `testing style <https://www.scalatest.org/user_guide/selecting_a_style>`__ `FunSuite <https://doc.scalatest.org/3.0.0/#org.scalatest.FunSuite>`__ for framework testing and to 
facilitate the types of testing outlined above we have several testing traits to aid in creating simple 
and powerful tests.

For more information on `ScalaTest <https://www.scalatest.org/>`__, see the `ScalaTest User Guide <https://www.scalatest.org/user_guide>`__.

To make use of another ScalaTest testing style, such as `FunSpec <https://doc.scalatest.org/3.0.0/#org.scalatest.FunSpec>`__ 
or others, see `Test Mixins <mixins.html>`__.

Resources on Writing Tests
--------------------------
- `The Tao of Testing: Chapter 4 - How to Write Tests <https://jasonpolites.github.io/tao-of-testing/ch4-1.1.html>`__

`Test Dependencies <../..#test-dependencies>`_
----------------------------------------------

Please be sure to read `the section <../..#test-dependencies>`_ on including test-jar dependencies in your project.

More Information
----------------

- :doc:`embedded`
- :doc:`feature_tests`
- :doc:`integration_tests`
- :doc:`startup_tests`
- :doc:`mixins`
- :doc:`mocks`
- :doc:`override_modules`
- :doc:`bind_dsl`

