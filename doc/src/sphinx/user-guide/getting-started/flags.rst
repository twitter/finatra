.. _flags:

Flags
=====

Finatra builds on the support of `TwitterUtil <https://github.com/twitter/util>`__ `Flags <https://github.com/twitter/util/blob/1dd3e6228162c78498338b1c3aa11afe2f8cee22/util-app/src/main/scala/com/twitter/app/Flag.scala>`__
from `TwitterServer <https://twitter.github.io/twitter-server/Features.html#flags>`__, adding the
ability to inject Flag values into your classes.

Flags by their definition represent some external configuration that is passed to the server and are
thus an excellent way to parameterize configuration that may be environment specific, e.g., a
database host or URL that is different per environment: *production*, *staging*, *development*, or
*jills-staging*. This allows you to work with the same application code in different environments.

This type of configuration parameterization is generally preferred over hardcoding logic by a type
of "*environment*\ " string within code, e.g.

.. code:: bash

  if (env == "production") { ... }

It is generally good practice to make Flags *granular* controls that are  fully orthogonal to one
another. They can then be independently managed for each deploy and this scales consistently as the
number of supported  "environments" scales.

Global Flags
------------

`TwitterUtil <https://github.com/twitter/util>`__ `Flags <https://github.com/twitter/util/blob/1dd3e6228162c78498338b1c3aa11afe2f8cee22/util-app/src/main/scala/com/twitter/app/Flag.scala>`__
also has the concept of a "global" flag. That is, a flag that is "global" to the JVM process (as it is
generally defined as a Scala object). In the discussion of Flags with Finatra we **do not** mean
"global" flags unless it is explicitly stated.

See the `scaladoc <http://twitter.github.io/util/docs/com/twitter/app/GlobalFlag.html>`__ for
`c.t.app.GlobalFlag` for more information.

But I have a lot of Flags
-------------------------

If you find that you end up with a lot of Flags to configure for your service and your deployment
system provides no relief -- there are still alternatives to moving external configuration into code.

See `this thread <https://groups.google.com/forum/#!searchin/finatra-users/typesafe$20config%7Csort:relevance/finatra-users/kkZgI5dG9CY/lzDPAmUxAwAJ>`__
on using `Lightbend Config <https://github.com/lightbend/config>`__ with Finatra.

How To Define Flags
-------------------

Flags can be `defined <https://github.com/twitter/finatra/blob/ec8d584eb914f50f92314c740dc68fb7abb47eff/http/src/test/scala/com/twitter/finatra/http/tests/integration/doeverything/main/modules/DoEverythingModule.scala#L13>`__
within a `Module <modules.html>`__ to allow for scoping of reusable external configuration (since
Modules are meant to be re-usable). Though, you can also choose to define a Flag
`directly in a server <https://github.com/twitter/finatra/blob/ec8d584eb914f50f92314c740dc68fb7abb47eff/http/src/test/scala/com/twitter/finatra/http/tests/integration/doeverything/main/DoEverythingServer.scala#L22>`__.

When defined within a `Module <modules.html>`__, Flags can be used to aid in the construction of an
instance to be `provided to the object graph <modules.html#provides>`__, e.g., a `DatabaseConnection`
instance with the database URL specified by a Flag. The module is then able to tell the injector how
to provide an instance of this type when necessary by defining an ``@Provides`` annotated method.

In Finatra, we also provide a way to override bound instances in the object graph when testing
through `Override Modules <../testing/index.html#override-modules>`__ or by using
`Embedded Server #bind[T] <../testing/index.html##embedded-server-bind-t>`__.

``@Flag`` annotation
--------------------

``@Flag`` is a `binding annotation <../getting-started/binding_annotations.html>`__. This annotation
allows Flag values to be injected into classes (and provider methods).

.. important::
   While Flags support parsing into any |Flaggable[T]|_ type, it is currently only possible to
   `bind to String types <https://github.com/twitter/finatra/blob/31efc1d46dea436fb580f4b71f9196d15bade2e3/inject/inject-app/src/main/scala/com/twitter/inject/app/internal/TwitterTypeConvertersModule.scala>`__
   or easily convertible from String types. Finatra provides `type conversions for <https://github.com/twitter/finatra/blob/31efc1d46dea436fb580f4b71f9196d15bade2e3/inject/inject-app/src/main/scala/com/twitter/inject/app/internal/TwitterTypeConvertersModule.scala>`__
   `c.t.util.Duration` and  `org.joda.time.Duration` as well as default conversions provided by
   `Guice <https://github.com/google/guice>`__ for
   `Numbers, Booleans, and Chars <https://github.com/google/guice/blob/55bb902701f6e0277fbfaedd735f4315213957bf/core/src/com/google/inject/internal/TypeConverterBindingProcessor.java#L43>`__.

   The reason for this limitation is that when creating the binding key for the Flag value we are
   not able to obtain enough type information to properly bind to any paramaterized type like
   `Seq[T]` or `Map[K, V]` as the |Flaggable[T]|_ trait does not currently carry enough type
   information to construct the correct binding key.

Flag Definition
^^^^^^^^^^^^^^^

Define a Flag (in this case within a `TwitterModule`)

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

In the example above, notice that we do not save a local reference to the created Flag and instead
reference its value in the provider method by use of the ``@Flag`` binding annotation.

However, when defining a Flag you can also simply dereference the Flag directly within the Module or
server (in lieu of using the ``@Flag`` annotation). Just keep a local reference to the created Flag
then use the `Flag#apply <https://github.com/twitter/util/blob/1dd3e6228162c78498338b1c3aa11afe2f8cee22/util-app/src/main/scala/com/twitter/app/Flag.scala#L171>`__,
`Flag#get <https://github.com/twitter/util/blob/1dd3e6228162c78498338b1c3aa11afe2f8cee22/util-app/src/main/scala/com/twitter/app/Flag.scala#L205>`__
or other methods, depending e.g.:

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

Flags specified with defaults can be injected as a constructor-arg to a class. When the class is
obtained from the injector the correctly parsed Flag value will be injected.

.. code:: scala

    class MyService @Inject()(
      @Flag("key") key: String) {
    }

Note, you can also always instantiate the above class manually. When doing so, you will need to pass
all the constructor args manually including a value for the Flag argument.

You can also ask the Injector directly for a Flag value using `Flags.named` (similar to Guice's
|Names.named|_):

.. code:: scala

    val key: String = injector.instance[String](Flags.named("key"))

.. caution:: Attempting to get a Flag value from the Injector for a Flag **without** a default value
    will result in an `ProvisionException`.

Flags Without Defaults
----------------------

When creating a Flag, the returned Flag is parameterized to the type of the supplied default
argument, e.g., the method signature looks like this:

.. code:: scala

    def apply[T: Flaggable](name: String, default: => T, help: String): Flag[T]


Thus if you do not specify a default value, you must explicitly parameterize calling
`TwitterModule#flag` with a defined type `T`, e.g,

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

Keep in mind that the specified `T` in this case must be a `Flaggable <https://github.com/twitter/util/blob/develop/util-app/src/main/scala/com/twitter/app/Flaggable.scala>`__
type. `Flag#get` will return a `None` when no value is passed on the command line for a Flag with no
default.

Note that you should not call `Flag#apply <https://github.com/twitter/util/blob/1dd3e6228162c78498338b1c3aa11afe2f8cee22/util-app/src/main/scala/com/twitter/app/Flag.scala#L171>`__
on a Flag without a default (as this will result in an `IllegalArgumentException`) but instead use
`Flag#get <https://github.com/twitter/util/blob/1dd3e6228162c78498338b1c3aa11afe2f8cee22/util-app/src/main/scala/com/twitter/app/Flag.scala#L205>`__
which returns an `Option[T]` on which you can then pattern match.

Because Finatra does not currently support binding optional types, Flags without defaults *are not
injectable* but can still be useful for accepting external configuration to either
`provide instances to the object graph <modules.html#using-flags-in-modules>`__ or configure a
server. That is, you can still use these Flags to help in providing other types to the object graph,
or  to configure logic in a server, their values just cannot be obtained from the Injector. You will
want to hold on to a local reference as seen above and use `Flag#get` to obtain a parsed value.

.. caution::
    Since Flags without default values are not supported for injection, this means if you try to inject
    a non-defaulted Flag instance using the ``@Flag`` binding annotation
    `you will get an IllegalArgumentException <https://github.com/twitter/finatra/blob/ec8d584eb914f50f92314c740dc68fb7abb47eff/inject/inject-app/src/main/scala/com/twitter/inject/app/internal/FlagsModule.scala#L34>`__
    on startup of your application or server.

Passing Flag Values as Command-Line Arguments
---------------------------------------------

Flags are set by passing them as arguments to your java application. E.g.,

.. code:: bash

    $ java -jar finatra-hello-world-assembly-2.0.0.jar -key=value

An example of this is passing the `-help` Flag to see usage for running a Finatra server, e.g.

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

Note that Finatra defaults the `failfastOnFlagsNotParsed` option as mentioned in the `TwitterServer
documentation <https://twitter.github.io/twitter-server/Features.html#flags>`__ to
`true <https://github.com/twitter/finatra/blob/ec8d584eb914f50f92314c740dc68fb7abb47eff/inject/inject-server/src/main/scala/com/twitter/inject/server/TwitterServer.scala#L61>`__
for you.


Modules Depending on Other Modules - Flags Edition
--------------------------------------------------

As we saw in the `Modules section <modules.html#modules-depending-on-other-modules>`__, Modules can
"depend" on other Modules. In that case we wanted an already bound type for use in another Module.

Flags are special since they are bound to the object graph by the framework due to the fact that
their values are parsed from the command line at a specific point in the server lifecycle.But the
principle is the same. What if we have a Module which defines a configuration Flag that is useful
in other contexts?

As an example, let's assume we have a Module which defines a Flag for the service's "Client Id"
String -- how it identifies itself as a client to other services -- that is necessary for
constructing different clients:

.. code:: scala

    object ClientIdModule extends TwitterModule {
      flag[String]("client.id", "System-wide client id for identifying this server as a client to other services.")
    }


You could choose to build and provide every client which needs the `client.id` Flag value in the
same Module, e.g.,

.. code:: scala

    object ClientsModule extends TwitterModule {
      val clientIdFlag = flag[String]("client.id", "System-wide client id for identifying this server as a client to other services.")

      @Singleton
      @Provides
      def providesClientA: ClientA = {
        new ClientA(clientIdFlag())
      }  

      @Singleton
      @Provides
      def providesClientB: ClientB = {
        new ClientB(clientIdFlag())
      }

      @Singleton
      @Provides
      def providesClientC: ClientC = {
        new ClientA(clientIdFlag())
      }

    }

Or you could choose to break up the client creation into separate Modules -- allowing them to be
used and tested independently. If you do the latter, how do you get access to the parsed `client.id`
Flag value from the `ClientIdModule` inside of another Module?

Most often you are trying to inject the Flag value into a class using the ``@Flag``
`binding annotation <binding_annotations.html>`__ on a class constructor-arg. E.g.,

.. code:: scala

    @Singleton
    class MyClassFoo @Inject() (
      @Flag("client.id") clientId) {
      ...
    }

You can do something similar in a Module. However, instead of the injection point being the
constructor annotated with ``@Inject``, it is the argument list of any ``@Provides``-annotated
method.

E.g.,

.. code:: scala

    object ClientAModule extends TwitterModule {
      override val modules = Seq(ClientIdModule)

      @Singleton
      @Provides
      def providesClientA(
        @Flag("client.id") clientId): ClientA = {
        new ClientA(clientId)
      }
    }


What's happening here?

Firstly, we've defined a `ClientAModule` and override the `modules` val to be a `Seq` of Modules
that includes the `ClientIdModule`. This guarantees that if the `ClientIdModule` is not mixed into
the list of Modules for a server, the `ClientAModule` ensures it will be installed since it's
declared as a dependency.

This ensures that there will be a bound value for the `ClientId` Flag. Otherwise, our Module
definition is brittle in that we are trying to make use of a Flag which may never be defined
within the scope of our server. With TwitterUtil Flags, trying to use an undefined Flag
`could cause your server to fail to start <https://github.com/twitter/util/blob/1dd3e6228162c78498338b1c3aa11afe2f8cee22/util-app/src/main/scala/com/twitter/app/Flags.scala#L118>`__.

Thus we want to ensure that:

a. we are only using Flags we define in our Module or
b. we include the Module that does.

Note that it is an `error to try to define the same Flag twice <https://github.com/twitter/util/blob/1dd3e6228162c78498338b1c3aa11afe2f8cee22/util-app/src/main/scala/com/twitter/app/Flags.scala#L251>`__.

Finatra will de-dupe all Modules before installing, so it is OK if a Module appears twice in the
server configuration, though you should strive to make this the exception.

Secondly, we've defined a method which provides a `ClientA`. Since injection is by type (and the
argument list to an ``@Provides`` annotated method in a Module is an injection point) and `String`
is not specific enough we use the ``@Flag`` `binding annotation <binding_annotations.html>`__.

We could continue this through another Module. For example, if we wanted to provide a `ClientB`
which needs both the `ClientId` and a `ClientA` we could define a `ClientBModule`:

.. code:: scala

    object ClientBModule extends TwitterModule {
      override val modules = Seq(
        ClientIdModule,
        ClientAModule)

      @Singleton
      @Provides
      def providesClientB(
        @Flag("client.id") clientId,
        clientA: ClientA): ClientB = {
        new ClientB(clientId, clientA)
      }
    }


Notice that we choose to list both the `ClientIdModule` and `ClientAModule` in the Modules for the
`ClientBModule`. Yet, since we know that the `ClientAModule` includes the `ClientIdModule` we could
have chosen to leave it out.

The `providesClientB` method in the Module above takes in both a `ClientId` String and a `ClientA`.
Since it declares the two Modules, we're assured that these types will be available from the
injector for our `providesClientB` method to use.

This is just an Example
-----------------------

Note that usage of a `client.id` Flag is just an example. In Finatra, we provide a
`ThriftClientIdModule <https://github.com/twitter/finatra/blob/develop/inject/inject-thrift-client/src/main/scala/com/twitter/inject/thrift/modules/ThriftClientIdModule.scala>`__
for binding a `c.t.finagle.thrift.ClientId` type so that you do not need to rely on the Flag value.

You'll see that this type is then expected to be bound in other Modules like the
`FilteredThriftClientModule <https://github.com/twitter/finatra/blob/ec8d584eb914f50f92314c740dc68fb7abb47eff/inject/inject-thrift-client/src/main/scala/com/twitter/inject/thrift/modules/FilteredThriftClientModule.scala#L234>`__
which is a utility for building filtered thrift clients.

The framework does not assume that you are using the
`ThriftClientIdModule <https://github.com/twitter/finatra/blob/develop/inject/inject-thrift-client/src/main/scala/com/twitter/inject/thrift/modules/ThriftClientIdModule.scala>`__
for providing the bound `ClientId` type thus the `FilteredThriftClientModule <https://github.com/twitter/finatra/blob/develop/inject/inject-thrift-client/src/main/scala/com/twitter/inject/thrift/modules/FilteredThriftClientModule.scala>`__
does **not** specify the `ThriftClientIdModule` in it's list of Modules to allow users to bind an
instance of the `ClientId` type in any manner they choose.


.. |Flaggable[T]| replace:: ``Flaggable[T]``
.. _Flaggable[T]: https://github.com/twitter/util/blob/1bdeab56e49015c1f4c097ef76e47b93a079a239/util-app/src/main/scala/com/twitter/app/Flaggable.scala#L19

.. |Names.named| replace:: `Names.named`
.. _Names.named: https://github.com/google/guice/blob/master/core/src/com/google/inject/name/Names.java
