.. _lifecycle:

Server Lifecycle
================

Startup
-------

At a high-level, the start-up lifecycle of a Finatra server looks like:

.. image:: ../_static/FinatraLifecycle.png

Shutdown
--------

Upon *graceful* shutdown, all registered `onExit {...}` blocks are executed. See `c.t.util.App#exits <https://github.com/twitter/util/blob/develop/util-app/src/main/scala/com/twitter/app/App.scala#L69>`__).

This includes closing the external interface(s), the admin interface, and firing the `c.t.inject.TwitterModuleLifecycle#singletonShutdown <https://github.com/twitter/finatra/blob/develop/inject/inject-core/src/main/scala/com/twitter/inject/TwitterModuleLifecycle.scala#L25>`__
function on all installed modules.