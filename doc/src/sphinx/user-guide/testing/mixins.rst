.. _mixins:

Test Mixins
===========

.. important::

  Please see the section on including test-jar dependencies in your project: `Test Dependencies <../..#test-dependencies>`_.

Twitter's recommended ScalaTest test style is `FunSuite <https://doc.scalatest.org/3.0.0/#org.scalatest.FunSuite>`__.

You can use this ScalaTest test style by extending either:

-  |c.t.inject.Test|_
-  |c.t.inject.IntegrationTest|_
-  |c.t.inject.server.FeatureTest|_

However, you are free to choose a ScalaTest testing style that suits your team by using the test mixin companion classes directly and mix in your preferred ScalaTest style:

-  |c.t.inject.TestMixin|_
-  |c.t.inject.IntegrationTestMixin|_
-  |c.t.inject.server.FeatureTestMixin|_

An example of using the |c.t.inject.server.FeatureTestMixin|_ with the `FunSpec` ScalaTest test style:

.. code:: scala

    import com.google.inject.Stage
    import com.twitter.finatra.http.EmbeddedHttpServer
    import com.twitter.inject.server.FeatureTestMixin
    import org.scalatest.FunSpec

    class SampleApiStartupTest
      extends FunSpec
      with FeatureTestMixin {

      override val server = new EmbeddedHttpServer(
        twitterServer = new SampleApiServer,
        stage = Stage.PRODUCTION,
        flags = Map(
          "foo.flag" -> "bar"
        )
      )

      describe("Sample Server") {
        it("should startup") {
          server.assertHealthy()
        }
      }
    }

More Information
----------------

- :doc:`index`
- :doc:`embedded`
- :doc:`feature_tests`
- :doc:`integration_tests`
- :doc:`startup_tests`
- :doc:`mocks`
- :doc:`override_modules`
- :doc:`bind_dsl`

.. |c.t.inject.Test| replace:: `c.t.inject.Test`
.. _c.t.inject.Test: https://github.com/twitter/finatra/blob/develop/inject/inject-core/src/test/scala/com/twitter/inject/Test.scala

.. |c.t.inject.IntegrationTest| replace:: `c.t.inject.IntegrationTest`
.. _c.t.inject.IntegrationTest: https://github.com/twitter/finatra/blob/develop/inject/inject-core/src/test/scala/com/twitter/inject/IntegrationTest.scala

.. |c.t.inject.server.FeatureTest| replace:: `c.t.inject.server.FeatureTest`
.. _c.t.inject.server.FeatureTest: https://github.com/twitter/finatra/blob/develop/inject/inject-server/src/test/scala/com/twitter/inject/server/FeatureTest.scala

.. |c.t.inject.TestMixin| replace:: `c.t.inject.TestMixin`
.. _c.t.inject.TestMixin: https://github.com/twitter/finatra/blob/develop/inject/inject-core/src/test/scala/com/twitter/inject/TestMixin.scala

.. |c.t.inject.IntegrationTestMixin| replace:: `c.t.inject.IntegrationTestMixin`
.. _c.t.inject.IntegrationTestMixin: https://github.com/twitter/finatra/blob/develop/inject/inject-core/src/test/scala/com/twitter/inject/IntegrationTestMixin.scala

.. |c.t.inject.server.FeatureTestMixin| replace:: `c.t.inject.server.FeatureTestMixin`
.. _c.t.inject.server.FeatureTestMixin: https://github.com/twitter/finatra/blob/develop/inject/inject-server/src/test/scala/com/twitter/inject/server/FeatureTestMixin.scala
