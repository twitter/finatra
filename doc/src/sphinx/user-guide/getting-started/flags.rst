.. _flags:

Flags
=====

Finatra builds on the support of `TwitterUtil <https://github.com/twitter/util>`__ `Flags <https://github.com/twitter/util/blob/develop/util-app/src/main/scala/com/twitter/app/Flag.scala>`__ from `TwitterServer <https://twitter.github.io/twitter-server/Features.html#flags>`__, adding the ability to inject flag values into your classes.

Flags by their definition represent some external configuration that is passed to the server and are thus an excellent way to parameterize configuration that may be environment specific, e.g., a database host or URL that is different per environment: *production*, *staging*, *development*, or *jills-staging*. This allows you to work with the same application code in different environments.

This type of configuration parameterization is generally preferred over hardcoding logic by a type of "*environment*\ " string within code, e.g.

.. code:: bash

	if (env == "production") { ... }

It is generally good practice to make flags *granular* controls that are  fully orthogonal to one another. They can then be independently managed for each deploy and this scales consistently as the number of supported  "environments" scales.

But I have a lot of Flags
-------------------------

If you find that you end up with a lot of flags to configure for your service and your deployment system provides no relief -- there are still alternatives to moving external configuration into code. 

See `this thread <https://groups.google.com/forum/#!searchin/finatra-users/typesafe$20config%7Csort:relevance/finatra-users/kkZgI5dG9CY/lzDPAmUxAwAJ>`__ on using `Typesafe Config <https://github.com/typesafehub/config>`__ with Finatra and this `example repository <https://github.com/dkowis/finatra-typesafe-config>`__. 


How To Define Flags
-------------------

Flags can be `defined <https://github.com/twitter/finatra/blob/develop/http/src/test/scala/com/twitter/finatra/http/tests/integration/doeverything/main/modules/DoEverythingModule.scala#L13>`__ within a `Module <modules.html>`__ to allow for scoping of reusable external configuration (since Modules are meant to be re-usable). Though, you can also choose to define a flag `directly in a server <https://github.com/twitter/finatra/blob/develop/http/src/test/scala/com/twitter/finatra/http/tests/integration/doeverything/main/DoEverythingServer.scala#L22>`__.

When defined within a `Module <modules.html>`__, flags can be used to aid in the construction of an instance to be `provided to the object graph <modules.html#provides>`__, e.g., a DatabaseConnection instance with the database URL specified by a flag. The module is then able to tell the injector how to provide an instance of this type when necessary by defining an ``@Provides`` annotated method.

In Finatra, we also provide a way to override bound instances in the object graph when testing through `Override Modules <../testing/index.html#override-modules>`__ or with `@Bind <../testing/index.html#bind>`__.

``@Flag`` annotation
--------------------

``@Flag`` is a `binding annotation <../getting-started/binding_annotations.html>`__. This annotation allows flag values to be injected into classes (and provider methods).

Flag Definition
^^^^^^^^^^^^^^^

Define a flag (in this case within a `TwitterModule`)

.. code:: scala

    class MyModule extends TwitterModule {
      flag(name = "key", default = "default", help = "The key to use")

      @Provides
      @Singleton
      def providesFoo(
        @Flag("key") key: String) = {
        new Foo(key)
      }
    }

In the example above, notice that we do not save a local reference to the created `Flag` and instead reference it's value in the provider method by use of the ``@Flag`` binding annotation.

However, when defining a flag you can also simply dereference the flag directly within the module or server (in lieu of using the ``@Flag`` annotation). Just keep a local reference
to the created, then you can use the `Flag#apply <https://github.com/twitter/util/blob/develop/util-app/src/main/scala/com/twitter/app/Flag.scala#L171>`__, `Flag#get <https://github.com/twitter/util/blob/develop/util-app/src/main/scala/com/twitter/app/Flag.scala#L205>`__ or other methods, depending e.g.:

.. code:: scala

    object MyModule1 extends TwitterModule {
      val key = flag(name = "key", default = "default", help = "The key to use")

      @Singleton
      @Provides
      def providesThirdPartyFoo: ThirdPartyFoo = {
        new ThirdPartyFoo(key())
      }
    }

Flag Value Injection
^^^^^^^^^^^^^^^^^^^^

Flags specified with defaults can be injected as a constructor-arg to a class. When the class is obtained from the injector the correctly parsed flag value will be injected.

.. code:: scala

    class MyService @Inject()(
      @Flag("key") key: String) {
    }

Note, you can also always instantiate the above class manually. When doing so, you will need to pass all the constructor args manually including a value for the flag argument.

Flags Without Defaults
----------------------

`TwitterModule#flag` is parameterized to return a Flag of type `T` where `T` is the type of the argument passed as the default. If you do not specify a default value then you must explicitly parameterize your call to `TwitterModule#flag` with a defined type `T`, e.g,

.. code:: scala

    object MyModule1 extends TwitterModule {
      val key = flag[String](name = "key", help = "The key to use")

      @Singleton
      @Provides
      def providesThirdPartyFoo: ThirdPartyFoo = {
        val myKey = key.get match {
          case Some(value) => value
          case _ => "DEFAULT"
        }
        new ThirdPartyFoo(myKey)
      }
    }

Keep in mind that the specified `T` in this case must be a `Flaggable <https://github.com/twitter/util/blob/develop/util-app/src/main/scala/com/twitter/app/Flaggable.scala>`__ type.

Note that you should not call `Flag#apply <https://github.com/twitter/util/blob/develop/util-app/src/main/scala/com/twitter/app/Flag.scala#L171>`__ on a `Flag` without a default (as this will result in an Exception) but instead use `Flag#get <https://github.com/twitter/util/blob/develop/util-app/src/main/scala/com/twitter/app/Flag.scala#L205>`__ which returns an `Option[T]`.

Because Finatra does not currently support binding optional types, Flags without defaults *are not injectable* but can still be useful for accepting external configuration for either `providing instances to the object graph <modules.html#using-flags-in-modules>`__ or for a server.

This means if you try to inject a non-defaulted `Flag` instance using the ``@Flag`` binding annotation `you will get an IllegalArgumentException <https://github.com/twitter/finatra/blob/develop/inject/inject-app/src/main/scala/com/twitter/inject/app/internal/FlagsModule.scala#L34>`__.

Passing Flag Values as Command-Line Arguments
---------------------------------------------

Flags are set by passing them as arguments to your java application. E.g.,

.. code:: bash

    $ java -jar finatra-hello-world-assembly-2.0.0.jar -key=value

An example of this is passing the `-help` flag to see usage for running a Finatra server, e.g.

.. code:: bash

    $ java -jar finatra-hello-world-assembly-2.0.0.jar -help
    HelloWorldServer
      -alarm_durations='1.seconds,5.seconds': 2 alarm durations
      -help='false': Show this help
      -admin.port=':8080': Admin http server port
      -bind=':0': Network interface to use
      -log.level='INFO': Log level
      -log.output='/dev/stderr': Output file
      -key='default': The key to use


``failfastOnFlagsNotParsed``
----------------------------

Note that Finatra defaults the `failfastOnFlagsNotParsed` option as mentioned in the `TwitterServer documentation <https://twitter.github.io/twitter-server/Features.html#flags>`__ to `true <https://github.com/twitter/finatra/blob/develop/inject/inject-server/src/main/scala/com/twitter/inject/server/TwitterServer.scala#L61>`__ for you.


Modules Depending on Other Modules
----------------------------------

There may be times where you would like to reuse the flags defined in a given module inside another module. For instance, we have a module which defines a configuration flag that is useful in other contexts. 

As an example, let's assume we have a module which defines a flag for the service's `ClientId` String -- how it identifies itself as client to other services -- that is necessary for constructing different clients:

.. code:: scala

    object ClientIdModule extends TwitterModule {
      flag[String]("client.id", "System-wide client id for identifying this server as a client to other services.")
    }


You could choose to build and provide every client which needs this `ClientId` in the same module or you could decide to break up the client creation into separate modules (allowing them to be used and tested independently). If you do the latter, how do you get access to the set `ClientId` value from the `ClientIdModule` inside of another Module? 

Typically, you would inject the flag value using the ``@Flag`` `binding annotation <binding_annotations.html>`__ as a class constructor-arg. You can do the same for usage from a module -- however instead of the injection point being the constructor annotated with ``@Inject``, it is the argument list of any ``@Provides``-annotated method. 

E.g.,

.. code:: scala

    object ClientAModule extends TwitterModule {
      override val modules = Seq(ClientIdModule)

      @Singleton
      @Provides
      def provideClientA(
        @Flag("client.id") clientId): ClientA = {
        new ClientA(clientId)
      }
    }


What's happening here?

Firstly, we define a `ClientAModule` and override the `modules` val to be a `Seq` of modules that includes the `ClientIdModule`. This guarantees that if the `ClientIdModule` is not mixed into the list of modules for a server, the `ClientAModule` ensures it will be installed since it's declared as a dependency. 

This ensures that there will be a bound value for the `ClientId` flag. Otherwise, our module definition is brittle in that we are trying to make use of a flag which may never be defined within the scope of our server. With TwitterUtil flags, trying to use an undefined flag `could cause your server to fail to start <https://github.com/twitter/util/blob/develop/util-app/src/main/scala/com/twitter/app/Flags.scala#L118>`__. 

Thus we want to ensure that if we are only using flags we define in our module or we include the module that does. Note that it is an `error to try to define the same flag twice <https://github.com/twitter/util/blob/develop/util-app/src/main/scala/com/twitter/app/Flags.scala#L251>`__.

Finatra will de-dupe all modules before installing, so it's OK if a module appears twice in the server configuration, though you should strive to make this the exception.

Secondly, we then define a method which provides a `ClientA`. Since injection is by type (and the argument list to an ``@Provides`` annotated method in a module is an injection point) and ``String`` is not specific enough we use the ``@Flag`` `binding annotation <binding_annotations.html>`__.

We could continue this through another module. For example, if we wanted to provide a `ClientB` which needs both the `ClientId` and a `ClientA` we could define a `ClientBModule`:

.. code:: scala

    object ClientBModule extends TwitterModule {
      override val modules = Seq(
        ClientIdModule,
        ClientAModule)

      @Singleton
      @Provides
      def provideClientB(
        @Flag("client.id") clientId,
        clientA: ClientA): ClientB = {
        new ClientB(clientId, clientA)
      }
    }


Notice that we choose to list both the `ClientIdModule` and `ClientAModule` in the modules for the `ClientBModule`. Yet, since we know that the `ClientAModule` includes the `ClientIdModule` we could have choosen to leave it out. The "provides" method in the module above takes in both a `ClientId` String and a `ClientA`. 

Since it declares the two modules we're assured that these types will be available from the injector for our "provides" method to use.