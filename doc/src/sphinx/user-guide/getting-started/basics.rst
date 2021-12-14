.. _basics:

Finatra Basics
==============

At a high level, you can think of your Finatra app or server as a container. This container has a `lifecycle <../getting-started/lifecycle.html>`__ and some "state". This state includes `Flags <../getting-started/flags.html>`__ and `Modules <../getting-started/modules.html>`__ which help to create an `Injector <https://github.com/twitter/finatra/blob/develop/inject/inject-core/src/main/scala/com/twitter/inject/Injector.scala>`__ that provides access to an `object graph <../getting-started/dependency_injection.html#why-use-the-object-graph>`__.

The `lifecycle <../getting-started/lifecycle.html>`__ defines an order of operations for container startup, running, and shutdown that allows for you to hook into phases as necessary to ensure the proper initialization or teardown of your container.

.. image:: ../../_static/basics.png

`Flags <../getting-started/flags.html>`__ are constructs that allow your container to accept and parse command-line input in a type-safe manner which can be used to help configure container resources (like database connections, or network clients, etc). `Flags <../getting-started/flags.html>`__ are parsed at a `specific point in the container lifecycle <../getting-started/flags.html#when-are-flags-parsed>`__, so it is important to take care in how/when they are accessed with the recommended way being to always access parsed values `via injection <../getting-started/flags.html#id3>`__.

`Modules <../getting-started/modules.html>`__ are used to help the `Injector <https://github.com/twitter/finatra/blob/develop/inject/inject-core/src/main/scala/com/twitter/inject/Injector.scala>`__ create instances (typically of code that `is not your own <../getting-started/modules.html#defining-modules>`__) to keep in the `object graph <../getting-started/dependency_injection.html#why-use-the-object-graph>`__. 

.. note::

	It is not always necessary to create a `Module <../getting-started/modules.html>`__ since the `Injector <https://github.com/twitter/finatra/blob/develop/inject/inject-core/src/main/scala/com/twitter/inject/Injector.scala>`__ will recursively try to satisfy `@Inject`-annotated constructors for a requested type from the object graph, instantiating each constructor argument, all the way back to something with a no-arg constructor. 

	`Modules <../getting-started/modules.html>`__ are only necessary to help the `Injector <https://github.com/twitter/finatra/blob/develop/inject/inject-core/src/main/scala/com/twitter/inject/Injector.scala>`__ instantiate types that it cannot otherwise instantiate on its own. With the most common case being needing to instantiate instances of code that require external configuration via `Flags <../getting-started/flags.html>`__.


Your container's list of `modules <../getting-started/modules.html>`__ should be `defined <../getting-started/modules.html#module-configuration-in-servers>`__ statically such that they can be used to create the `Injector <https://github.com/twitter/finatra/blob/develop/inject/inject-core/src/main/scala/com/twitter/inject/Injector.scala>`__ at the appropriate point in the container `lifecycle <../getting-started/lifecycle.html>`__. `Dependency injection <../getting-started/dependency_injection.html>`__ provides best-practice mechanisms (like using `override modules <../testing/override_modules.html>`__ or the `#bind[T] DSL <../testing/bind_dsl.html>`__ in testing) which can be used to swap out implementations of types in the object graph such that you **should not have a situation where you need to define a different list of modules in the production code based on a conditional**. 

It is an anti-pattern if your are chaging the list of modules based on some conditional -- *especially, if the conditional is defined by external configuration*.

Lastly, your logic can then request instances of types from the `Injector <https://github.com/twitter/finatra/blob/develop/inject/inject-core/src/main/scala/com/twitter/inject/Injector.scala>`__ or via `constructor injection <../dependency_injection.html#use-constructor-injection>`__. The goal of `using dependency injection <../dependency_injection.html#dependency-injection-best-practices>`__ is to make it easier to `write robust tests <./index.html#testing>`__ for your container.

More information and details are provided in the following sections.