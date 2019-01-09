.. Author notes: this file is formatted with restructured text
  (http://docutils.sourceforge.net/docs/user/rst/quickstart.html)
  as it is included in Finatra's documentation.

Note that ``RB_ID=#`` and ``PHAB_ID=#`` correspond to associated message in commits.

Unreleased
----------

Added
~~~~~

* finatra-jackson: Added @Pattern annotation to support to finatra/jackson for regex pattern
  validation on string values

19.1.0
-------

Added
~~~~~

* finatra-kafka-streams: SumAggregator and CompositeSumAggregator only support enhanced window
  aggregations for the sum operation. Deprecate SumAggregator and CompositeSumAggregator and create
  an AggregatorTransformer class that can perform arbitrary aggregations. ``PHAB_ID=D257138``

* finatra-streams: Open-source Finatra Streams. Finatra Streams is an integration
  between Kafka Streams and Finatra which we've been using internally at Twitter
  for the last year. The library is not currently open-source.
  ``PHAB_ID=D248408``

* inject-server: Add lint rule to alert when deprecated `util-logging` JUL flags from the
  `c.t.inject.server.DeprecatedLogging` trait are user defined. This trait was mixed-in
  only for backwards compatibility when TwitterServer was moved to the slf4j-api and the flags are
  not expected to be configured. By default, `util-app` based applications will fail to start if
  they are passed a flag value at startup which they do not define. Users should instead configure
  their chosen slf4j-api logging implementation directly. ``PHAB_ID=D256489``

* finatra-thrift: `c.t.finatra.thrift.Controllers` now support per-method filtering and
  access to headers via `c.t.scrooge.{Request, Response}` wrappers. To use this new
  functionality, create a `Controller` which extends the
  `c.t.finatra.thrift.Controller(SomeThriftService)` abstract class instead of constructing a
  Controller that mixes in the `SomeThriftService.BaseServiceIface` trait. With this, you can now
  provide implementations in form of `c.t.scrooge.Request`/`c.t.scrooge.Response` wrappers by calling
  the `handle(ThriftMethod)` method. Note that a `Controller` constructed this way cannot also
  extend a `BaseServiceIface`.

    handle(SomeMethod).filtered(someFilter).withFn { req: Request[SomeMethod.Args] =>
      val requestHeaders = req.headers
      // .. implementation here

      // response: Future[Response[SomeMethod.SuccessType]]
    }

  Note that if `Request`/`Response` based implementations are used the types on any
  existing `ExceptionMappers` should be adjusted accordingly. Also, if a `DarkTrafficFilterModule`
  was previously used, it must be swapped out for a `ReqRepDarkTrafficFilterModule`
  ``PHAB_ID=D236724``

Changed
~~~~~~~

* inject-core, inject-server: Remove deprecated `@Bind` support from test mixins. Users should
  instead prefer using the `bind[T] <https://twitter.github.io/finatra/user-guide/testing/bind_dsl.html>`__
  DSL in tests. ``PHAB_ID=D250325``

* inject-app: Remove deprecated `bind[T]` DSL methods from `c.t.inject.app.BindDSL`.

  Instead of:

  .. code:: scala

    injector.bind[T](instance)
    injector.bind[T, Ann](instance)
    injector.bind[T](ann, instance)

  Users should instead use the more expressive forms of these methods, e.g.,:

  .. code:: scala

    injector.bind[T].toInstance(instance)
    injector.bind[T].annotatedWith[Ann].toInstance(instance)
    injector.bind[T].annotatedWith(ann).toInstance(instance)

  which more closely mirrors the scala-guice binding DSL. ``PHAB_ID=D255591``

* finatra-thrift: For services that wish to support dark traffic over
  `c.t.scrooge.Request`/`c.t.scrooge.Response`-based services, a new dark traffic module is
  available: `c.t.finatra.thrift.modules.ReqRepDarkTrafficFilterModule` ``PHAB_ID=D236724``

* finatra-thrift: Creating a `c.t.finatra.thrift.Controller` that extends a
  `ThriftService.BaseServiceIface` has been deprecated. See the related bullet point in "Added" with
  the corresponding PHAB_ID to this one for how to migrate. ``PHAB_ID=D236724``

* inject-core, inject-server: Remove deprecated `WordSpec` testing utilities. The framework
  default ScalaTest testing style is `FunSuite` though users are free to mix their testing
  style of choice with the framework provided test mixins as per the
  `documentation <https://twitter.github.io/finatra/user-guide/testing/mixins.html>`__.
  ``PHAB_ID=D255094``

* finatra-thrift: Instead of failing (potentially silently)
  `c.t.finatra.thrift.routing.ThriftWarmup` now explicitly checks that it is
  using a properly configured `c.t.finatra.thrift.routing.Router` ``PHAB_ID=D253603``

* finatra-inject: `c.t.finatra.inject.server.PortUtils` has been modified to
  work with `c.t.f.ListeningServer` only. Methods which worked with the
  now-removed `c.t.f.b.Server` have been modified or removed.
  ``PHAB_ID=D254339``

* finatra-kafka-streams: Finatra Queryable State methods currently require the window size
  to be passed into query methods for windowed key value stores. This is unnecessary, as
  the queryable state class can be passed the window size at construction time. We also now
  save off all FinatraKeyValueStores in a global manager class to allow query services
  (e.g. thrift) to access the same KeyValueStore implementation that the FinatraTransformer
  is using. ``PHAB_ID=D256920``

Fixed
~~~~~

* finatra-kafka-streams: Fix bug where KeyValueStore#isOpen was throwing an
  exception when called on an uninitialized key value store
  ``PHAB_ID=D257635``

Closed
~~~~~~

18.12.0
-------

Added
~~~~~

Changed
~~~~~~~

* finatra-thrift: `c.t.finatra.thrift.Controller` is now an abstract class
  rather than a trait. ``PHAB_ID=D251314``

* finatra-thrift: `c.t.finatra.thrift.internal.ThriftMethodService` is now
  private. ``PHAB_ID=D251186``

* finatra-thrift: `c.t.finatra.thrift.exceptions.FinatraThriftExceptionMapper` and
  `c.t.finatra.thrift.exceptions.FinatraJavaThriftExceptionMapper` now extend
  `ExceptionManager[Throwable, Nothing]` since the return type was never used. They are
  now also final. ``PHAB_ID=D249011``

* finatra-thrift: Remove `c.t.finatra.thrift.routing.JavaThriftRouter#beforeFilter`. This method
  adds too much confusion to the Router API and users are encouraged to instead apply their
  TypeAgnostic Filters directly to the resultant `Service[-R, +R]`  by overriding the
  `c.t.finatra.thrift.AbstractThriftServer#configureService` method instead. ``PHAB_ID=D245424``

* finatra-thrift: `c.t.finagle.Filter.TypeAgnostic` filters are now the standard type of filter
  that can be added by configuring a `ThriftRouter`. `c.t.finatra.thrift.ThriftFilter` has been
  deprecated. ``PHAB_ID=D238666``

* finatra-thrift: `c.t.finatra.thrift.ThriftRequest` has been deprecated. All of the information
  contained in a ThriftRequest can be found in other ways:
    `methodName` -> `Method.current.get.name`
    `traceId`    -> `Trace.id`
    `clientId`   -> `ClientId.current`
  ``PHAB_ID=D238666``

Fixed
~~~~~

* finatra-http: Validate headers to prevent header injection vulnerability. ``PHAB_ID=D246889``

Closed
~~~~~~

18.11.0
-------

Added
~~~~~

Changed
~~~~~~~

* finatra-thrift: Fixes and improvements for better Java support. ExceptionMappingFilter now
  works properly with generated Java controllers, added an exception mapper for the exceptions
  defined in `finatra_thrift_exceptions.thrift` which works on the geneated Java code for these
  exceptions. Better Java API separation to make usage less error prone and confusing.
  ``PHAB_ID=D237483``

* finatra-thrift: (BREAKING API CHANGE) Update `DarkTrafficFilter#handleFailedInvocation` to accept
  the request type for more fidelity in handling the failure. ``PHAB_ID=D237484``

* finatra-http: Move `request.ContentType` and `response.Mustache` Java annotations to
  `com.twitter.finatra.http` package namespace. ``PHAB_ID=D237485``

* finatra-jackson: Move away from deprecated code and update error handling and exceptions post
  Jackson 2.9.x upgrade. ``PHAB_ID=D229601``

* inject-core: (BREAKING API CHANGE) Remove `c.t.inject.TestMixin#sleep`. We do not want to
  promote this usage of Thread blocking in testing utilities. Add a new testing function:
  `c.t.inject.TestMixin#await` which will perform `Await.result` on a given `c.t.util.Awaitable`.
  This function was duplicated across tests in the codebase. We also introduce an overridable default
  timeout on the underlying `Await.result` call: `c.t.inject.TestMixin#defaultAwaitTimeout`.
  ``PHAB_ID=D231717``

Fixed
~~~~~

* finatra-http: Fix registration of HTTP Routes in the Library registry to properly account
  for Routes that duplicate a URI with a different HTTP verb. That is, a Route should be considered
  unique per URI + HTTP verb combination. ``PHAB_ID=D232014``

Closed
~~~~~~

18.10.0
-------

Added
~~~~~

Changed
~~~~~~~

* finatra-http, finatra-thrift: Make HTTP and Thrift StatsFilters "Response Classification"
  aware. ``PHAB_ID=D219116``

* finatra-http, finatra-thrift: (BREAKING API CHANGE) Update the `DarkTrafficFilterModule` in
  both HTTP and Thrift to allow for specifying further configuration of the underlying Finagle client.
  This allows users the ability to set Finagle client concerns like ResponseClassification or other
  configuration not expressed by the DarkTrafficFilterModule's API.

  Additionally, the Thrift `DarkTrafficFilterModule` has been updated to be ThriftMux only.
  For more information on mux see: `What is ThriftMux <https://twitter.github.io/finagle/guide/FAQ.html?highlight=thriftmux#what-is-thriftmux>`__.

  We also update the `enableSampling` method to accept a `c.t.inject.Injector` to aid in the
  decision-making for if a given request should be "sampled" by the filter. ``PHAB_ID=D225897``

* finatra-thrift: (BREAKING API CHANGE) Update `c.t.finatra.thrift.routing.ThriftRouter` API for
  adding Java Thrift controllers. The `service: Class[_]` was rendered unnecessary some time ago
  but not removed from the API signature. Because this parameter is useless and it shadows
  another variable inside of the code we remove it from the signature altogether rather than
  deprecating the API. ``PHAB_ID=D224336``

* finatra-thrift: Rename `defaultFinatraThriftPort` to `defaultThriftPort`.
  ``PHAB_ID=D224735``

Fixed
~~~~~

* finatra-thrift: Set the bound `StatsReceiver` in the underlying Finagle `ThriftMux` server
  in the `c.t.finatra.thrift.ThriftServer`. This prevented testing of underlying Finagle server
  stats as the `InMemoryStatsReceiver` used by the `EmbeddedThriftServer` was not properly passed
  all the way through the stack. ``PHAB_ID=D228494``

Closed
~~~~~~

18.9.1
------

Added
~~~~~

Changed
~~~~~~~

* finatra-thrift: Allow java classes to extend ThriftFilter via AbstractThriftFilter.
  ``PHAB_ID=D221534``

* http/thrift: Update Library registry route information to include controller
  class name. ``PHAB_ID=D216425``

Fixed
~~~~~

Closed
~~~~~~

18.9.0
------

Added
~~~~~

Changed
~~~~~~~

* inject-core: Remove unnecessary Await.result Future.Value in TestMixin. ``PHAB_ID=D208995``

* finatra-http: (BREAKING API CHANGE) ``c.t.io.Reader`` and ``c.t.io.Writer`` are now abstracted
  over the type they produce/consume (``Reader[A]`` and ``Writer[A]``) and are no longer fixed to
  ``Buf``. ``PHAB_ID=D195638``

Fixed
~~~~~

Closed
~~~~~~

18.8.0
------

Added
~~~~~

Changed
~~~~~~~

* finatra-http: (BREAKING API CHANGE) Typical TLS Configuration for an HTTPS server has been
  moved into a trait, ``c.t.finatra.http.Tls`` which also defines the relevant flags (and
  overridable defaults) for specifying the SSL cert and key paths. Users can choose to mix this
  trait into their ``c.t.finatra.http.HttpServer`` classes in order to specify an HTTPS server.
  Users who wish to maintain the current HTTPS functionality SHOULD mix in the Tls trait to their
  HttpServer: e.g., ``class FooService extends HttpServer with Tls { ...   }`` Additionally, TLS
  transport configuration for the underlying Finagle ``c.t.finagle.Http.Server`` is no longer
  done by default when creating and running an HTTPS server. This is to allow for more flexible
  configuration on the underlying ``c.t.finagle.Http.Server`` when setting up TLS. Thus it is
  recommended that users ensure to either mix in the provided Tls trait or provide the correct
  ``c.t.finagle.Http.Server`` transport configuration via the ``configureHttpsServer`` method.
  ``PHAB_ID=D193579``

* finatra-http: Rename ``defaultFinatraHttpPort`` to ``defaultHttpPort``. ``PHAB_ID=D193578``

* finatra-utils: Remove deprecated ``c.t.f.utils.Handler``. ``PHAB_ID=D192288``

Fixed
~~~~~

Closed
~~~~~~

18.7.0
------

Added
~~~~~

* inject-utils: Add 'toLoggable' implicit from Array[Byte] to String.
  ``PHAB_ID=D182262``

Changed
~~~~~~~

Fixed
~~~~~

* finatra-http: Fix infinite loop introduced by ``PHAB D180166``. Fix underlying issue of the
  ``ResponseBuilder`` requiring a stored ``RouteInfo`` for classifying exceptions for stating.
  ``PHAB_ID=D189504``

* finatra-http: Fix FailureExceptionMapper handling of wrapped exceptions. Unwrap cause for
  all ``c.t.finagle.Failure`` exceptions, regardless of flags and add a try-catch to
  ``ExceptionManager`` to remap exceptions thrown by ``ExceptionMapper``\ s ``PHAB_ID=D180166``

* finatra-http: (BREAKING API CHANGE) Fix HttpResponseFilter to properly respect URI schema
  during location header overwriting\ ``PHAB_ID=D191448``

Closed
~~~~~~

18.6.0
------

Added
~~~~~

* finatra: Add HTTP route, Thrift method, and Filter information to the Library registry.
  ``PHAB_ID=D177583``

* finatra-inject/inject-logback: Add an ``c.t.inject.logback.AsyncAppender`` to provide
  metrics about the underlying queue. ``PHAB_ID=D173278``

Changed
~~~~~~~

* inject-slf4j: Move the SLF4J API logging bridges from ``inject-slf4j`` to ``inject-app``
  and ``inject-server``. This allows code in the inject framework to be mostly useful in
  environments where having the bridges on the classpath causes issues. ``PHAB_ID=D179652``

Fixed
~~~~~

* finatra-http: Fail startup for incorrect Controller callback functions. Controller route callback
  functions that do not specify an input parameter or specify an incorrect input parameter should
  fail server startup but were not correctly detected when building routes in the ``CallbackConverter``.
  The route building logic has been patched to correctly detect these routes which would fail at
  runtime to ensure we fail fast at server startup (and can thus be caught by StartupTests).
  ``PHAB_ID=D178330``

* finatra-http: Change exceptions emitted from ``c.t.f.http.filter.HttpNackFilter`` to not extend
  from ``HttpException`` and add a specific mapper over ``HttpNackException`` such that Nack
  exceptions are handled distinctly from HttpExceptions and thus more specifically. Handling
  of Nack exceptions should not be conflated with handling of the more generic ``HttpExceptions``
  and it should be clear if a new mapper is desired that it is specifically for changing how Nack
  exceptions are handled. ``PHAB_ID=D172456``

Closed
~~~~~~

18.5.0
------

Added
~~~~~

* examples: Add external TwitterServer example. ``PHAB_ID=D161204``

Changed
~~~~~~~

* inject-utils: Remove deprecated ``c.t.inject.RootMonitor``.
  ``PHAB_ID=D161036``

* finatra-http: Updated ``c.t.finatra.http.AdminHttpServer`` to
  isolate routes added to the admin. ``PHAB_ID=D157818``

Fixed
~~~~~

* inject-slf4j, finatra-http: Fix ``c.t.inject.logging.FinagleMDCAdapter`` to initialize
  properly. We were lazily initializing the backing ``java.util.Map``
  of the ``FinagleMDCAdapter``
  which could cause values to disappear when the map was not created
  eagerly enough. Typical
  usage would add one of the MDC logging filters to the top of the
  request filter chain which would
  put a value into the MDC thus creating the backing ``java.util.Map``
  early in the request chain.
  However, if a filter which puts to the MDC was not included and the
  first put happened in a
  Future closure the map state would be lost upon exiting the closure.

  This change updates how the MDC mapping is stored to move from a
  ``Local`` to a ``LocalContext``
  and introduces new ergonomics for using/initializing the framework MDC
  integration.

  Initialization of the MDC integration should now go through the
  ``c.t.inject.logging.MDCInitializer`` (that is users are not expected to
  need to interact directly with the ``FinagleMDCAdapter``). E.g.,
  to initialize the MDC:

  ``com.twitter.inject.logging.MDCInitializer.init()``

  This will initialize the ``org.slf4j.MDC`` and swap out the default
  ``org.slf4j.spi.MDCAdapter`` with
  an instance of the ``c.t.inject.logging.FinagleMDCAdapter`` allowing
  for reading/writing MDC values across Future boundaries.

  Then to start the scoping of an MDC context, use
  ``c.t.inject.logging.MDCInitializer#let``:

  ``com.twitter.inject.logging.MDCInitializer.let {     // operations which set and read MDC values     ???   }``
  Typically, this is done in a Filter wrapping the execution of the
  service in the Filter's apply,
  For example, the framework provides this initialization and scoping in
  both the ``c.t.finatra.http.filters.LoggingMDCFilter`` and the
  ``c.t.finatra.thrift.filters.LoggingMDCFilter``.

  Simply including these at the top of the request filter chain for a
  service will allow MDC integration to function properly. ``PHAB_ID=D159536``

*  inject-app: Ensure that installed modules are de-duped before
   creating injector. ``PHAB_ID=D160955``

Closed
~~~~~~

18.4.0
------

Added
~~~~~

* finatra-http: Added the ability for requests to have a maximum
  forward depth to
  ``c.t.finatra.http.routing.HttpRouter``, which prevents requests
  from being forwarded
  an infinite number of times. By default the maximum forward depth
  is 5. ``PHAB_ID=D154737``

* inject-thrift-client: Update ``configureServicePerEndpoint`` and
  ``configureMethodBuilder`` in ``ThriftMethodBuilderClientModule``
  to also pass a
  ``c.t.inject.Injector`` instance which allows users to use bound
  instances from the object graph when providing further ``thriftmux.MethodBuilder``
  or ``ThriftMethodBuilderFactory`` configuration.
  ``PHAB_ID=D155451``

* inject-thrift-client: Update ``configureThriftMuxClient`` in
  ``ThriftClientModuleTrait`` to
  also pass a ``c.t.inject.Injector`` instance which allows users to
  use bound instances
  from the object graph when providing further ``ThriftMux.client``
  configuration.
  ``PHAB_ID=D152973``

* inject-server: Capture errors on close of the underlying
  TwitterServer. The embedded
  testing utilities can now capture and report on an exception that
  occurs during close
  of the underlying TwitterServer.
  ``EmbeddedTwitterServer#assertCleanShutdown`` inspects
  for any Throwable captured from closing the underlying server which
  it will then throw.
  ``PHAB_ID=D148946``

* finatra-http: Created a new API into
  ``c.t.f.h.response.StreamingResponse`` which permits passing
  a ``transformer`` which is an
  ``AsynStream[T] => AsyncStream[(U, Buf)]`` for serialization
  purposes,
  as well as two callbacks -* ``onDisconnect``, called when the
  stream is disconnected, and ``onWrite``,
  which is a ``respond`` side-effecting callback to every individual
  write to the stream.
  ``PHAB_ID=D147925``

Changed
~~~~~~~

* inject-app: Update and improve the test ``#bind[T]`` DSL. The testing
  ``#bind[T]`` DSL is lacking in
  its ability to be used from Java and we would like to revise the API
  to be more expressive such
  that it also includes binding from a Type to a Type. Due to wanting
  to also support the ability
  to bind a Type to a Type, the DSL has been re-worked to more closely
  match the actual Guice binding DSL.

  For Scala users the ``#bind[T]`` DSL now looks as follows:

  ::

      bind[T].to[U <: T]
      bind[T].to[Class[U <: T]]
      bind[T].toInstance(T)

      bind[T].annotatedWith[Ann].to[U <: T]
      bind[T].annotatedWith[Ann].to[Class[U <: T]]
      bind[T].annotatedWith[Ann].toInstance(T)

      bind[T].annotatedWith[Class[Ann]].to[U <: T]
      bind[T].annotatedWith[Class[Ann]].to[Class[U <: T]]
      bind[T].annotatedWith[Class[Ann]].toInstance(T)

      bind[T].annotatedWith(Annotation).to[U <: T]
      bind[T].annotatedWith(Annotation).to[Class[U <: T]]
      bind[T].annotatedWith(Annotation).toInstance(T)

      bindClass(Class[T]).to[T]
      bindClass(Class[T]).to[Class[U <: T]]
      bindClass(Class[T]).toInstance(T)

      bindClass(Class[T]).annotatedWith[Class[Ann]].to[T]
      bindClass(Class[T]).annotatedWith[Class[Ann]].[Class[U <: T]]
      bindClass(Class[T]).annotatedWith[Class[Ann]].toInstance(T)

      bindClass(Class[T]).annotatedWith(Annotation).to[T]
      bindClass(Class[T]).annotatedWith(Annotation).[Class[U <: T]]
      bindClass(Class[T]).annotatedWith(Annotation).toInstance(T)


  For Java users, there are more Java-friendly methods:

  ::

      bindClass(Class[T], T)
      bindClass(Class[T], Annotation, T)
      bindClass(Class[T], Class[Annotation], T)

      bindClass(Class[T], Class[U <: T])
      bindClass(Class[T],  Annotation, Class[U <: T])
      bindClass(Class[T], Class[Annotation], Class[U <: T])

  Additionally, these changes highlighted the lack of Java-support in
  the ``TwitterModule`` for
  creating injectable Flags. Thus ``c.t.inject.TwitterModuleFlags`` has
  been updated to also provide
  Java-friendly flag creation methods:

  ::

      protected def createFlag[T](name: String, default: T, help: String, flggble: Flaggable[T]): Flag[T]
      protected def createMandatoryFlag[T](name: String, help: String, usage: String, flggble: Flaggable[T]): Flag[T]``

  ``PHAB_ID=D149252``

* inject-thrift-client: The "retryBudget" in the
  ``c.t.inject.thrift.modules.ThriftMethodBuilderClientModule``
  should be a ``RetryBudget`` and not the generic ``Budget``
  configuration Param. Updated the type.
  ``PHAB_ID=D151938``

* inject-server: Move HTTP-related concerns out of the embedded
  testing utilities into
  specific HTTP "clients". The exposed ``httpAdminClient`` in the
  ``EmbeddedTwitterServer``
  and the ``httpClient`` and ``httpsClient`` in the
  ``EmbeddedHttpServer`` are no longer just
  Finagle Services from Request to Response, but actual objects. The
  underlying Finagle
  ``Service[Request, Response]`` can be accessed via
  ``Client.service``. ``PHAB_ID=D148946``

Fixed
~~~~~

Closed
~~~~~~

18.3.0
------

Added
~~~~~

* inject-server: Add a lint rule in
  ``c.t.inject.server.TwitterServer#warmup``. If a server does not
  override the default implementation of ``TwitterServer#warmup`` a
  lint rule violation will appear
  on the lint page of the HTTP admin interface. ``PHAB_ID=D141267``

* inject-server: Add ``c.t.inject.server.TwitterServer#setup``
  lifecycle callback method. This is
  run at the end of the ``postInjectorStartup`` phase and is
  primarily intended as a way for
  servers to start pub-sub components on which the server depends.
  Users should prefer this method
  over overriding the ``c.t.inject.server.TwitterServer#postWarmup``
  @Lifecycle-annotated method as
  the callback does not require a call its super implementation for
  the server to correctly start
  and is ideally less error-prone to use. ``PHAB_ID=D135827``

* inject-app: Add ``c.t.inject.annotations.Flags#named`` for getting
  an implementation of an ``@Flag``
  annotation. This is useful when trying to get or bind an instance
  of an ``@Flag`` annotated type.
  ``PHAB_ID=D140831``

Changed
~~~~~~~

* finatra-http: ``ReaderDiscarded`` failures writing in
  ``c.t.f.http.StreamingResponse`` now only log
  at the info level without a stack trace, while other failures log
  at the error level with
  a stacktrace. ``PHAB_ID=D141453``

* inject-thrift-client: Removed ``withBackupRequestFilter`` method on
  deprecated
  ``c.t.inject.thrift.filters.ThriftClientFilterChain``. Instead of
  ``c.t.inject.thrift.modules.FilteredThriftClientModule``, use
  ``c.t.inject.thrift.modules.ThriftMethodBuilderClientModule`` and
  use the ``idempotent`` method on
  ``c.t.inject.thrift.ThriftMethodBuilder`` to configure backup
  requests. ``PHAB_ID=D142049``.

* inject-app: ``c.t.inject.annotations.FlagImpl`` is no longer public
  and should not be used directly.
  Use ``c.t.inject.annotations.Flags#named`` instead.
  ``PHAB_ID=D140831``

Fixed
~~~~~

* inject-thrift-client: Fix for duplicate stack client registration.
  The
  ``c.t.inject.thrift.modules.ThriftMethodBuilderClientModule`` was
  incorrectly calling the
  ``ThriftMux.client`` twice. Once to create a MethodBuilder and once
  to create a ServicePerEndpoint.
  Now, the ServicePerEndpoint is obtained from the configured
  MethodBuilder. ``PHAB_ID=D141304``

* inject-thrift-client: Convert non-camel case ``ThriftMethod``
  names, e.g., "get\_tweets" to
  camelCase, e.g., "getTweets" for reflection lookup on generated
  ``ServicePerEndpoint`` interface in
  ``c.t.inject.thrift.ThriftMethodBuilder``. ``PHAB_ID=D138499``

Closed
~~~~~~

18.2.0
------

Added
~~~~~

* inject-thrift-client: Add methods to
  ``c.t.inject.thrift.filters.ThriftClientFilterChain`` to allow
  Tunable timeouts and request timeouts. ``PHAB_ID=D128506``

* inject-thrift-client: Add ``idempotent`` and ``nonIdempotent``
  methods to
  ``c.t.inject.thrift.ThriftMethodBuilder``, which can be used to
  configure retries and the sending of
  backup requests. ``PHAB_ID=D129959``

* inject-thrift-client: Add
  ``c.t.inject.thrift.modules.ServicePerEndpointModule`` for
  building ThriftMux clients using the ``thriftmux.MethodBuilder``.
  ``PHAB_ID=D128196``

Changed
~~~~~~~

* inject-thrift: Update ``c.t.inject.thrift.PossibleRetryable`` to specify a
  ResponseClassifier and update usages in inject-thrift-client to use it. ``PHAB_ID=D134328``

* inject-thrift-client: Un-deprecate ``c.t.inject.thrift.modules.ThriftClientModule`` and
  update for parity with ``ServicePerEndpointModule`` in regards to ThriftMux client configuration.
  Update documentation. Rename ``ServicePerEndpointModule`` to the more descriptive and consistently
  named ``ThriftMethodBuilderClientModule``. ``PHAB_ID=D129891``

Fixed
~~~~~

Closed
~~~~~~

18.1.0
------

Added
~~~~~

* finatra-thrift: Add support for building all types of Finagle Thrift clients to the underlying
  embedded TwitterServer with the ``c.t.finatra.thrift.ThriftClient`` test utility.
  See: `Creating a client <https://twitter.github.io/scrooge/Finagle.html#creating-a-client>`__
  ``PHAB_ID=D123915``

* finatra-jackson: Added support to finatra/jackson for de-serializing
  ``com.twitter.util.Duration`` instances from their String representations.
  ``PHAB_ID=D122366``

Changed
~~~~~~~

*  finatra-http: Change visibility of internal class
   ``c.t.finatra.http.internal.marshalling.RequestInjectableValues``
   to be correctly specified as private to the ``http`` package.
   ``PHAB_ID=D127975``

Fixed
~~~~~

*  finatra-http: Ensure we close resources in the ``ResponseBuilder``.
   Addresses `#440 <https://github.com/twitter/finatra/issues/440>`__. ``PHAB_ID=D120779``

Closed
~~~~~~

17.12.0
-------

Added
~~~~~

*  finatra-thrift: Add tests for new Scrooge
   ``ReqRepServicePerEndpoint``
   functionality. ``PHAB_ID=D107397``

Changed
~~~~~~~

*  finatra-http: add a ``multipart = true`` arg to
   ``EmbeddedHttpServer.httpMultipartFormPost``
   \`\ ``PHAB_ID=D113151``
*  inject-sever: Do not use the
   ``c.t.inject.server.EmbeddedTwitterServer``
   ``InMemoryStatsReceiver`` for embedded http clients. The http client
   stats are
   emitted with the server under test stats which can be confusing, thus
   we now
   create a new ``InMemoryStatsReceiver`` when creating an embedded http
   client.
   ``PHAB_ID=D112024``

Fixed
~~~~~

Closed
~~~~~~

17.11.0
-------

Added
~~~~~

Changed
~~~~~~~

*  EmbeddedTwitterServer, EmbeddedHttpServer, and EmbeddedThriftServer
   flags
   and args parameters changed to call-by-name.
   \`\ ``PHAB_ID=``\ D104733\`

Fixed
~~~~~

*  inject-server: Ensure EmbeddedTwitterServer has started before trying
   to
   close httpAdminClient. ``PHAB_ID=D111294``

Closed
~~~~~~

17.10.0
-------

Added
~~~~~

* inject-core: Remove deprecated ``c.t.inject.TestMixin#resetMocks``.
  Properly
  use ``c.t.inject.Mockito`` trait in tests. Deprecate resetting of
  mocks and
  resettables in ``c.t.inject.IntegrationTestMixin``.
  ``PHAB_ID=D93876``

* finatra-http: Parameterize
  ``@RouteParam``,\ ``@QueryParam``,\ ``@FormParam``, and
  ``@Header`` to allow specifying the field name to read from the
  params or
  header map. Previously these annotations only looked for values by
  the
  case class field name leading to possible ugliness when defining
  case
  class fields (especially with ``@Header``).
  \`\ ``PHAB_ID=``\ D94220\`

* finatra: Add support for using a
  ``java.lang.annotation.Annotation`` instance
  with the ``#bind[T]`` testing DSL. This adds a way to bind
  instances in tests
  that use the @Named binding annotation. ``PHAB_ID=D91330``

* finatra-http: Allow setting the content type of a Mustache view.
  ``PHAB_ID=D91949``

Changed
~~~~~~~

*  finatra-http: Move ``FileResolver`` to finatra/utils.
   ``PHAB_ID=D103536``

*  finatra-utils: Move ``ResponseUtils`` to finatra/http.
   ``PHAB_ID=D103507``

* From now on, release versions will be based on release date in the
  format of
  YY.MM.x where x is a patch number. ``PHAB_ID=D101244``

*  finatra-utils: Remove deprecated ``ExternalServiceExceptionMatcher``.
   ``PHAB_ID=D98343``

* finatra-jackson: ScalaType's ``isMap`` and ``isCollection`` methods
  now check that
  the given object's class is a subclass of
  ``scala.collection.Map[Any, Any]`` and
  ``scala.collection.Iterable[Any]``, respectively. Previously the
  superclasses'
  packages were unspecified. This is a runtime behavior change.
  ``PHAB_ID=D93104``

* finatra-http: Require that route URIs and prefixes begin with
  forward slash (/).
  ``PHAB_ID=D90895``

* inject-utils: (BREAKING API CHANGE) RichOption toFutureOrFail,
  toTryOrFail, and
  toFutureOrElse signature changed to take the fail or else parameter
  by name.
  ``PHAB_ID=D89544``

* inject-server: Remove usage of deprecated
  ``c.t.inject.logging.Slf4jBridgeUtility``.
  Change usages to ``c.t.util.logging.Slf4jBridgeUtility``.
  ``PHAB_ID=D88095``

* finatra-http, inject-thrift-client: Remove netty3 specific types
  and dependency.
  In finatra-http, the code using these types is deprecated and can
  be removed allowing
  us to remove netty3-specific dependencies. In inject-thrift-client
  we can default to
  use the DefaultTimer for the backupRequestFilter method param
  instead of the
  HashedWheelTimer. ``PHAB_ID=D88025``

Fixed
~~~~~

* finatra-http: Parameterized route callback inputs fail because the
  lookup of a
  corresponding ``MessageBodyManager`` reader lookup does not
  properly handle parameterized
  types such as collections. This change updates the
  ``MessageBodyManager`` ``MessageBodyReader``
  lookup to take into account parameterized types. This allows for a
  user to parse a
  ``Seq[T]``, or ``Map[K, V]`` as a route callback input type using
  the default Finatra
  ``MessageBodyReader``. ``PHAB_ID=D104277``

* finatra-jackson: Fix issue causing ``IllegalArgumentException``
  from Validations to
  be swallowed. A catch clause in the
  ``c.t.finatra.json.internal.caseclass.jackson.FinatraCaseClassDeserializer``
  is too broad as it catches thrown ``IllegalArgumentException``\ s
  from field validations
  when the annotation is applied to a field of the incorrect type,
  e.g., when ``@Max`` is
  applied to a String field. ``PHAB_ID=D95306``

Closed
~~~~~~

2.13.0
------

Added
~~~~~

*  inject-server: Add ability to fail embedded server startup on lint
   rule violation.
   There is now a flag in the embedded servers that when set to true
   will fail
   server startup if a lint rule violation is detected. This will then
   fail
   the running test. ``PHAB_ID=D82399``

Changed
~~~~~~~

*  finatra-http: No longer depend on bijection-util. ``PHAB_ID=D86640``

* finatra-jackson: Deprecate
  c.t.finatra.json.utils.CamelCasePropertyNamingStrategy.
  This object was created to reduce ambiguity with previous releases
  of Jackson in which
  the default PropertyNamingStrategy was an abstract class with a
  default of camel case.
  Users are encouraged to use the Jackson PropertyNamingStrategy
  constants directly. ``PHAB_ID=D81707``

Fixed
~~~~~

Closed
~~~~~~

2.12.0
------

Added
~~~~~

*  finatra-jackson: Add support for injecting a snake case
   FinatraObjectMapper by annotating
   parameters with a new @SnakeCaseMapper binding annotation.
   ``PHAB_ID=D7798``

Changed
~~~~~~~

* finatra-http: Add close hook when constructing a StreamingResponse
  to allow for resource
  release without consuming an entire AsyncStream. ``PHAB_ID=D64013``

* finatra-http: Unmarshalling JSON no longer consumes the body of a
  HTTP Request.
  ``PHAB_ID=D74519``

* finatra-inject: RetryUtil.retry has been removed because it used a
  blocking call
  to Thread.sleep. Blocking Finagle threads results in poor
  performance and
  RetryUtil.retryFuture should be used instead. ``PHAB_ID=D73949``

Fixed
~~~~~

Closed
~~~~~~

2.11.0
------

Added
~~~~~

Changed
~~~~~~~

Fixed
~~~~~

*  finatra-jackson: Fix JSON deserialization of scala.util.Either type
   in FinatraObjectMapper
   for Scala 2.12. ``RB_ID=917699``

Closed
~~~~~~

2.10.0
------

Added
~~~~~

Changed
~~~~~~~

*  finatra-http: Increase composability and flexibility of RouteDSL.
   ``RB_ID=912095``

* inject-app: Run installed modules postInjectorStartup before server
  function. This makes
  reasoning about the server lifecycle a bit more straight-forward
  and simplifies things
  like the exception manager logic for adding and overridding
  mappers. ``RB_ID=911965``

*  finatra-jackson: Update framework tests to FunSuite ScalaTest testing
   style. ``RB_ID=911745``

* finatra: Move finatra/benchmarks and finatra/utils framework tests
  to FunSuite ScalaTest
  testing style. ``RB_ID=910680``

Fixed
~~~~~

* finatra-http: Correctly return a JsonParseException when the
  incoming JSON is not parsable
  as an expected custom case class request object. ``RB_ID=912529``

* finatra-http: Ensure underlying members are injected for
  AbstractControllers. ``RB_ID=911635``

* finatra-jackson: Patch ``FinatraDatetimeDeserializer`` to support
  parsing of Long value passed
  as String, e.g., when parsing a query parameter.\ ``RB_ID=911162``

* finatra: Close embedded server clients on embedded server close.
  ``RB_ID=910862``

Closed
~~~~~~

2.9.0
-----

Added
~~~~~

Changed
~~~~~~~

*  inject-core: (BREAKING API CHANGE) Allow for binding of higher-kinded
   types when testing. Deprecated ``@Bind`` mechanism for replacing bound types in an object
   graph. Now instead of using ``@Bind`` like this:

  ::

      class DarkTrafficCanonicalResourceHeaderTest
        extends FeatureTest
        with Mockito {

        @Bind
        @DarkTrafficService
        val darkTrafficService: Option[Service[Request, Response]] =
          Some(smartMock[Service[Request, Response]])

        /* mock request */
        darkTrafficService.get.apply(any[Request]).returns(Future.value(smartMock[Response]))

        override val server = new EmbeddedHttpServer(
          twitterServer = new DarkTrafficTestServer)

        test("DarkTrafficServer#has Canonical-Resource header correctly set") {
         ...

  Users can instead do:

  ::

      class DarkTrafficCanonicalResourceHeaderTest
      extends FeatureTest
      with Mockito {
       val darkTrafficService: Option[Service[Request, Response]] =
         Some(smartMock[Service[Request, Response]])

       /* mock request */
       darkTrafficService.get.apply(any[Request]).returns(Future.value(smartMock[Response]))

       override val server = new EmbeddedHttpServer(
         twitterServer = new DarkTrafficTestServer)
         .bind[Option[Service[Request, Response]], DarkTrafficService](darkTrafficService)

       test("DarkTrafficServer#has Canonical-Resource header correctly set") {
         ...

  This allows for more flexibility (as the binding is now per object
  graph, rather
  than per test files) and is less susceptible to errors due to
  incorrect usage.

  The breaking API change is due to adding this support in the
  TestInjector, it is
  now required that users call the ``TestInjector#create`` method in
  order to build
  the injector and that this is done *after* calls to
  ``TestInjector#bind``. Previously,
  an ``Injector`` was directly returned from ``TestInjector#apply``
  which is no longer true,
  thus it may look like your IntegrationTests are broken as you now need
  to add a
  call to ``TestInjector#create``.

  Additionally, this change updates all of the framework tests in the
  inject modules to
  the FunSuite testing style from the deprecated WordSpec testing style.
  ``RB_ID=910011``

* finatra-thrift: Update framework tests to FunSuite ScalaTest testing
  style. ``RB_ID=910262``

* inject-core: Move Logging from grizzled-slf4j to
  util/util-slf4j-api.
  ``c.t.inject.Logger`` is now deprecated in favor of
  ``c.t.util.logging.Logger``
  in util. ``PHAB_ID=D29713``

*  finatra-httpclient: Update framework tests to FunSuite ScalaTest
   testing style. ``RB_ID=909526``

*  finatra-http: Update framework tests to FunSuite ScalaTest testing
   style. ``RB_ID=909349``

*  finatra: Bump guava to 19.0. ``RB_ID=907807``

* inject-thrift-client: Various APIs have changed to work with
  ``ThriftMethod.SuccessType``
  instead of ``ThriftMethod.Result``. See
  ``ThriftClientFilterChain``, ``Controller``,
  ``ThriftWarmup``, ``PossiblyRetryable``. ``RB_ID=908846``

Fixed
~~~~~

* finatra-http: Correctly support adding Java AbstractController by
  instance. ``RB_ID=910502``

Closed
~~~~~~

2.8.0
-----

Added
~~~~~

* finatra-http: Add Java support for declaring admin routes.
  ``RB_ID=906264``

* finatra-http: Add AbstractExceptionMapper for ExceptionMapper usage
  from Java.
  Also update the HttpRouter to allow for registration of
  AbstractExceptionMappers.
  ``RB_ID=902995``

* finatra-http: Support for JSON Patch
  (https://tools.ietf.org/html/rfc6902). Utilities are
  located in package ``com.twitter.finatra.http.jsonpatch``.
  ``RB_ID=889152``

* finatra: Created companion trait mixins for
  Test/FeatureTest/IntegrationTest/HttpTest.
  ``RB_ID=897778``

* finatra-http: Support for optional trailing slashes in HTTP routes.
  Routes can
  now specify that they allow an optional trailing slash by ending
  the route URI
  in the Controller with "/?". ``RB_ID=893167``

* finatra-http: Support for Controller route prefixes. This allows
  users to define a
  common prefix for a set of routes declaratively inside a
  controller. ``RB_ID=894695``

Changed
~~~~~~~

* inject-core: Add back JUNitRUnner to ``c.t.inject.Test`` and
  ``c.t.inject.WordSpecTest``
  so that tests can be run when building with maven. ``RB_ID=909789``

* finatra-http: Allow routes which begin with "/admin" to be exposed
  on the external
  interface and routes which DO NOT begin with "/admin" to be exposed
  on the admin interface.
  NOTE: routes which begin with "/admin/finatra" will continue to be
  on the admin interface
  only. Routes which begin with "/admin" that should be served from
  the admin interface MUST
  set the flag "admin = true" on the route in the Controller.
  ``RB_ID=905225``

* finatra: Move conversions and retry utilities from finatra/utils to
  finatra/inject/inject-utils.
  ``RB_ID=905109``

* finatra: (BREAKING API CHANGE) Rename the existing test helper
  classes to include
  their current opinionated testing style, "WordSpec". These are
  functionally
  equivalent as this is just a name change. We also introduce new
  versions of the
  test helpers which mix in the recommended FunSuite. Thus it will
  look like your
  tests are broken as you will need to update to change to use the
  new "WordSpec"
  classes or changed your testing style to the recommended
  ``FunSuite`` style.
  ``PHAB_ID=D19822``

* inject-core: Remove JUnitRunner from ``c.t.inject.Test``. This was
  only necessary for
  internal building with pants and is no longer required. The sbt
  build uses the
  ScalaTest runner and is thus not affected. Additionally, update
  specs2 to 2.4.17 and
  to depend on just the ``specs2-mock`` dependency where needed.
  ``PHAB_ID=D18011``

Fixed
~~~~~

* finatra-http: Fix issue where added admin routes did not have their
  HTTP method
  correctly specified leading to all routes being defaulted to 'GET'.
  ``RB_ID=905887``

* finatra-http: Fix for custom request case class collection-type
  fields which are
  annotated with either ``@RouteParam``, ``@QueryParam``, or
  ``@FormParam`` to correctly
  use a specified default value when a value is not sent in the
  request. ``RB_ID=903697``

* inject-app: Fix TestInjector to properly parse flags. The
  TestInjector didn't
  properly handle defaulted boolean flags when defined in Modules.
  Updated the
  TestInjector logic to properly parse flags. Fixes `Issue
  #373 <https://github.com/twitter/finatra/issues/373>`__
  ``RB_ID=901525``

* finatra: Correctly filter published tests-javadocs and
  tests-sources jars for
  projects. We are incorrectly publishing tests in the sources and
  javadocs jars
  for projects which publish a test-jar dependency (http, httpclient,
  jackson,
  thrift, util, inject-app, inject-core, inject-modules, and
  inject-server).
  ``RB_ID=901153``

Closed
~~~~~~

2.7.0
-----

Added
~~~~~

* finatra-http: Add built-in support for Scala
  ``scala.concurrent.Future``. The
  CallbackConverter now supports a return type of Scala
  ``scala.concurrent.Future``
  by using a bijection to convert to a Twitter ``c.t.util.Future``.
  ``RB_ID=898147``

* finatra-http: Support for request forwarding. Requests can be
  forwarded from
  one route to another. Forwarded requests will not go through the
  server's
  defined filter chain again but will pass through any Controller
  defined filters
  on the "forwarded to" route. ``RB_ID=883224``

Changed
~~~~~~~

Fixed
~~~~~

Closed
~~~~~~

2.6.0
-----

Added
~~~~~

*  finatra: Move the OSS documentation to internal code repository to be
   co-located with
   source code. ``RB_ID=881112``

Changed
~~~~~~~

* finatra-http: Decompose the ``ThrowableExceptionMapper`` to allow
  users to more easily replace
  the portions they care about. Users can now just replace the
  functionality per exception
  type rather than needing to replace the entire
  ``ThrowableExceptionMapper``. \`RB\_ID=891666\`\`

* finatra-http: The 'cookie' method of
  ``c.t.finatra.http.response.ResponseBuilder#EnrichedResponse``
  that takes a Netty 3 cookie instance has been deprecated. Please
  use the method which takes a
  Finagle HTTP cookie instead. ``RB_ID=888683``

* finatra-http: Update adding routes to the TwitterServer HTTP Admin
  Interface to use
  ``c.t.finagle.http.RouteIndex`` and remove the
  ``c.t.finatra.http.routing.AdminIndexInfo``.
  Also relaxed the rules for what routes can be added to the index to
  include constant
  /POST routes. Additionally, no longer fail if you define
  conflicting admin routes --
  we will now only warn. It is up to the user to not shoot themselves
  in the foot.
  ``RB_ID=889792``

*  finatra-http: Request in request case classes no longer requires
   Inject annotation. ``RB_ID=888197``

* inject-utils: Deprecated RootMonitor since finagle DefaultMonitor
  is implicitly installed
  and handles all exceptions caught in stack. We provide a monitor
  method by default is a NullMonitor in
  ``c.t.finatra.thrift.modules.DarkTrafficFilterModule`` and
  ``c.t.inject.thrift.modules.FilteredThriftClientModule``,
  users can handle other exceptions (unhandled by DefaultMonitor) by
  overriding the monitor method ``RB_ID=886773``

* finatra: We now depend on a fork of libthrift hosted in the Central
  Repository.
  The new package lives in the 'com.twitter' organization. This
  removes the necessity of
  depending on maven.twttr.com. This also means that eviction will
  not be automatic and
  using a newer libthrift library requires manual eviction if
  artifacts are being pulled
  in transitively. ``RB_ID=885879``

*  inject-thrift-client: (BREAKING API CHANGE) Update filter building
   API with
   FilteredThriftClientModule. The
   ``c.t.inject.thrift.filters.ThriftClientFilterChain``
   builder API has changed along with the underlying mechanisms to
   support
   enforcement of a "correct" filter order when using the helper
   methods. Methods
   have been renamed to a 'with'-syntax to be more inline with other
   builders and
   the confusing "globalFilter" method to the more verbose but more
   accurate
   "withAgnosticFilter". ``RB_ID=878260``

* inject-thrift-client: Remove deprecated package aliases. We'd like
  people to
  move the correct packages.\ ``RB_ID=879330``

* finatra-http: (BREAKING API CHANGE) Update StreamingResponse to
  avoid keeping
  a reference to the head of the AsyncStream. This resolves the
  memory leak
  when streaming an infinite stream. The constructor is now private;
  use the
  StreamingResponse object methods that take an AsyncStream by-name
  instead.
  \`\`RB\_ID=890205''

Fixed
~~~~~

*  finatra-http: Allow 0,1,t,f as valid boolean values for QueryParam
   case class requests.
   ``RB_ID=881939``

Closed
~~~~~~

2.5.0
-----

Added
~~~~~

*  finatra-http: Add DarkTrafficFilterModule symmetric with
   thrift/DarkTrafficFilterModule. Add DarkTrafficService annotation in
   finatra-utils and a filter function used for requests annotated with
   Annotation Type in order to add DarkTrafficFilter. ``RB_ID=878079``

Changed
~~~~~~~

*  finatra: No longer need to add an additional resolver that points to
   maven.twttr.com. ``RB_ID=878967``
*  inject-thrift-client: Stop counting response failures in the
   ``c.t.inject.thrift.ThriftClientFilterChain`` as these are now
   counted in the
   ``c.t.finagle.thrift.ThriftServiceIface``. ``RB_ID=879075``
*  finatra-jackson: Fix issue around JsonProperty annotation empty
   value. In
   CaseClassField.jsonNameForField, if the @JsonProperty annotation is
   used
   without a value, the property name is interpreted as "". It now
   follows the
   default Jackson behavior of using the name field name as the property
   name when the annotation is empty. ``RB_ID=877060``
*  finatra: Correct instances of misspelled word "converter". There are
   several instances where the word "converter" is misspelled as
   "convertor".
   Specifically, TwitterModule.addTypeConvertor has been changed to
   TwitterModule.addTypeConverter. Other internal renamings are
   TwitterDurationTypeConverter, JodatimeDurationTypeConverter, and
   JacksonToGuiceTypeConverter. ``RB_ID=877736``
*  finatra: Move installation of the SLF4JBridgeHandler to the
   constructor of
   ``c.t.inject.server.TwitterServer``. The
   ``c.t.finatra.logging.modules.Slf4jBridgeModule`` has been removed as
   there is
   now little reason to use it unless you are building an application
   directly
   from ``c.t.inject.app.App`` since the functionality is now provided
   by default
   in the constructor of ``c.t.inject.server.TwitterServer``. If using
   ``c.t.inject.app.App``, then users can use the
   ``c.t.inject.logging.modules.LoggerModule``. The main advantage is
   that slf4j
   bridges are now installed earlier in the application or server
   lifecycle and
   thus more of the initialization logging is bridged to the slf4j-api.
   ``RB_ID=870913``

Fixed
~~~~~

*  finatra-jackson: Test jar is missing files. Classes in the test
   ``c.t.finatra.validation`` package were not properly marked for
   inclusion in the finatra-jackson tests jar. They've now been added.
   ``RB_ID=878755``

Closed
~~~~~~

2.4.0
-----

Added
~~~~~

*  finatra-thrift: Enhanced support for Java Thrift services.
   ``RB_ID=868254``
*  finatra-examples: Add web/UI application example. ``RB_ID=868027``
*  inject-server: Allow for the ability to disable test logging via
   System
   property. ``RB_ID=867344``

Changed
~~~~~~~

*  finatra-http: Simplify ExceptionMapper configuration and usage.
   We are dropping the need for a specialized DefaultExceptionMapper
   (which
   was simply an ExceptionMapper[Throwable]). Instead we now allow the
   configuration of mappers in the ExceptionManager to be much more
   flexible.
   Previously, the framework tried to prevent a user from registering a
   mapper
   over a given exception type multiple times and specialized a
   "default"
   ExceptionMapper to invoke on an exception type of Throwable. The
   ExceptionManager will now accept any mapper. If a mapper is added
   over a
   type already added, the previous mapper will be overwritten.

The last registered mapper for an exception type wins.

| The framework adds three mappers to the manager by default. If a user
  wants
| to swap out any of these defaults they simply need add their own
  mapper to
| the manager for the exception type to map. E.g., by default the
  framework
| will add:
| Throwable ->
| com.twitter.finatra.http.internal.exceptions.ThrowableExceptionMapper
| JsonParseException ->
| com.twitter.finatra.http.internal.exceptions.json.JsonParseExceptionMapper
| CaseClassMappingException ->
| com.twitter.finatra.http.internal.exceptions.json.CaseClassExceptionMapper

| The manager walks the exception type hierarchy starting at the given
| exceptiontype and moving up the inheritence chain until it finds
  mapper
| configured for the type. In this manner an ExceptionMapper[Throwable]
  will
| be the last mapper invoked and performs as the "default".

| Thus, to change the "default" mapper, simply adding a new mapper over
  the
| Throwable type will suffice, i.e., ExceptionMapper[Throwable] to the
| ExceptionManager. There are multiple ways to add a mapper. Either
  through
| the HttpRouter:

::

    override def configureHttp(router: HttpRouter): Unit = {
      router
        .exceptionMapper[MyDefaultExceptionMapper]
        ...
    }

Or in a module which is then added to the Server, e.g.,

::

    object MyExceptionMapperModule extends TwitterModule {
      override def singletonStartup(injector: Injector): Unit = {
        val manager = injector.instance[ExceptionManager]
        manager.add[MyDefaultExceptionMapper]
        manager.add[OtherExceptionMapper]
      }
    }


    override val modules = Seq(
      MyExceptionMapperModule,
      ...)

| This also means we can simplify the HttpServer as we no longer need to
  expose
| any "framework" module for overridding the default ExceptionMappers.
  So the
| "def exceptionMapperModule" has also been removed.\ ``RB_ID=868614``

* finatra-http: Specify HTTP Java API consistently. ``RB_ID=868264``
* inject-core: Clean up inject.Logging trait. Remove dead code from
  Logging.
  ``RB_ID=868261``
* finatra-http: Move integration tests to a package under
  ``com.twitter.finatra.http``. ``RB_ID=866487``

Fixed
~~~~~

* finatra-http: Fix issue with unimplemented methods in
  NonValidatingHttpHeadersResponse. ``RB_ID=868480``

Closed
~~~~~~

2.3.0
-----

Added
~~~~~

* finatra-thrift: Add non-guice method to add controller to
  ThriftRouter ``RB_ID=863977``
* finatra-thrift: Add support for a "dark" traffic filter in thrift
  routing. Add a Finatra implementation
  of the Finagle AbstractDarkTrafficFilter which sub-classes
  ThriftFilter and will work in the Finatra
  filter chain. This will allow users to play incoming requests to a
  configured "dark" service. ``RB_ID=852338``

Changed
~~~~~~~

* finatra-http: Performance improvements from latest micro-benchmarking
  run.
* BREAKING API CHANGE: Removed ``HttpHeaders#setDate``,
  ``HttpHeaders#set`` and ``HttpHeaders#GMT``. ``RB_ID=865247``
* finatra-thrift: Provide access to statsReceiver argument in
  ThriftClientFilterBuilder. ``RB_ID=857286``

Fixed
~~~~~

* finatra-http: Add content headers for EmbeddedHttpServer #httpDelete
  and #httpPatch methods. ``RB_ID=862200``

Closed
~~~~~~

2.2.0
-----

Added
~~~~~

* finatra-thrift: Add python namespace to
  finatra\_thrift\_exceptions.thrift. ``RB_ID=844668``
* finatra-http: Support ANY method in HTTP Controllers. Adds support
  for defining routes which will answer
  to "any" HTTP method. ``RB_ID=830429``

Changed
~~~~~~~

* finatra: Address lifecycle around com.twitter.inject.app.App#appMain.
* (BREAKING CHANGE) EmbeddedApp has been completely re-written to be a
  better utility for testing command-line applications,
  as a result there are transparent changes to EmbeddedTwitterServer.
* com.twitter.inject.app.App#appMain is now
  com.twitter.inject.app.App#run and
  com.twitter.inject.server.TwitterServer#start.

   .. rubric:: run() is used for "running" applications and #start() is
      used for "starting" servers. In the lifecycle TwitterServer
      implements
      :name: run-is-used-for-running-applications-and-start-is-used-for-starting-servers.-in-the-lifecycle-twitterserver-implements

  App#run() as final and simply delegates to the start() method.
* Server await callback for adding server Awaitables to a list so that
  the server will now Await.all on all collected
  Awaitables.
* Added a new TwitterModuleLifecycle method:
  singletonPostWarmupComplete.
* More documentation around server and app Lifecycle methods, their
  intended usages, and usages of callback functions.\ ``RB_ID=844303``
* finatra: Narrow visibility on classes/objects in internal packages.
  Classes/objects in internal packages are not
  intended for use outside of the framework. ``RB_ID=845278``
* finatra-http: fix HttpHeaders's Date locale problem. ``RB_ID=843966``
* inject-thrift: Address issues with
  com.twitter.inject.exceptions.PossiblyRetryable. PossiblyRetryable
  does not correctly
  determine what is retryable. Updated to correct the logic for better
  default retry utility. ``RB_ID=843428``
* finatra: finatra: Move com.twitter.finatra.annotations.Flag\|FlagImpl
  to com.twitter.inject.annotations.Flag\|FlagImpl. ``RB_ID=843383``
* finatra: Remove
  com.twitter.inject.conversions.map#atomicGetOrElseUpdate. This was
  necessary for Scala 2.10 support
  since #getOrElseUpdate was not atomic until Scala 2.11.6. See:
  https://github.com/scala/scala/pull/4319. ``RB_ID=842684``
* finatra: Upgrade to Jackson 2.6.5. ``RB_ID=836819``
* inject: Introduce inject/inject-thrift module to undo cyclic
  dependency introduced in RB 839427. ``RB_ID=841128``
* inject-thrift-client: Improvements to FilteredThriftClientModule to
  provide finer-grain insight on ThriftClientExceptions.
  NOTE: previously per-route failure stats were in the form:
  route/add1String/GET/status/503/handled/ThriftClientException/Adder/add1String/com.twitter.finatra.thrift.thriftscala.ServerError

These will now split across per-route and detailed "service component"
failure stats, e.g.,

| // per-route
| route/add1String/GET/failure/adder-thrift/Adder/add1String/com.twitter.finatra.thrift.thriftscala.ServerError
| route/add1String/GET/status/503/mapped/ThriftClientException
| // service component
| service/failure/adder-thrift/Adder/add1String/com.twitter.finatra.thrift.thriftscala.ServerError

| Where the latter is in the form
  "service/failure/SOURCE/THRIFT\_SERVICE\_NAME/THRIFT\_METHOD/NAME/details".
| "SOURCE" is by default the thrift client label, however, users are
  able to map this to something else.\ ``RB_ID=839427``

*  finatra: Renamed Embedded testing utilities constructor args,
   clientFlags --> flags and extraArgs --> args. ``RB_ID=839537``
*  finatra-http: Set Content-Length correctly in EmbeddedHttpServer, to
   support multi-byte characters
   in the request body. ``RB_ID=837438``
*  finatra-http: No longer special-case NoSuchMethodException in the
   ExceptionMappingFilter. ``RB_ID=837369``
*  finatra-http: Remove deprecated package objects in
   com.twitter.finatra. Callers should be using code in
   the com.twitter.finatra.http package. ``RB_ID=836194``
*  finatra-http: Removed deprecated ExceptionBarrierFilter. NOTE: The
   ExceptionBarrierFilter produced stats in the form:
   "server/response/status/RESPONSE\_CODE". Using the replacement
   StatsFilter (in combination with the
   ExceptionMappingFilter) will produce more granular per-route stats.
   The comparable stats from the StatsFilter will be
   in the form: "route/ROUTE\_URI/HTTP\_METHOD/status/RESPONSE\_CODE"
   with an additional aggregated total
   stat. ``RB_ID=836073`` E.g,
   server/response/status/200: 5,
   server/response/status/201: 5,
   server/response/status/202: 5,
   server/response/status/403: 5,

| will now be:
| route/bar\_uri/GET/status/200: 5,
| route/bar\_uri/GET/status/2XX: 5,
| route/bar\_uri/GET/status/400: 5,
| route/bar\_uri/GET/status/401: 5,
| route/bar\_uri/GET/status/403: 5,
| route/bar\_uri/GET/status/4XX: 15,
| route/foo\_uri/POST/status/200: 5,
| route/foo\_uri/POST/status/2XX: 5,
| route/foo\_uri/POST/status/400: 5,
| route/foo\_uri/POST/status/401: 5,
| route/foo\_uri/POST/status/403: 5,
| route/foo\_uri/POST/status/4XX: 15,

*  finatra: Made implicit classes extend AnyVal for less runtime
   overhead. ``RB_ID=835972``
*  finatra-http: Remove deprecated package objects in
   com.twitter.finatra. Callers should be using code in
   the com.twitter.finatra.http package. ``RB_ID=836194``
*  finatra: Publish all artifacts under com.twitter organization.
   ``RB_ID=834484``
*  finatra: Update sbt memory settings. ``RB_ID=834571``
*  inject-server: Rename com.twitter.inject.server.TwitterServer#run to
   com.twitter.inject.server.TwitterServer#handle. ``RB_ID=833965``
*  finatra-http: Move test utilities in
   ``com.twitter.finatra.http.test.*`` to
   ``com.twitter.finatra.http.*``. ``RB_ID=833170``
*  finatra: Update SLF4J to version 1.7.21 and Logback to 1.1.7. Also
   update example
   logging configurations for best practices. ``RB_ID=832633``
*  Builds are now only for Java 8 and Scala 2.11. See the
   ``blog post <https://finagle.github.io/blog/2016/04/20/scala-210-and-java7/>``\ \_
   for details. ``RB_ID=828898``

Fixed
~~~~~

*  finatra-examples: Add sbt-revolver to the hello-world example. Fixes
   `GH-209 <https://github.com/twitter/finatra/issues/209>`__.
   ``RB_ID=838215``
*  finatra: Fix to properly support Java controllers that return Futures
   in their route callbacks. ``RB_ID=834467``

Closed
~~~~~~

*  `GH-276 <https://github.com/twitter/finatra/issues/276>`__.
   ``RB_ID=836819``
*  `PR-273 <https://github.com/twitter/finatra/pull/273>`__.
   ``RB_ID=838215``
*  `PR-324 <https://github.com/twitter/finatra/pull/324>`__.
   ``RB_ID=838215``

2.1.6
-----

`Full
Changelog <https://github.com/twitter/finatra/compare/v2.1.5...finatra-2.1.6>`__

Added
~~~~~

*  finatra-thrift: Add ThriftWarmup for thrift servers. ``RB_ID=820771``
*  finatra-inject/inject-server: Register framework in Library registry.
   ``RB_ID=809458``
*  finatra-http: Support for trace, connect & options in RequestBuilder.
   ``RB_ID=811102``
*  finatra-thrift: Allow for thrift server configuration.
   ``RB_ID=811126``

Changed
~~~~~~~

*  finatra/twitter-server: Update to register TwitterServer as library
   in /admin/registry.json. ``RB_ID=825129``
*  finatra-inject/inject-server: Deprecate PromoteToOldGenUtils in favor
   of twitter-server's prebindWarmup event. ``RB_ID=819411``
*  finatra-http: Move HttpServer to new Http stack API. ``RB_ID=812718``

Fixed
~~~~~

*  finatra: Revert sbt-scoverage plugin to 1.2.0. ``RB_ID=812098``
*  finatra-http: Ensure headers are set correctly in requests and
   responses. ``RB_ID=813969``

Closed
~~~~~~

`v2.1.5 <https://github.com/twitter/finatra/tree/v2.1.5>`__ (2016-03-15)
------------------------------------------------------------------------

`Full
Changelog <https://github.com/twitter/finatra/compare/v2.1.4...v2.1.5>`__

Added
~~~~~

*  finatra-http: Ability to access the finagle request in the
   ResponseBuilder
   for templating. ``RB_ID=805317``
*  finatra-http: Added ability to register routes into the TwitterServer
   admin UI. ``RB_ID=808272``
*  finatra: Added PULL\_REQUEST\_TEMPLATE ``RB_ID=808946``

Changed
~~~~~~~

*  finatra: Move to ``develop`` branch as default branch for Github.
   ``RB_ID=810088``
*  finatra: Updated test jars to **only** contain test utility
   code. ``RB_ID=809803``

Fixed
~~~~~

*  finatra-http; finatra-thrift: Slf4JBridgeModule is added by default
   and no
   longer breaks services which use the slf4k-jdk14 logging
   implementation. ``RB_ID=807171``
*  finatra-http: Fixed incorrect (or missing) content-type on some http
   responses. ``RB_ID=807773``
*  finatra-jackson: Fix to support doubles/floats in the jackson
   Min/Max/Range
   validations. ``RB_ID=809821``

`v2.1.4 <https://github.com/twitter/finatra/tree/v2.1.4>`__ (2016-02-25)
------------------------------------------------------------------------

`Full
Changelog <https://github.com/twitter/finatra/compare/v2.1.3...v2.1.4>`__

Fixed
~~~~~

*  Some Scaladoc links are broken on twitter.github.io/finatra `Github
   Issue 298 <https://github.com/twitter/finatra/issues/298>`__

Closed
~~~~~~

*  LoggingMDCFilter lacks documentation `Github Issue
   303 <https://github.com/twitter/finatra/issues/303>`__

*  bug in finatra/examples/hello-world/src/main/resources/logback.xml
   `Github Issue 289 <https://github.com/twitter/finatra/issues/289>`__

*  Improve error message when @Header field is missing `Github Issue
   263 <https://github.com/twitter/finatra/issues/263>`__

`v2.1.3 <https://github.com/twitter/finatra/tree/v2.1.3>`__ (2016-02-05)
------------------------------------------------------------------------

`Full
Changelog <https://github.com/twitter/finatra/compare/v2.1.2...v2.1.3>`__

Closed
~~~~~~

*  Is it possible to have different modules listen in different ports?
   `Github Issue 295 <https://github.com/twitter/finatra/issues/295>`__

*  Asynchronous method validations `Github Issue
   292 <https://github.com/twitter/finatra/issues/292>`__

*  if the Cookie contain version='' ,can't get the request.cookies
   `Github Issue 290 <https://github.com/twitter/finatra/issues/290>`__

*  Failed to auto configure default logger context `Github Issue
   288 <https://github.com/twitter/finatra/issues/288>`__

*  Inject properties `Github Issue
   287 <https://github.com/twitter/finatra/issues/287>`__

*  sbt compile error on master `Github Issue
   284 <https://github.com/twitter/finatra/issues/284>`__

*  Optionally announce server location on startup `Github Issue
   241 <https://github.com/twitter/finatra/issues/241>`__

`v2.1.2 <https://github.com/twitter/finatra/tree/v2.1.2>`__ (2015-12-09)
------------------------------------------------------------------------

`Full
Changelog <https://github.com/twitter/finatra/compare/v2.1.1...v2.1.2>`__

Fixed
~~~~~

*  Missing Scaladoc `Github Issue
   279 <https://github.com/twitter/finatra/issues/279>`__

Closed
~~~~~~

*  Finatra + Protobuf `Github Issue
   277 <https://github.com/twitter/finatra/issues/277>`__

*  Simple hello-world example does not compiled `Github Issue
   274 <https://github.com/twitter/finatra/issues/274>`__

*  Allow overriding of the http service name `Github Issue
   270 <https://github.com/twitter/finatra/issues/270>`__

*  Bump to latest finagle? `Github Issue
   266 <https://github.com/twitter/finatra/issues/266>`__

*  ClassCastException: com.twitter.inject.logging.FinagleMDCAdapter
   cannot be cast to ch.qos.logback.classic.util.LogbackMDCAdapter
   `Github Issue 256 <https://github.com/twitter/finatra/issues/256>`__

`v2.1.1 <https://github.com/twitter/finatra/tree/v2.1.1>`__ (2015-10-29)
------------------------------------------------------------------------

`Full
Changelog <https://github.com/twitter/finatra/compare/v2.1.0...v2.1.1>`__

Closed
~~~~~~

*  Update Startup Test on doc `Github Issue
   261 <https://github.com/twitter/finatra/issues/261>`__

*  Error with simple test using httpPutJson `Github Issue
   257 <https://github.com/twitter/finatra/issues/257>`__

*  appfrog problem with admin server, I only can use one port `Github
   Issue 252 <https://github.com/twitter/finatra/issues/252>`__

*  Streaming content every X seconds `Github Issue
   250 <https://github.com/twitter/finatra/issues/250>`__

*  Mustache templates getting stripped `Github Issue
   112 <https://github.com/twitter/finatra/issues/112>`__

**Merged pull requests:**

*  Remove unneccesary files `Github Issue
   265 <https://github.com/twitter/finatra/pull/265>`__
   (`cacoco <https://github.com/cacoco>`__)

`v2.1.0 <https://github.com/twitter/finatra/tree/v2.1.0>`__ (2015-10-01)
------------------------------------------------------------------------

`Full
Changelog <https://github.com/twitter/finatra/compare/v2.0.1...v2.1.0>`__

**Merged pull requests:**

*  finatra/inject * Rename InjectUtils to more specific PoolUtils
   `Github Issue 258 <https://github.com/twitter/finatra/pull/258>`__
   (`cacoco <https://github.com/cacoco>`__)

`v2.0.1 <https://github.com/twitter/finatra/tree/v2.0.1>`__ (2015-09-21)
------------------------------------------------------------------------

`Full
Changelog <https://github.com/twitter/finatra/compare/v2.0.0...v2.0.1>`__

Closed
~~~~~~

*  Split code into packages/modules `Github Issue
   254 <https://github.com/twitter/finatra/issues/254>`__

*  Support for Scala Future's `Github Issue
   249 <https://github.com/twitter/finatra/issues/249>`__

*  Override TwitterModule in FeatureTest `Github Issue
   233 <https://github.com/twitter/finatra/issues/233>`__

**Merged pull requests:**

*  Update TweetsControllerIntegrationTest.scala `Github Issue
   251 <https://github.com/twitter/finatra/pull/251>`__
   (`scosenza <https://github.com/scosenza>`__)

*  Update Travis CI to build with java8 fix. `Github Issue
   244 <https://github.com/twitter/finatra/pull/244>`__
   (`cacoco <https://github.com/cacoco>`__)

`v2.0.0 <https://github.com/twitter/finatra/tree/v2.0.0>`__ (2015-09-09)
------------------------------------------------------------------------

`Full
Changelog <https://github.com/twitter/finatra/compare/v2.0.0.M2...v2.0.0>`__

Closed
~~~~~~

*  Singleton classes `Github Issue
   236 <https://github.com/twitter/finatra/issues/236>`__

*  com.twitter.finatra.utils.ResponseUtils for 2.0.0.M2 missing
   functions used in examples `Github Issue
   235 <https://github.com/twitter/finatra/issues/235>`__

*  Warmup example in README seems to be using non-existent features
   `Github Issue 234 <https://github.com/twitter/finatra/issues/234>`__

*  Unable to resolve finatra-slf4j artifact `Github Issue
   232 <https://github.com/twitter/finatra/issues/232>`__

*  Unable to resolve some of the dependencies `Github Issue
   231 <https://github.com/twitter/finatra/issues/231>`__

*  How to render static webpage in finatra2 `Github Issue
   230 <https://github.com/twitter/finatra/issues/230>`__

*  When running a FeatureTest a lot of data is dumped to stdout and
   stderr `Github Issue
   226 <https://github.com/twitter/finatra/issues/226>`__

*  Mapping a header by name to a case class requires additional metadata
   `Github Issue 225 <https://github.com/twitter/finatra/issues/225>`__

*  Missing scaladoc documentation `Github Issue
   221 <https://github.com/twitter/finatra/issues/221>`__

*  finatra-hello-world does not compile `Github Issue
   219 <https://github.com/twitter/finatra/issues/219>`__

*  Add tags for Finatra 1.6.0 and 1.5.4 `Github Issue
   216 <https://github.com/twitter/finatra/issues/216>`__

*  FeatureTest withJsonBody not working `Github Issue
   215 <https://github.com/twitter/finatra/issues/215>`__

*  Disable admin `Github Issue
   208 <https://github.com/twitter/finatra/issues/208>`__

*  Regexes in paths for route definitions `Github Issue
   197 <https://github.com/twitter/finatra/issues/197>`__

*  AppService doesn't support POST of JSON containing % and then &
   `Github Issue 173 <https://github.com/twitter/finatra/issues/173>`__

*  fatjar includes unexpected assets in the public directory `Github
   Issue 147 <https://github.com/twitter/finatra/issues/147>`__

*  allow subclassing of request `Github Issue
   116 <https://github.com/twitter/finatra/issues/116>`__

*  Builtin Compressor for static files `Github Issue
   113 <https://github.com/twitter/finatra/issues/113>`__

*  bring back controller prefixes `Github Issue
   104 <https://github.com/twitter/finatra/issues/104>`__

*  code coverage stats `Github Issue
   98 <https://github.com/twitter/finatra/issues/98>`__

*  Add Aurora/Mesos support `Github Issue
   94 <https://github.com/twitter/finatra/issues/94>`__

*  Simplify Cookie API with a CookieBuilder `Github Issue
   93 <https://github.com/twitter/finatra/issues/93>`__

*  implement a routes.txt in admin `Github Issue
   80 <https://github.com/twitter/finatra/issues/80>`__

*  support ETAGS and/or Cache-Control headers in file server `Github
   Issue 73 <https://github.com/twitter/finatra/issues/73>`__

*  asset pipeline filter `Github Issue
   62 <https://github.com/twitter/finatra/issues/62>`__

**Merged pull requests:**

*  Scosenza update readmes `Github Issue
   242 <https://github.com/twitter/finatra/pull/242>`__
   (`scosenza <https://github.com/scosenza>`__)

*  Update warmup docs `Github Issue
   238 <https://github.com/twitter/finatra/pull/238>`__
   (`scosenza <https://github.com/scosenza>`__)

*  Change Google Analytics tracking to use Twitter OSS account `Github
   Issue 217 <https://github.com/twitter/finatra/pull/217>`__
   (`travisbrown <https://github.com/travisbrown>`__)

`v2.0.0.M2 <https://github.com/twitter/finatra/tree/v2.0.0.M2>`__ (2015-06-12)
------------------------------------------------------------------------------

`Full
Changelog <https://github.com/twitter/finatra/compare/v2.0.0.M1...v2.0.0.M2>`__

Closed
~~~~~~

*  Issue with POST request `Github Issue
   214 <https://github.com/twitter/finatra/issues/214>`__

*  error running example with sbt run: overloaded method value settings
   with alternatives. `Github Issue
   207 <https://github.com/twitter/finatra/issues/207>`__

*  Was the 1.5.3 release retagged? `Github Issue
   206 <https://github.com/twitter/finatra/issues/206>`__

*  Finatra 1.5.3 and dependencies at Travis CI `Github Issue
   205 <https://github.com/twitter/finatra/issues/205>`__

*  Add an ADOPTERs.md `Github Issue
   204 <https://github.com/twitter/finatra/issues/204>`__

*  connect finagle filter to specific controller `Github Issue
   203 <https://github.com/twitter/finatra/issues/203>`__

*  Does Finatra support Scala 2.11? `Github Issue
   196 <https://github.com/twitter/finatra/issues/196>`__

*  Support multipart PUT requests `Github Issue
   194 <https://github.com/twitter/finatra/issues/194>`__

*  Content-type custom settings do not work when render json `Github
   Issue 191 <https://github.com/twitter/finatra/issues/191>`__

*  FlatSpecHelper dependency missing in finagle 1.6.0 `Github Issue
   189 <https://github.com/twitter/finatra/issues/189>`__

*  Allow other logging handlers `Github Issue
   187 <https://github.com/twitter/finatra/issues/187>`__

*  ErrorHandler used by ControllerCollection depends on order
   Controllers are added `Github Issue
   182 <https://github.com/twitter/finatra/issues/182>`__

*  Deployment for newly generated project does not work on heroku
   `Github Issue 180 <https://github.com/twitter/finatra/issues/180>`__

*  finatra doc typo `Github Issue
   174 <https://github.com/twitter/finatra/issues/174>`__

*  Admin interface is showing a blank page. `Github Issue
   171 <https://github.com/twitter/finatra/issues/171>`__

*  Update to scala 2.11.x `Github Issue
   159 <https://github.com/twitter/finatra/issues/159>`__

*  Missing static resources report 500 Internal Server Error `Github
   Issue 157 <https://github.com/twitter/finatra/issues/157>`__

*  flag values are not resolved until server starts `Github Issue
   148 <https://github.com/twitter/finatra/issues/148>`__

*  docs are wrong about default template path `Github Issue
   143 <https://github.com/twitter/finatra/issues/143>`__

*  Static files can\`t be found if finatra server starts at Windows
   `Github Issue 130 <https://github.com/twitter/finatra/issues/130>`__

*  Add support for parsing JSON request body `Github Issue
   129 <https://github.com/twitter/finatra/issues/129>`__

*  Add test for unicode content-length `Github Issue
   122 <https://github.com/twitter/finatra/issues/122>`__

*  Expose logger without having to include App and Logger traits in
   every class `Github Issue
   121 <https://github.com/twitter/finatra/issues/121>`__

*  Make View class generic `Github Issue
   118 <https://github.com/twitter/finatra/issues/118>`__

*  premain docs `Github Issue
   114 <https://github.com/twitter/finatra/issues/114>`__

*  allow registration of custom jackson modules `Github Issue
   110 <https://github.com/twitter/finatra/issues/110>`__

*  Add CONTRIBUTING.md `Github Issue
   109 <https://github.com/twitter/finatra/issues/109>`__

*  expose server ip at startup time `Github Issue
   108 <https://github.com/twitter/finatra/issues/108>`__

*  explore dynamic routing `Github Issue
   103 <https://github.com/twitter/finatra/issues/103>`__

*  implement rails-like "flash" `Github Issue
   100 <https://github.com/twitter/finatra/issues/100>`__

*  CSRF Support `Github Issue
   89 <https://github.com/twitter/finatra/issues/89>`__

*  Session support `Github Issue
   88 <https://github.com/twitter/finatra/issues/88>`__

*  Configurable Key/Value store `Github Issue
   87 <https://github.com/twitter/finatra/issues/87>`__

*  apache-like directory browser for files `Github Issue
   54 <https://github.com/twitter/finatra/issues/54>`__

*  benchmark suite with caliper `Github Issue
   45 <https://github.com/twitter/finatra/issues/45>`__

*  RequestAdapter does not support multiple values for query params
   `Github Issue 22 <https://github.com/twitter/finatra/issues/22>`__

**Merged pull requests:**

*  Update README.md `Github Issue
   202 <https://github.com/twitter/finatra/pull/202>`__
   (`scosenza <https://github.com/scosenza>`__)

`v2.0.0.M1 <https://github.com/twitter/finatra/tree/v2.0.0.M1>`__ (2015-04-30)
------------------------------------------------------------------------------

`Full
Changelog <https://github.com/twitter/finatra/compare/1.6.0...v2.0.0.M1>`__

Closed
~~~~~~

*  UNRESOLVED DEPENDENCIES `Github Issue
   199 <https://github.com/twitter/finatra/issues/199>`__

*  Changing port breaks embedded static file server `Github Issue
   192 <https://github.com/twitter/finatra/issues/192>`__

*  Finatra cannot be built when Finagle's version is greater than 6.13.0
   `Github Issue 153 <https://github.com/twitter/finatra/issues/153>`__

**Merged pull requests:**

*  2.0.0.M1 `Github Issue
   200 <https://github.com/twitter/finatra/pull/200>`__
   (`cacoco <https://github.com/cacoco>`__)

1.6.0
-----

`Full
Changelog <https://github.com/twitter/finatra/compare/1.5.4...1.6.0>`__

Closed
~~~~~~

*  Finatra 1.5.4 with finagle-stats 6.22.0 throws an exception `Github
   Issue 184 <https://github.com/twitter/finatra/issues/184>`__

*  Document unit testing controllers by using MockApp `Github Issue
   178 <https://github.com/twitter/finatra/issues/178>`__

*  maven.twttr.com not showing finatra `Github Issue
   175 <https://github.com/twitter/finatra/issues/175>`__

*  Finatra 1.5.4 java.lang.RuntimeException with Finagle 6.22.0 `Github
   Issue 172 <https://github.com/twitter/finatra/issues/172>`__

*  Error while pushing on Heroku `Github Issue
   170 <https://github.com/twitter/finatra/issues/170>`__

*  Finatra closes connection `Github Issue
   161 <https://github.com/twitter/finatra/issues/161>`__

*  Spec test doesn't populate multiParams `Github Issue
   155 <https://github.com/twitter/finatra/issues/155>`__

*  RequestAdapter fails to decode non-multipart POSTs `Github Issue
   154 <https://github.com/twitter/finatra/issues/154>`__

**Merged pull requests:**

*  FIX: issue Github Issue 182, let controller's error handler handle
   its own errors. `Github Issue
   188 <https://github.com/twitter/finatra/pull/188>`__
   (`plaflamme <https://github.com/plaflamme>`__)

*  Update to use new Travis CI infrastructure `Github Issue
   186 <https://github.com/twitter/finatra/pull/186>`__
   (`caniszczyk <https://github.com/caniszczyk>`__)

*  Refactor FinatraServer to allow custom tlsConfig `Github Issue
   183 <https://github.com/twitter/finatra/pull/183>`__
   (`bpfoster <https://github.com/bpfoster>`__)

*  Fix heroku deployments for template project `Github Issue
   181 <https://github.com/twitter/finatra/pull/181>`__
   (`tomjadams <https://github.com/tomjadams>`__)

*  remove dependency on scalatest `Github Issue
   179 <https://github.com/twitter/finatra/pull/179>`__
   (`c089 <https://github.com/c089>`__)

*  Update to twitter-server 1.8.0 and finagle 6.22.0 `Github Issue
   176 <https://github.com/twitter/finatra/pull/176>`__
   (`bpfoster <https://github.com/bpfoster>`__)

*  Add an apache style directory browser `Github Issue
   169 <https://github.com/twitter/finatra/pull/169>`__
   (`leeavital <https://github.com/leeavital>`__)

*  MultipartParsing should only be called for POST requests that are
   multipart `Github Issue
   168 <https://github.com/twitter/finatra/pull/168>`__
   (`manjuraj <https://github.com/manjuraj>`__)

*  fixed resource resolution not loading from dependencies, and
   consistent ... `Github Issue
   167 <https://github.com/twitter/finatra/pull/167>`__
   (`tptodorov <https://github.com/tptodorov>`__)

*  Fix type error in sample code `Github Issue
   165 <https://github.com/twitter/finatra/pull/165>`__
   (`leeavital <https://github.com/leeavital>`__)

*  added builder from ChannelBuffer `Github Issue
   164 <https://github.com/twitter/finatra/pull/164>`__
   (`tptodorov <https://github.com/tptodorov>`__)

*  Do not log errors in the ErrorHandler `Github Issue
   163 <https://github.com/twitter/finatra/pull/163>`__
   (`eponvert <https://github.com/eponvert>`__)

*  Adding missing copyright headers to source files `Github Issue
   162 <https://github.com/twitter/finatra/pull/162>`__
   (`bdimmick <https://github.com/bdimmick>`__)

*  support use of templates from dependencies in development mode, by
   loadi... `Github Issue
   160 <https://github.com/twitter/finatra/pull/160>`__
   (`tptodorov <https://github.com/tptodorov>`__)

*  Update readme.md to reflect issues on installation `Github Issue
   152 <https://github.com/twitter/finatra/pull/152>`__
   (`comamitc <https://github.com/comamitc>`__)

*  Add code coverage support with coveralls `Github Issue
   151 <https://github.com/twitter/finatra/pull/151>`__
   (`caniszczyk <https://github.com/caniszczyk>`__)

*  Use HttpServerDispatcher to fix remoteAddress property of Request.
   `Github Issue 142 <https://github.com/twitter/finatra/pull/142>`__
   (`pixell <https://github.com/pixell>`__)

*  Don't add .mustache extension to template file name if it already has
   an extension `Github Issue
   138 <https://github.com/twitter/finatra/pull/138>`__
   (`jliszka <https://github.com/jliszka>`__)

*  Pass the filename of the template to the factory `Github Issue
   136 <https://github.com/twitter/finatra/pull/136>`__
   (`jliszka <https://github.com/jliszka>`__)

*  path definitions on routes `Github Issue
   131 <https://github.com/twitter/finatra/pull/131>`__
   (`grandbora <https://github.com/grandbora>`__)

*  ObjectMapper reuse & config `Github Issue
   126 <https://github.com/twitter/finatra/pull/126>`__
   (`Xorlev <https://github.com/Xorlev>`__)

1.5.4
-----

`Full
Changelog <https://github.com/twitter/finatra/compare/1.5.3...1.5.4>`__

Closed
~~~~~~

*  Could add support for Windows? `Github Issue
   145 <https://github.com/twitter/finatra/issues/145>`__

*  Sessions example `Github Issue
   134 <https://github.com/twitter/finatra/issues/134>`__

*  No main class detected. `Github Issue
   133 <https://github.com/twitter/finatra/issues/133>`__

*  Unresolved dependencies `Github Issue
   132 <https://github.com/twitter/finatra/issues/132>`__

**Merged pull requests:**

*  Bumped twitter-server to 1.6.1 `Github Issue
   150 <https://github.com/twitter/finatra/pull/150>`__
   (`pcalcado <https://github.com/pcalcado>`__)

*  modify FileService handle conditional GETs for static assets `Github
   Issue 144 <https://github.com/twitter/finatra/pull/144>`__
   (`tomcz <https://github.com/tomcz>`__)

*  remove duplicated ``organization`` config `Github Issue
   140 <https://github.com/twitter/finatra/pull/140>`__
   (`jalkoby <https://github.com/jalkoby>`__)

*  More render shortcuts `Github Issue
   139 <https://github.com/twitter/finatra/pull/139>`__
   (`grandbora <https://github.com/grandbora>`__)

*  mixing Router with Twitter App creates exitTimer thread per request
   `Github Issue 135 <https://github.com/twitter/finatra/pull/135>`__
   (`manjuraj <https://github.com/manjuraj>`__)

1.5.3
-----

`Full
Changelog <https://github.com/twitter/finatra/compare/1.5.2...1.5.3>`__

Closed
~~~~~~

*  Response body truncated `Github Issue
   120 <https://github.com/twitter/finatra/issues/120>`__

*  Add 2 methods in FinatraServer.scala for custom start stop Code
   `Github Issue 107 <https://github.com/twitter/finatra/issues/107>`__

**Merged pull requests:**

*  Adding shortcut methods to common http statuses `Github Issue
   128 <https://github.com/twitter/finatra/pull/128>`__
   (`grandbora <https://github.com/grandbora>`__)

*  maxRequestSize flag has no effect `Github Issue
   127 <https://github.com/twitter/finatra/pull/127>`__
   (`manjuraj <https://github.com/manjuraj>`__)

*  Add content-length: 0 for no content responses `Github Issue
   124 <https://github.com/twitter/finatra/pull/124>`__
   (`grandbora <https://github.com/grandbora>`__)

*  Updated SpecHelper to support a body for POST, PUT and OPTIONS
   methods `Github Issue
   123 <https://github.com/twitter/finatra/pull/123>`__
   (`mattweyant <https://github.com/mattweyant>`__)

*  Use bytes length for content-length instead of string length `Github
   Issue 117 <https://github.com/twitter/finatra/pull/117>`__
   (`beenokle <https://github.com/beenokle>`__)

*  Add helper for setting contentType `Github Issue
   115 <https://github.com/twitter/finatra/pull/115>`__
   (`murz <https://github.com/murz>`__)

1.5.2
-----

`Full
Changelog <https://github.com/twitter/finatra/compare/1.5.1...1.5.2>`__

Closed
~~~~~~

*  multipart/form-data regression `Github Issue
   101 <https://github.com/twitter/finatra/issues/101>`__

*  flight/bower and bootstrap built in `Github Issue
   63 <https://github.com/twitter/finatra/issues/63>`__

**Merged pull requests:**

*  upgrade mustache to 0.8.14 `Github Issue
   106 <https://github.com/twitter/finatra/pull/106>`__
   (`murz <https://github.com/murz>`__)

*  set Content-Length on static file responses `Github Issue
   102 <https://github.com/twitter/finatra/pull/102>`__
   (`zuercher <https://github.com/zuercher>`__)

*  Add support for Bower and use default bootstrap.css in new projects
   `Github Issue 99 <https://github.com/twitter/finatra/pull/99>`__
   (`armandocanals <https://github.com/armandocanals>`__)

1.5.1
-----

`Full
Changelog <https://github.com/twitter/finatra/compare/1.5.0a...1.5.1>`__

Closed
~~~~~~

*  1.7.x `Github Issue
   96 <https://github.com/twitter/finatra/issues/96>`__

*  Investigate automatic html escaping in mustache templating `Github
   Issue 91 <https://github.com/twitter/finatra/issues/91>`__

*  Missing share files? `Github Issue
   90 <https://github.com/twitter/finatra/issues/90>`__

*  Stats broken after twitter-server upgrade `Github Issue
   95 <https://github.com/twitter/finatra/issues/95>`__

*  Response tied to originating request `Github Issue
   86 <https://github.com/twitter/finatra/issues/86>`__

*  Test/Harden logging `Github Issue
   84 <https://github.com/twitter/finatra/issues/84>`__

*  LogLevel doesn't seem to work `Github Issue
   83 <https://github.com/twitter/finatra/issues/83>`__

*  enable full admin endpoints besides metrics.json `Github Issue
   74 <https://github.com/twitter/finatra/issues/74>`__

*  request.routeParams should be decoded `Github Issue
   68 <https://github.com/twitter/finatra/issues/68>`__

**Merged pull requests:**

*  Fix unicode rendering in json. Correct size of response is now set
   `Github Issue 97 <https://github.com/twitter/finatra/pull/97>`__
   (`yuzeh <https://github.com/yuzeh>`__)

*  enable HTML escaping in mustache templates `Github Issue
   92 <https://github.com/twitter/finatra/pull/92>`__
   (`zuercher <https://github.com/zuercher>`__)

1.5.0a
------

`Full
Changelog <https://github.com/twitter/finatra/compare/1.5.0...1.5.0a>`__

Closed
~~~~~~

*  0 deprecation/warnings `Github Issue
   17 <https://github.com/twitter/finatra/issues/17>`__

1.5.0
-----

`Full
Changelog <https://github.com/twitter/finatra/compare/finatra-1.4.1...1.5.0>`__

Closed
~~~~~~

*  filters for select routes only `Github Issue
   85 <https://github.com/twitter/finatra/issues/85>`__

*  using websockets `Github Issue
   81 <https://github.com/twitter/finatra/issues/81>`__

*  maven to sbt `Github Issue
   78 <https://github.com/twitter/finatra/issues/78>`__

*  support in release scripts for dual publishing scala 2.9 and 2.10
   `Github Issue 75 <https://github.com/twitter/finatra/issues/75>`__

*  PUT and PATCH command param issue `Github Issue
   71 <https://github.com/twitter/finatra/issues/71>`__

**Merged pull requests:**

*  Add Content-Length header as part of building the request. `Github
   Issue 82 <https://github.com/twitter/finatra/pull/82>`__
   (`BenWhitehead <https://github.com/BenWhitehead>`__)

*  FinatraServer should take the generic Filters, not SimpleFilters
   `Github Issue 76 <https://github.com/twitter/finatra/pull/76>`__
   (`pcalcado <https://github.com/pcalcado>`__)

1.4.1
-----

`Full
Changelog <https://github.com/twitter/finatra/compare/1.4.0...finatra-1.4.1>`__

Closed
~~~~~~

*  1.4.1 `Github Issue
   72 <https://github.com/twitter/finatra/issues/72>`__

*  Filter invoked 4 times per single request? `Github Issue
   69 <https://github.com/twitter/finatra/issues/69>`__

*  Filters not working `Github Issue
   66 <https://github.com/twitter/finatra/issues/66>`__

*  libthrift outdated `Github Issue
   65 <https://github.com/twitter/finatra/issues/65>`__

**Merged pull requests:**

*  Adding lazy service `Github Issue
   67 <https://github.com/twitter/finatra/pull/67>`__
   (`grandbora <https://github.com/grandbora>`__)

*  Fixed a bug with Inheritance using Mustache `Github Issue
   64 <https://github.com/twitter/finatra/pull/64>`__
   (`pranjaltech <https://github.com/pranjaltech>`__)

1.4.0
-----

`Full
Changelog <https://github.com/twitter/finatra/compare/finatra-1.4.0...1.4.0>`__

Closed
~~~~~~

*  port back apache's multiupload handler `Github Issue
   43 <https://github.com/twitter/finatra/issues/43>`__

*  move to com.twitter.common.metrics instead of ostrich.stats `Github
   Issue 42 <https://github.com/twitter/finatra/issues/42>`__

*  move to twitter-server once published `Github Issue
   41 <https://github.com/twitter/finatra/issues/41>`__

*  Add public/ dir in src/main/resources as new docroot `Github Issue
   39 <https://github.com/twitter/finatra/issues/39>`__

1.4.0
-----

`Full
Changelog <https://github.com/twitter/finatra/compare/1.3.9...finatra-1.4.0>`__

1.3.9
-----

`Full
Changelog <https://github.com/twitter/finatra/compare/finatra-1.3.9...1.3.9>`__

1.3.9
-----

`Full
Changelog <https://github.com/twitter/finatra/compare/1.3.8...finatra-1.3.9>`__

1.3.8
-----

`Full
Changelog <https://github.com/twitter/finatra/compare/finatra-1.3.8...1.3.8>`__

1.3.8
-----

`Full
Changelog <https://github.com/twitter/finatra/compare/1.3.7...finatra-1.3.8>`__

Closed
~~~~~~

*  Make mustache factory use baseTemplatePath local docroot and template
   path `Github Issue
   56 <https://github.com/twitter/finatra/issues/56>`__

**Merged pull requests:**

*  Concatenate local docroot and template path when forming
   mustacheFactory `Github Issue
   57 <https://github.com/twitter/finatra/pull/57>`__
   (`yuzeh <https://github.com/yuzeh>`__)

1.3.7
-----

`Full
Changelog <https://github.com/twitter/finatra/compare/finatra-1.3.7...1.3.7>`__

1.3.7
-----

`Full
Changelog <https://github.com/twitter/finatra/compare/finatra-1.3.4...finatra-1.3.7>`__

1.3.4
-----

`Full
Changelog <https://github.com/twitter/finatra/compare/finatra-1.3.3...finatra-1.3.4>`__

Closed
~~~~~~

*  handle param routing for static file handling `Github Issue
   55 <https://github.com/twitter/finatra/issues/55>`__

*  make redirects RFC compliant `Github Issue
   49 <https://github.com/twitter/finatra/issues/49>`__

*  Sending redirect require a body `Github Issue
   48 <https://github.com/twitter/finatra/issues/48>`__

*  support a "rails style" render.action to render arbitrary actions
   from any other action without a redirect `Github Issue
   44 <https://github.com/twitter/finatra/issues/44>`__

*  Startup / Shutdown hooks `Github Issue
   37 <https://github.com/twitter/finatra/issues/37>`__

**Merged pull requests:**

*  Support OPTIONS HTTP method `Github Issue
   53 <https://github.com/twitter/finatra/pull/53>`__
   (`theefer <https://github.com/theefer>`__)

*  Stying pass across the codebase. Fixing conventions. `Github Issue
   51 <https://github.com/twitter/finatra/pull/51>`__
   (`twoism <https://github.com/twoism>`__)

*  closes Github Issue 49 * make redirects match the RFC `Github Issue
   50 <https://github.com/twitter/finatra/pull/50>`__
   (`twoism <https://github.com/twoism>`__)

1.3.3
-----

`Full
Changelog <https://github.com/twitter/finatra/compare/finatra-1.3.2...finatra-1.3.3>`__

**Merged pull requests:**

*  fixed typing of jsonGenerator so it can be actually overridden
   `Github Issue 47 <https://github.com/twitter/finatra/pull/47>`__
   (`bmdhacks <https://github.com/bmdhacks>`__)

1.3.2
-----

`Full
Changelog <https://github.com/twitter/finatra/compare/finatra-1.3.1...finatra-1.3.2>`__

**Merged pull requests:**

*  allow json encoder to be overwritten `Github Issue
   46 <https://github.com/twitter/finatra/pull/46>`__
   (`bmdhacks <https://github.com/bmdhacks>`__)

*  shutdown the built server on shutdown `Github Issue
   40 <https://github.com/twitter/finatra/pull/40>`__
   (`sprsquish <https://github.com/sprsquish>`__)

1.3.1
-----

`Full
Changelog <https://github.com/twitter/finatra/compare/finatra-1.3.0...finatra-1.3.1>`__

Closed
~~~~~~

*  ./finatra update-readme no longer works `Github Issue
   34 <https://github.com/twitter/finatra/issues/34>`__

1.3.0
-----

`Full
Changelog <https://github.com/twitter/finatra/compare/finatra-1.2.2...finatra-1.3.0>`__

1.2.2
-----

`Full
Changelog <https://github.com/twitter/finatra/compare/finatra-1.2.0...finatra-1.2.2>`__

Closed
~~~~~~

*  ./finatra generator doesnt work on linux `Github Issue
   24 <https://github.com/twitter/finatra/issues/24>`__

**Merged pull requests:**

*  Handle downstream exceptions and display the error handler. `Github
   Issue 38 <https://github.com/twitter/finatra/pull/38>`__
   (`bmdhacks <https://github.com/bmdhacks>`__)

*  Force mustache partials to be uncached from the local filesystem in
   development mode. `Github Issue
   36 <https://github.com/twitter/finatra/pull/36>`__
   (`morria <https://github.com/morria>`__)

*  Fixing call to the request logger `Github Issue
   35 <https://github.com/twitter/finatra/pull/35>`__
   (`morria <https://github.com/morria>`__)

1.2.0
-----

`Full
Changelog <https://github.com/twitter/finatra/compare/finatra-1.1.1...finatra-1.2.0>`__

1.1.1
-----

`Full
Changelog <https://github.com/twitter/finatra/compare/finatra-1.1.0...finatra-1.1.1>`__

Closed
~~~~~~

*  Custom error handlers `Github Issue
   29 <https://github.com/twitter/finatra/issues/29>`__

**Merged pull requests:**

*  Fix Set-Cookier header bug in response `Github Issue
   31 <https://github.com/twitter/finatra/pull/31>`__
   (`hontent <https://github.com/hontent>`__)

1.1.0
-----

`Full
Changelog <https://github.com/twitter/finatra/compare/finatra-1.0.3...finatra-1.1.0>`__

Closed
~~~~~~

*  Publish to Maven Central `Github Issue
   23 <https://github.com/twitter/finatra/issues/23>`__

1.0.3
-----

`Full
Changelog <https://github.com/twitter/finatra/compare/finatra-1.0.2...finatra-1.0.3>`__

1.0.2
-----

`Full
Changelog <https://github.com/twitter/finatra/compare/finatra-1.0.1...finatra-1.0.2>`__

Closed
~~~~~~

*  Serve static files `Github Issue
   28 <https://github.com/twitter/finatra/issues/28>`__

1.0.1
-----

`Full
Changelog <https://github.com/twitter/finatra/compare/finatra-1.0.0...finatra-1.0.1>`__

Closed
~~~~~~

*  Unable to retrieve post parameters `Github Issue
   26 <https://github.com/twitter/finatra/issues/26>`__

**Merged pull requests:**

*  fix of post parameters `Github Issue
   27 <https://github.com/twitter/finatra/pull/27>`__
   (`mairbek <https://github.com/mairbek>`__)

*  Immutable instead of mutable map in tests `Github Issue
   25 <https://github.com/twitter/finatra/pull/25>`__
   (`mairbek <https://github.com/mairbek>`__)

1.0.0
-----

`Full
Changelog <https://github.com/twitter/finatra/compare/finatra-0.3.4...finatra-1.0.0>`__

Closed
~~~~~~

*  an config `Github Issue
   12 <https://github.com/twitter/finatra/issues/12>`__

0.3.4
-----

`Full
Changelog <https://github.com/twitter/finatra/compare/finatra-0.3.3...finatra-0.3.4>`__

Closed
~~~~~~

*  do a perf review `Github Issue
   13 <https://github.com/twitter/finatra/issues/13>`__

*  update docs `Github Issue
   8 <https://github.com/twitter/finatra/issues/8>`__

0.3.3
-----

`Full
Changelog <https://github.com/twitter/finatra/compare/finatra-0.3.2...finatra-0.3.3>`__

0.3.2
-----

`Full
Changelog <https://github.com/twitter/finatra/compare/finatra-0.2.4...finatra-0.3.2>`__

Closed
~~~~~~

*  allow insertion of userland filters into the finagle stack `Github
   Issue 15 <https://github.com/twitter/finatra/issues/15>`__

*  bubble up view/mustache errors `Github Issue
   14 <https://github.com/twitter/finatra/issues/14>`__

0.2.4
-----

`Full
Changelog <https://github.com/twitter/finatra/compare/finatra-0.2.3...finatra-0.2.4>`__

**Merged pull requests:**

*  Add Controller method callback timing `Github Issue
   21 <https://github.com/twitter/finatra/pull/21>`__
   (`franklinhu <https://github.com/franklinhu>`__)

0.2.3
-----

`Full
Changelog <https://github.com/twitter/finatra/compare/finatra-0.2.1...finatra-0.2.3>`__

**Merged pull requests:**

*  Pass controllers into AppService `Github Issue
   20 <https://github.com/twitter/finatra/pull/20>`__
   (`franklinhu <https://github.com/franklinhu>`__)

0.2.1
-----

`Full
Changelog <https://github.com/twitter/finatra/compare/finatra-0.2.0...finatra-0.2.1>`__

**Merged pull requests:**

*  Fix FinatraServer register for AbstractFinatraController type change
   `Github Issue 19 <https://github.com/twitter/finatra/pull/19>`__
   (`franklinhu <https://github.com/franklinhu>`__)

0.2.0
-----

`Full
Changelog <https://github.com/twitter/finatra/compare/finatra-0.1.10...finatra-0.2.0>`__

Closed
~~~~~~

*  regexed routes `Github Issue
   11 <https://github.com/twitter/finatra/issues/11>`__

*  PID management `Github Issue
   5 <https://github.com/twitter/finatra/issues/5>`__

**Merged pull requests:**

*  Add Travis CI status to README `Github Issue
   18 <https://github.com/twitter/finatra/pull/18>`__
   (`caniszczyk <https://github.com/caniszczyk>`__)

0.1.10
------

`Full
Changelog <https://github.com/twitter/finatra/compare/finatra-0.1.9...finatra-0.1.10>`__

0.1.9
-----

`Full
Changelog <https://github.com/twitter/finatra/compare/finatra-0.1.8...finatra-0.1.9>`__

0.1.8
-----

`Full
Changelog <https://github.com/twitter/finatra/compare/finatra-0.1.7...finatra-0.1.8>`__

Closed
~~~~~~

*  mvn package doesnt fully package `Github Issue
   16 <https://github.com/twitter/finatra/issues/16>`__

*  update gem `Github Issue
   7 <https://github.com/twitter/finatra/issues/7>`__

*  verify heroku uploads works `Github Issue
   6 <https://github.com/twitter/finatra/issues/6>`__

0.1.7
-----

`Full
Changelog <https://github.com/twitter/finatra/compare/finatra-0.1.6...finatra-0.1.7>`__

0.1.6
-----

`Full
Changelog <https://github.com/twitter/finatra/compare/finatra-0.1.5...finatra-0.1.6>`__

Closed
~~~~~~

*  unbreak file upload/form support `Github Issue
   10 <https://github.com/twitter/finatra/issues/10>`__

0.1.5
-----

`Full
Changelog <https://github.com/twitter/finatra/compare/finatra-0.1.3...finatra-0.1.5>`__

Closed
~~~~~~

*  add logging `Github Issue
   4 <https://github.com/twitter/finatra/issues/4>`__

0.1.3
-----

`Full
Changelog <https://github.com/twitter/finatra/compare/finatra-0.1.2...finatra-0.1.3>`__

0.1.2
-----

`Full
Changelog <https://github.com/twitter/finatra/compare/finatra-0.1.1...finatra-0.1.2>`__

Closed
~~~~~~

*  unbreak cookie support `Github Issue
   9 <https://github.com/twitter/finatra/issues/9>`__

0.1.1
-----

`Full
Changelog <https://github.com/twitter/finatra/compare/finatra-0.1.0...finatra-0.1.1>`__

0.1.0
-----

`Full
Changelog <https://github.com/twitter/finatra/compare/finatra-0.0.1...finatra-0.1.0>`__

0.0.1
-----

**Merged pull requests:**

*  Fix synchronization/correctness issues `Github Issue
   3 <https://github.com/twitter/finatra/pull/3>`__
   (`franklinhu <https://github.com/franklinhu>`__)

*  Fix HTTP response code for routes not found `Github Issue
   2 <https://github.com/twitter/finatra/pull/2>`__
   (`franklinhu <https://github.com/franklinhu>`__)

*  Fix template file resolving for packaged jarfiles `Github Issue
   1 <https://github.com/twitter/finatra/pull/1>`__
   (`franklinhu <https://github.com/franklinhu>`__)
