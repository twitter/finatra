.. _lifecycle:

Application and Server Lifecycle
================================

Finatra manages the lifecycle of your `TwitterUtil <https://twitter.github.io/util/>`__ `c.t.app.App <https://twitter.github.io/util/docs/com/twitter/app/App.html>`__ or `TwitterServer <https://twitter.github.io/twitter-server/>`__ `c.t.server.TwitterServer` for you and provides methods which can be implemented for running or starting core logic. This is to ensure that flag parsing and module installation to build the object graph happens in the correct order for the application or server.

Thus you do not have access to the `main` method. Instead, any logic should be contained in either the `c.t.inject.app.App#run` or `c.t.inject.server.TwitterServer#start` method, respectively.

Startup
-------

At a high-level, the start-up lifecycle of a Finatra server looks like:

.. image:: ../_static/FinatraLifecycle.png

Shutdown
--------

Upon *graceful* shutdown, all registered `onExit {...}` blocks are executed. See `c.t.util.App#exits <https://github.com/twitter/util/blob/develop/util-app/src/main/scala/com/twitter/app/App.scala#L69>`__).

This includes closing the external interface(s), the admin interface, and firing the `c.t.inject.TwitterModuleLifecycle#singletonShutdown <https://github.com/twitter/finatra/blob/develop/inject/inject-core/src/main/scala/com/twitter/inject/TwitterModuleLifecycle.scala#L25>`__
function on all installed modules.