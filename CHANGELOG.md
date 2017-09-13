
# Change Log
All notable changes to this project will be documented in this file. Note that ``RB_ID=#`` and ``PHAB_ID=#`` correspond to associated message in commits.

## [Unreleased]

### Added

### Changed

* inject-utils: (BREAKING API CHANGE) RichOption toFutureOrFail, toTryOrFail, and 
  toFutureOrElse signature changed to take the fail or else parameter by name. 
  ``PHAB_ID=D89544``

* inject-server: Remove usage of deprecated `c.t.inject.logging.Slf4jBridgeUtility`.
  Change usages to `c.t.util.logging.Slf4jBridgeUtility`. ``PHAB_ID=D88095``

* finatra-http, inject-thrift-client: Remove netty3 specific types and dependency. 
  In finatra-http, the code using these types is deprecated and can be removed allowing 
  us to remove netty3-specific dependencies. In inject-thrift-client we can default to
  use the DefaultTimer for the backupRequestFilter method param instead of the 
  HashedWheelTimer. ``PHAB_ID=D88025``

* inject-utils: (BREAKING API CHANGE) RichOption toFutureOrFail, toTryOrFail, and toFutureOrElse signature
  changed to take the fail or else parameter by name.

### Fixed

### Closed

## [finatra-2.13.0](https://github.com/twitter/finatra/tree/finatra-2.13.0) (2017-09-06)

### Added

* inject-server: Add ability to fail embedded server startup on lint rule violation.
  There is now a flag in the embedded servers that when set to true will fail
  server startup if a lint rule violation is detected. This will then fail
  the running test. ``PHAB_ID=D82399``

### Changed

* finatra-http: No longer depend on bijection-util. ``PHAB_ID=D86640``

* finatra-jackson: Deprecate c.t.finatra.json.utils.CamelCasePropertyNamingStrategy.
  This object was created to reduce ambiguity with previous releases of Jackson in which
  the default PropertyNamingStrategy was an abstract class with a default of camel case.
  Users are encouraged to use the Jackson PropertyNamingStrategy 
  constants directly. ``PHAB_ID=D81707``

### Fixed

### Closed

## [finatra-2.12.0](https://github.com/twitter/finatra/tree/finatra-2.12.0) (2017-08-15)

### Added

* finatra-jackson: Add support for injecting a snake case FinatraObjectMapper by annotating
  parameters with a new @SnakeCaseMapper binding annotation. ``PHAB_ID=D7798``

### Changed

* finatra-http: Add close hook when constructing a StreamingResponse to allow for resource
  release without consuming an entire AsyncStream. ``PHAB_ID=D64013``

* finatra-http: Unmarshalling JSON no longer consumes the body of a HTTP Request.
  ``PHAB_ID=D74519``

* finatra-inject: RetryUtil.retry has been removed because it used a blocking call
  to Thread.sleep. Blocking Finagle threads results in poor performance and
  RetryUtil.retryFuture should be used instead. ``PHAB_ID=D73949``

### Fixed

### Closed

## [finatra-2.11.0](https://github.com/twitter/finatra/tree/finatra-2.11.0) (2017-06-06)

### Added

### Changed

### Fixed

* finatra-jackson: Fix JSON deserialization of scala.util.Either type in FinatraObjectMapper
  for Scala 2.12. ``RB_ID=917699``

### Closed

## [finatra-2.10.0](https://github.com/twitter/finatra/tree/finatra-2.10.0) (2017-04-20)

### Added

### Changed

* finatra-http: Increase composability and flexibility of RouteDSL. ``RB_ID=912095``

* inject-app: Run installed modules postInjectorStartup before server function. This makes
  reasoning about the server lifecycle a bit more straight-forward and simplifies things
  like the exception manager logic for adding and overridding mappers. ``RB_ID=911965``

* finatra-jackson: Update framework tests to FunSuite ScalaTest testing style. ``RB_ID=911745``

* finatra: Move finatra/benchmarks and finatra/utils framework tests to FunSuite ScalaTest
  testing style. ``RB_ID=910680``

### Fixed

* finatra-http: Correctly return a JsonParseException when the incoming JSON is not parsable
  as an expected custom case class request object. ``RB_ID=``

* finatra-http: Ensure underlying members are injected for AbstractControllers. ``RB_ID=911635``

* finatra-jackson: Patch `FinatraDatetimeDeserializer` to support parsing of Long value passed
  as String, e.g., when parsing a query parameter.``RB_ID=911162``

* finatra: Close embedded server clients on embedded server close. ``RB_ID=910862``

### Closed

## [finatra-2.9.0](https://github.com/twitter/finatra/tree/finatra-2.9.0) (2017-03-10)

### Added

### Changed

* inject-core: (BREAKING API CHANGE) Allow for binding of higher-kinded types when testing.
  Deprecated `@Bind` mechanism for replacing bound types in an object graph. Now instead of
  using `@Bind` like this:

  ```
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
   ```

   Users can instead do:

   ```
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
   ```

   This allows for more flexibility (as the binding is now per object graph, rather
   than per test files) and is less susceptible to errors due to incorrect usage.

   The breaking API change is due to adding this support in the TestInjector, it is
   now required that users call the `TestInjector#create` method in order to build
   the injector and that this is done *after* calls to `TestInjector#bind`. Previously,
   an `Injector` was directly returned from `TestInjector#apply` which is no longer true,
   thus it may look like your IntegrationTests are broken as you now need to add a
   call to `TestInjector#create`.

   Additionally, this change updates all of the framework tests in the inject modules to
   the FunSuite testing style from the deprecated WordSpec testing style. ``RB_ID=910011``

* finatra-thrift: Update framework tests to FunSuite ScalaTest testing style. ``RB_ID=910262``

* inject-core: Move Logging from grizzled-slf4j to util/util-slf4j-api.
  `c.t.inject.Logger` is now deprecated in favor of `c.t.util.logging.Logger`
  in util. ``PHAB_ID=D29713``

* finatra-httpclient: Update framework tests to FunSuite ScalaTest testing style. ``RB_ID=909526``

* finatra-http: Update framework tests to FunSuite ScalaTest testing style. ``RB_ID=909349``

* finatra: Bump guava to 19.0. ``RB_ID=907807``

* inject-thrift-client: Various APIs have changed to work with `ThriftMethod.SuccessType`
  instead of `ThriftMethod.Result`. See `ThriftClientFilterChain`, `Controller`,
  `ThriftWarmup`, `PossiblyRetryable`. ``RB_ID=908846``

### Fixed

* finatra-http: Correctly support adding Java AbstractController by instance. ``RB_ID=910502``

### Closed

## [finatra-2.8.0](https://github.com/twitter/finatra/tree/finatra-2.8.0) (2017-02-03)

### Added

* finatra-http: Add Java support for declaring admin routes. ``RB_ID=906264``

* finatra-http: Add AbstractExceptionMapper for ExceptionMapper usage from Java.
  Also update the HttpRouter to allow for registration of AbstractExceptionMappers.
  ``RB_ID=902995``

* finatra-http: Support for JSON Patch (https://tools.ietf.org/html/rfc6902). Utilities are
  located in package `com.twitter.finatra.http.jsonpatch`. ``RB_ID=889152``

* finatra: Created companion trait mixins for Test/FeatureTest/IntegrationTest/HttpTest.
  ``RB_ID=897778``

* finatra-http: Support for optional trailing slashes in HTTP routes. Routes can
  now specify that they allow an optional trailing slash by ending the route URI
  in the Controller with "/?". ``RB_ID=893167``

* finatra-http: Support for Controller route prefixes. This allows users to define a
  common prefix for a set of routes declaratively inside a controller. ``RB_ID=894695``

### Changed

* inject-core: Add back JUNitRUnner to `c.t.inject.Test` and `c.t.inject.WordSpecTest`
  so that tests can be run when building with maven. ``RB_ID=909789``

* finatra-http: Allow routes which begin with "/admin" to be exposed on the external
  interface and routes which DO NOT begin with "/admin" to be exposed on the admin interface.
  NOTE: routes which begin with "/admin/finatra" will continue to be on the admin interface
  only. Routes which begin with "/admin" that should be served from the admin interface MUST
  set the flag "admin = true" on the route in the Controller. ``RB_ID=905225``

* finatra: Move conversions and retry utilities from finatra/utils to finatra/inject/inject-utils.
  ``RB_ID=905109``

* finatra: (BREAKING API CHANGE) Rename the existing test helper classes to include
  their current opinionated testing style, "WordSpec". These are functionally
  equivalent as this is just a name change. We also introduce new versions of the
  test helpers which mix in the recommended FunSuite. Thus it will look like your
  tests are broken as you will need to update to change to use the new "WordSpec"
  classes or changed your testing style to the recommended `FunSuite` style.
  ``PHAB_ID=D19822``

* inject-core: Remove JUnitRunner from `c.t.inject.Test`. This was only necessary for
  internal building with pants and is no longer required. The sbt build uses the
  ScalaTest runner and is thus not affected. Additionally, update specs2 to 2.4.17 and
  to depend on just the `specs2-mock` dependency where needed. ``PHAB_ID=D18011``

### Fixed

* finatra-http: Fix issue where added admin routes did not have their HTTP method
  correctly specified leading to all routes being defaulted to 'GET'. ``RB_ID=905887``

* finatra-http: Fix for custom request case class collection-type fields which are
  annotated with either `@RouteParam`, `@QueryParam`, or `@FormParam` to correctly
  use a specified default value when a value is not sent in the request. ``RB_ID=903697``

* inject-app: Fix TestInjector to properly parse flags. The TestInjector didn't
  properly handle defaulted boolean flags when defined in Modules. Updated the
  TestInjector logic to properly parse flags. Fixes [Issue #373](https://github.com/twitter/finatra/issues/373)
  ``RB_ID=901525``

* finatra: Correctly filter published tests-javadocs and tests-sources jars for
  projects. We are incorrectly publishing tests in the sources and javadocs jars
  for projects which publish a test-jar dependency (http, httpclient, jackson,
  thrift, util, inject-app, inject-core, inject-modules, and inject-server).
  ``RB_ID=901153``

### Closed

## [finatra-2.7.0](https://github.com/twitter/finatra/tree/finatra-2.7.0) (2016-12-20)

### Added

* finatra-http: Add built-in support for Scala `scala.concurrent.Future`. The
  CallbackConverter now supports a return type of Scala `scala.concurrent.Future`
  by using a bijection to convert to a Twitter `c.t.util.Future`. ``RB_ID=898147``

* finatra-http: Support for request forwarding. Requests can be forwarded from
  one route to another. Forwarded requests will not go through the server's
  defined filter chain again but will pass through any Controller defined filters
  on the "forwarded to" route. ``RB_ID=883224``

### Changed

### Fixed

### Closed

## [finatra-2.6.0](https://github.com/twitter/finatra/tree/finatra-2.6.0) (2016-11-22)

### Added

* finatra: Move the OSS documentation to internal code repository to be co-located with
  source code. ``RB_ID=881112``

### Changed

* finatra-http: Decompose the `ThrowableExceptionMapper` to allow users to more easily replace
  the portions they care about. Users can now just replace the functionality per exception
  type rather than needing to replace the entire `ThrowableExceptionMapper`. `RB_ID=891666``

* finatra-http: The 'cookie' method of `c.t.finatra.http.response.ResponseBuilder#EnrichedResponse`
  that takes a Netty 3 cookie instance has been deprecated. Please use the method which takes a
  Finagle HTTP cookie instead. ``RB_ID=888683``

* finatra-http: Update adding routes to the TwitterServer HTTP Admin Interface to use
  `c.t.finagle.http.RouteIndex` and remove the `c.t.finatra.http.routing.AdminIndexInfo`.
  Also relaxed the rules for what routes can be added to the index to include constant
  /POST routes. Additionally, no longer fail if you define conflicting admin routes --
  we will now only warn. It is up to the user to not shoot themselves in the foot.
  ``RB_ID=889792``

* finatra-http: Request in request case classes no longer requires Inject annotation. ``RB_ID=888197``

* inject-utils: Deprecated RootMonitor since finagle DefaultMonitor is implicitly installed
  and handles all exceptions caught in stack. We provide a monitor method by default is a NullMonitor in
  `c.t.finatra.thrift.modules.DarkTrafficFilterModule` and `c.t.inject.thrift.modules.FilteredThriftClientModule`,
  users can handle other exceptions (unhandled by DefaultMonitor) by overriding the monitor method ``RB_ID=886773``

* finatra: We now depend on a fork of libthrift hosted in the Central Repository.
  The new package lives in the 'com.twitter' organization. This removes the necessity of
  depending on maven.twttr.com. This also means that eviction will not be automatic and
  using a newer libthrift library requires manual eviction if artifacts are being pulled
  in transitively. ``RB_ID=885879``

* inject-thrift-client: (BREAKING API CHANGE) Update filter building API with
  FilteredThriftClientModule. The `c.t.inject.thrift.filters.ThriftClientFilterChain`
  builder API has changed along with the underlying mechanisms to support
  enforcement of a "correct" filter order when using the helper methods. Methods
  have been renamed to a 'with'-syntax to be more inline with other builders and
  the confusing "globalFilter" method to the more verbose but more accurate
  "withAgnosticFilter". ``RB_ID=878260``
* inject-thrift-client: Remove deprecated package aliases. We'd like people to
  move the correct packages.``RB_ID=879330``

* finatra-http: (BREAKING API CHANGE) Update StreamingResponse to avoid keeping
  a reference to the head of the AsyncStream. This resolves the memory leak
  when streaming an infinite stream. The constructor is now private; use the
  StreamingResponse object methods that take an AsyncStream by-name instead.
  ``RB_ID=890205''

### Fixed

* finatra-http: Allow 0,1,t,f as valid boolean values for QueryParam case class requests.
  ``RB_ID=881939``

### Closed

## [finatra-2.5.0](https://github.com/twitter/finatra/tree/finatra-2.5.0) (2016-10-10)

### Added

* finatra-http: Add DarkTrafficFilterModule symmetric with
  thrift/DarkTrafficFilterModule. Add DarkTrafficService annotation in
  finatra-utils and a filter function used for requests annotated with
  Annotation Type in order to add DarkTrafficFilter. ``RB_ID=878079``

### Changed

* finatra: No longer need to add an additional resolver that points to
  maven.twttr.com. ``RB_ID=878967``
* inject-thrift-client: Stop counting response failures in the
  `c.t.inject.thrift.ThriftClientFilterChain` as these are now counted in the
  `c.t.finagle.thrift.ThriftServiceIface`. ``RB_ID=879075``
* finatra-jackson: Fix issue around JsonProperty annotation empty value. In
  CaseClassField.jsonNameForField, if the @JsonProperty annotation is used
  without a value, the property name is interpreted as "". It now follows the
  default Jackson behavior of using the name field name as the property
  name when the annotation is empty. ``RB_ID=877060``
* finatra: Correct instances of misspelled word "converter". There are
  several instances where the word "converter" is misspelled as "convertor".
  Specifically, TwitterModule.addTypeConvertor has been changed to
  TwitterModule.addTypeConverter. Other internal renamings are
  TwitterDurationTypeConverter, JodatimeDurationTypeConverter, and
  JacksonToGuiceTypeConverter. ``RB_ID=877736``
* finatra: Move installation of the SLF4JBridgeHandler to the constructor of
  `c.t.inject.server.TwitterServer`. The
  `c.t.finatra.logging.modules.Slf4jBridgeModule` has been removed as there is
  now little reason to use it unless you are building an application directly
  from `c.t.inject.app.App` since the functionality is now provided by default
  in the constructor of `c.t.inject.server.TwitterServer`. If using
  `c.t.inject.app.App`, then users can use the
  `c.t.inject.logging.modules.LoggerModule`. The main advantage is that slf4j
  bridges are now installed earlier in the application or server lifecycle and
  thus more of the initialization logging is bridged to the slf4j-api.
  ``RB_ID=870913``

### Fixed

* finatra-jackson: Test jar is missing files. Classes in the test
  `c.t.finatra.validation` package were not properly marked for
  inclusion in the finatra-jackson tests jar. They've now been added.
  ``RB_ID=878755``

### Closed

## [finatra-2.4.0](https://github.com/twitter/finatra/tree/finatra-2.4.0) (2016-09-07)

### Added

* finatra-thrift: Enhanced support for Java Thrift services. ``RB_ID=868254``
* finatra-examples: Add web/UI application example. ``RB_ID=868027``
* inject-server: Allow for the ability to disable test logging via System
  property. ``RB_ID=867344``

### Changed

* finatra-http: Simplify ExceptionMapper configuration and usage.
  We are dropping the need for a specialized DefaultExceptionMapper (which
  was simply an ExceptionMapper[Throwable]). Instead we now allow the
  configuration of mappers in the ExceptionManager to be much more flexible.
  Previously, the framework tried to prevent a user from registering a mapper
  over a given exception type multiple times and specialized a "default"
  ExceptionMapper to invoke on an exception type of Throwable. The
  ExceptionManager will now accept any mapper. If a mapper is added over a
  type already added, the previous mapper will be overwritten.

  The last registered mapper for an exception type wins.

  The framework adds three mappers to the manager by default. If a user wants
  to swap out any of these defaults they simply need add their own mapper to
  the manager for the exception type to map. E.g., by default the framework
  will add:
  Throwable ->
      com.twitter.finatra.http.internal.exceptions.ThrowableExceptionMapper
  JsonParseException ->
      com.twitter.finatra.http.internal.exceptions.json.JsonParseExceptionMapper
  CaseClassMappingException ->
      com.twitter.finatra.http.internal.exceptions.json.CaseClassExceptionMapper

  The manager walks the exception type hierarchy starting at the given
  exceptiontype and moving up the inheritence chain until it finds mapper
  configured for the type. In this manner an ExceptionMapper[Throwable] will
  be the last mapper invoked and performs as the "default".

  Thus, to change the "default" mapper, simply adding a new mapper over the
  Throwable type will suffice, i.e., ExceptionMapper[Throwable] to the
  ExceptionManager. There  are multiple ways to add a mapper. Either through
  the HttpRouter:


    override def configureHttp(router: HttpRouter): Unit = {
      router
        .exceptionMapper[MyDefaultExceptionMapper]
        ...
    }


  Or in a module which is then added to the Server, e.g.,


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


  This also means we can simplify the HttpServer as we no longer need to expose
  any "framework" module for overridding the default ExceptionMappers. So the
  "def exceptionMapperModule" has also been removed.``RB_ID=868614``
* finatra-http: Specify HTTP Java API consistently. ``RB_ID=868264``
* inject-core: Clean up inject.Logging trait. Remove dead code from Logging.
  ``RB_ID=868261``
* finatra-http: Move integration tests to a package under
  `com.twitter.finatra.http`. ``RB_ID=866487``

### Fixed

* finatra-http: Fix issue with unimplemented methods in
  NonValidatingHttpHeadersResponse. ``RB_ID=868480``

### Closed

## [finatra-2.3.0](https://github.com/twitter/finatra/tree/finatra-2.3.0) (2016-08-25)

### Added

* finatra-thrift: Add non-guice method to add controller to ThriftRouter ``RB_ID=863977``
* finatra-thrift: Add support for a "dark" traffic filter in thrift routing. Add a Finatra implementation
  of the Finagle AbstractDarkTrafficFilter which sub-classes ThriftFilter and will work in the Finatra
  filter chain. This will allow users to play incoming requests to a configured "dark" service. ``RB_ID=852338``

### Changed

* finatra-http: Performance improvements from latest micro-benchmarking run.
  - BREAKING API CHANGE: Removed `HttpHeaders#setDate`, `HttpHeaders#set` and `HttpHeaders#GMT`. ``RB_ID=865247``
* finatra-thrift: Provide access to statsReceiver argument in ThriftClientFilterBuilder. ``RB_ID=857286``

### Fixed

* finatra-http: Add content headers for EmbeddedHttpServer #httpDelete and #httpPatch methods. ``RB_ID=862200``

### Closed

## [finatra-2.2.0](https://github.com/twitter/finatra/tree/finatra-2.2.0) (2016-07-07)

### Added

* finatra-thrift: Add python namespace to finatra_thrift_exceptions.thrift. ``RB_ID=844668``
* finatra-http: Support ANY method in HTTP Controllers. Adds support for defining routes which will answer
  to "any" HTTP method. ``RB_ID=830429``

### Changed

* finatra: Address lifecycle around com.twitter.inject.app.App#appMain.
  - (BREAKING CHANGE) EmbeddedApp has been completely re-written to be a better utility for testing command-line applications,
    as a result there are transparent changes to EmbeddedTwitterServer.
  - com.twitter.inject.app.App#appMain is now com.twitter.inject.app.App#run and com.twitter.inject.server.TwitterServer#start.
    #run() is used for "running" applications and #start() is used for "starting" servers. In the lifecycle TwitterServer implements
    App#run() as final and simply delegates to the start() method.
  - Server await callback for adding server Awaitables to a list so that the server will now Await.all on all collected
    Awaitables.
  - Added a new TwitterModuleLifecycle method: singletonPostWarmupComplete.
  - More documentation around server and app Lifecycle methods, their intended usages, and usages of callback functions.``RB_ID=844303``
* finatra: Narrow visibility on classes/objects in internal packages. Classes/objects in internal packages are not
  intended for use outside of the framework. ``RB_ID=845278``
* finatra-http: fix HttpHeaders's Date locale problem. ``RB_ID=843966``
* inject-thrift: Address issues with com.twitter.inject.exceptions.PossiblyRetryable. PossiblyRetryable does not correctly
  determine what is retryable. Updated to correct the logic for better default retry utility. ``RB_ID=843428``
* finatra: finatra: Move com.twitter.finatra.annotations.Flag|FlagImpl to com.twitter.inject.annotations.Flag|FlagImpl. ``RB_ID=843383``
* finatra: Remove com.twitter.inject.conversions.map#atomicGetOrElseUpdate. This was necessary for Scala 2.10 support
  since #getOrElseUpdate was not atomic until Scala 2.11.6. See: https://github.com/scala/scala/pull/4319. ``RB_ID=842684``
* finatra: Upgrade to Jackson 2.6.5. ``RB_ID=836819``
* inject: Introduce inject/inject-thrift module to undo cyclic dependency introduced in RB 839427. ``RB_ID=841128``
* inject-thrift-client: Improvements to FilteredThriftClientModule to provide finer-grain insight on ThriftClientExceptions.
  NOTE: previously per-route failure stats were in the form:
  route/add1String/GET/status/503/handled/ThriftClientException/Adder/add1String/com.twitter.finatra.thrift.thriftscala.ServerError

  These will now split across per-route and detailed "service component" failure stats, e.g.,

  // per-route
  route/add1String/GET/failure/adder-thrift/Adder/add1String/com.twitter.finatra.thrift.thriftscala.ServerError
  route/add1String/GET/status/503/mapped/ThriftClientException
  // service component
  service/failure/adder-thrift/Adder/add1String/com.twitter.finatra.thrift.thriftscala.ServerError

  Where the latter is in the form "service/failure/SOURCE/THRIFT_SERVICE_NAME/THRIFT_METHOD/NAME/details".
  "SOURCE" is by default the thrift client label, however, users are able to map this to something else.``RB_ID=839427``
* finatra: Renamed Embedded testing utilities constructor args, clientFlags --> flags and extraArgs --> args. ``RB_ID=839537``
* finatra-http: Set Content-Length correctly in EmbeddedHttpServer, to support multi-byte characters
  in the request body. ``RB_ID=837438``
* finatra-http: No longer special-case NoSuchMethodException in the ExceptionMappingFilter. ``RB_ID=837369``
* finatra-http: Remove deprecated package objects in com.twitter.finatra. Callers should be using code in
  the com.twitter.finatra.http package. ``RB_ID=836194``
* finatra-http: Removed deprecated ExceptionBarrierFilter. NOTE: The ExceptionBarrierFilter produced stats in the form:
  "server/response/status/RESPONSE_CODE". Using the replacement StatsFilter (in combination with the
  ExceptionMappingFilter) will produce more granular per-route stats. The comparable stats from the StatsFilter will be
  in the form: "route/ROUTE_URI/HTTP_METHOD/status/RESPONSE_CODE" with an additional aggregated total
  stat. ``RB_ID=836073`` E.g,
  server/response/status/200: 5,
  server/response/status/201: 5,
  server/response/status/202: 5,
  server/response/status/403: 5,

  will now be:
  route/bar_uri/GET/status/200: 5,
  route/bar_uri/GET/status/2XX: 5,
  route/bar_uri/GET/status/400: 5,
  route/bar_uri/GET/status/401: 5,
  route/bar_uri/GET/status/403: 5,
  route/bar_uri/GET/status/4XX: 15,
  route/foo_uri/POST/status/200: 5,
  route/foo_uri/POST/status/2XX: 5,
  route/foo_uri/POST/status/400: 5,
  route/foo_uri/POST/status/401: 5,
  route/foo_uri/POST/status/403: 5,
  route/foo_uri/POST/status/4XX: 15,
* finatra: Made implicit classes extend AnyVal for less runtime overhead. ``RB_ID=835972``
* finatra-http: Remove deprecated package objects in com.twitter.finatra. Callers should be using code in
  the com.twitter.finatra.http package. ``RB_ID=836194``
* finatra: Publish all artifacts under com.twitter organization. ``RB_ID=834484``
* finatra: Update sbt memory settings. ``RB_ID=834571``
* inject-server: Rename com.twitter.inject.server.TwitterServer#run to com.twitter.inject.server.TwitterServer#handle. ``RB_ID=833965``
* finatra-http: Move test utilities in `com.twitter.finatra.http.test.*` to `com.twitter.finatra.http.*`. ``RB_ID=833170``
* finatra: Update SLF4J to version 1.7.21 and Logback to 1.1.7. Also update example
  logging configurations for best practices. ``RB_ID=832633``
* Builds are now only for Java 8 and Scala 2.11. See the
  `blog post <https://finagle.github.io/blog/2016/04/20/scala-210-and-java7/>`_
  for details. ``RB_ID=828898``

### Fixed

* finatra-examples: Add sbt-revolver to the hello-world example. Fixes [GH-209](https://github.com/twitter/finatra/issues/209). ``RB_ID=838215``
* finatra: Fix to properly support Java controllers that return Futures in their route callbacks. ``RB_ID=834467``

### Closed

* [GH-276](https://github.com/twitter/finatra/issues/276). ``RB_ID=836819``
* [PR-273](https://github.com/twitter/finatra/pull/273). ``RB_ID=838215``
* [PR-324](https://github.com/twitter/finatra/pull/324). ``RB_ID=838215``

## [finatra-2.1.6](https://github.com/twitter/finatra/tree/finatra-2.1.6) (2016-04-26)

[Full Changelog](https://github.com/twitter/finatra/compare/v2.1.5...finatra-2.1.6)

### Added

* finatra-thrift: Add ThriftWarmup for thrift servers. ``RB_ID=820771``
* finatra-inject/inject-server: Register framework in Library registry. ``RB_ID=809458``
* finatra-http: Support for trace, connect & options in RequestBuilder. ``RB_ID=811102``
* finatra-thrift: Allow for thrift server configuration. ``RB_ID=811126``

### Changed

* finatra/twitter-server: Update to register TwitterServer as library in /admin/registry.json. ``RB_ID=825129``
* finatra-inject/inject-server: Deprecate PromoteToOldGenUtils in favor of twitter-server's prebindWarmup event. ``RB_ID=819411``
* finatra-http: Move HttpServer to new Http stack API. ``RB_ID=812718``

### Fixed

* finatra: Revert sbt-scoverage plugin to 1.2.0. ``RB_ID=812098``
* finatra-http: Ensure headers are set correctly in requests and responses. ``RB_ID=813969``

### Closed

## [v2.1.5](https://github.com/twitter/finatra/tree/v2.1.5) (2016-03-15)

[Full Changelog](https://github.com/twitter/finatra/compare/v2.1.4...v2.1.5)

### Added

* finatra-http: Ability to access the finagle request in the ResponseBuilder
  for templating. ``RB_ID=805317``
* finatra-http: Added ability to register routes into the TwitterServer
  admin UI. ``RB_ID=808272``
* finatra: Added PULL_REQUEST_TEMPLATE ``RB_ID=808946``

### Changed

* finatra: Move to `develop` branch as default branch for Github. ``RB_ID=810088``
* finatra: Updated test jars to **only** contain test utility
  code. ``RB_ID=809803``

### Fixed

* finatra-http; finatra-thrift: Slf4JBridgeModule is added by default and no
  longer breaks services which use the slf4k-jdk14 logging
  implementation. ``RB_ID=807171``
* finatra-http: Fixed incorrect (or missing) content-type on some http
  responses. ``RB_ID=807773``
* finatra-jackson: Fix to support doubles/floats in the jackson Min/Max/Range
  validations. ``RB_ID=809821``

## [v2.1.4](https://github.com/twitter/finatra/tree/v2.1.4) (2016-02-25)

[Full Changelog](https://github.com/twitter/finatra/compare/v2.1.3...v2.1.4)

## Fixed

- Some Scaladoc links are broken on twitter.github.io/finatra [Github Issue 298](https://github.com/twitter/finatra/issues/298)

## Closed

- LoggingMDCFilter lacks documentation [Github Issue 303](https://github.com/twitter/finatra/issues/303)

- bug in finatra/examples/hello-world/src/main/resources/logback.xml [Github Issue 289](https://github.com/twitter/finatra/issues/289)

- Improve error message when @Header field is missing [Github Issue 263](https://github.com/twitter/finatra/issues/263)

## [v2.1.3](https://github.com/twitter/finatra/tree/v2.1.3) (2016-02-05)

[Full Changelog](https://github.com/twitter/finatra/compare/v2.1.2...v2.1.3)

## Closed

- Is it possible to have different modules listen in different ports? [Github Issue 295](https://github.com/twitter/finatra/issues/295)

- Asynchronous method validations [Github Issue 292](https://github.com/twitter/finatra/issues/292)

- if the Cookie contain version='' ,can't get the request.cookies [Github Issue 290](https://github.com/twitter/finatra/issues/290)

- Failed to auto configure default logger context [Github Issue 288](https://github.com/twitter/finatra/issues/288)

- Inject properties [Github Issue 287](https://github.com/twitter/finatra/issues/287)

- sbt compile error on master [Github Issue 284](https://github.com/twitter/finatra/issues/284)

- Optionally announce server location on startup [Github Issue 241](https://github.com/twitter/finatra/issues/241)

## [v2.1.2](https://github.com/twitter/finatra/tree/v2.1.2) (2015-12-09)

[Full Changelog](https://github.com/twitter/finatra/compare/v2.1.1...v2.1.2)

## Fixed

- Missing Scaladoc [Github Issue 279](https://github.com/twitter/finatra/issues/279)

## Closed

- Finatra + Protobuf [Github Issue 277](https://github.com/twitter/finatra/issues/277)

- Simple hello-world example does not compiled [Github Issue 274](https://github.com/twitter/finatra/issues/274)

- Allow overriding of the http service name [Github Issue 270](https://github.com/twitter/finatra/issues/270)

- Bump to latest finagle? [Github Issue 266](https://github.com/twitter/finatra/issues/266)

- ClassCastException: com.twitter.inject.logging.FinagleMDCAdapter cannot be cast to ch.qos.logback.classic.util.LogbackMDCAdapter [Github Issue 256](https://github.com/twitter/finatra/issues/256)

## [v2.1.1](https://github.com/twitter/finatra/tree/v2.1.1) (2015-10-29)

[Full Changelog](https://github.com/twitter/finatra/compare/v2.1.0...v2.1.1)

## Closed

- Update Startup Test on doc [Github Issue 261](https://github.com/twitter/finatra/issues/261)

- Error with simple test using httpPutJson [Github Issue 257](https://github.com/twitter/finatra/issues/257)

- appfrog problem with admin server, I only can use one port [Github Issue 252](https://github.com/twitter/finatra/issues/252)

- Streaming content every X seconds [Github Issue 250](https://github.com/twitter/finatra/issues/250)

- Mustache templates getting stripped  [Github Issue 112](https://github.com/twitter/finatra/issues/112)

**Merged pull requests:**

- Remove unneccesary files [Github Issue 265](https://github.com/twitter/finatra/pull/265) ([cacoco](https://github.com/cacoco))

## [v2.1.0](https://github.com/twitter/finatra/tree/v2.1.0) (2015-10-01)

[Full Changelog](https://github.com/twitter/finatra/compare/v2.0.1...v2.1.0)

**Merged pull requests:**

- finatra/inject - Rename InjectUtils to more specific PoolUtils [Github Issue 258](https://github.com/twitter/finatra/pull/258) ([cacoco](https://github.com/cacoco))

## [v2.0.1](https://github.com/twitter/finatra/tree/v2.0.1) (2015-09-21)

[Full Changelog](https://github.com/twitter/finatra/compare/v2.0.0...v2.0.1)

## Closed

- Split code into packages/modules [Github Issue 254](https://github.com/twitter/finatra/issues/254)

- Support for Scala Future's [Github Issue 249](https://github.com/twitter/finatra/issues/249)

- Override TwitterModule in FeatureTest [Github Issue 233](https://github.com/twitter/finatra/issues/233)

**Merged pull requests:**

- Update TweetsControllerIntegrationTest.scala [Github Issue 251](https://github.com/twitter/finatra/pull/251) ([scosenza](https://github.com/scosenza))

- Update Travis CI to build with java8 fix. [Github Issue 244](https://github.com/twitter/finatra/pull/244) ([cacoco](https://github.com/cacoco))

## [v2.0.0](https://github.com/twitter/finatra/tree/v2.0.0) (2015-09-09)

[Full Changelog](https://github.com/twitter/finatra/compare/v2.0.0.M2...v2.0.0)

## Closed

- Singleton classes [Github Issue 236](https://github.com/twitter/finatra/issues/236)

- com.twitter.finatra.utils.ResponseUtils for 2.0.0.M2 missing functions used in examples [Github Issue 235](https://github.com/twitter/finatra/issues/235)

- Warmup example in README seems to be using non-existent features [Github Issue 234](https://github.com/twitter/finatra/issues/234)

- Unable to resolve finatra-slf4j artifact [Github Issue 232](https://github.com/twitter/finatra/issues/232)

- Unable to resolve some of the dependencies [Github Issue 231](https://github.com/twitter/finatra/issues/231)

- How to render static webpage in finatra2 [Github Issue 230](https://github.com/twitter/finatra/issues/230)

- When running a FeatureTest a lot of data is dumped to stdout and stderr [Github Issue 226](https://github.com/twitter/finatra/issues/226)

- Mapping a header by name to a case class requires additional metadata [Github Issue 225](https://github.com/twitter/finatra/issues/225)

- Missing scaladoc documentation [Github Issue 221](https://github.com/twitter/finatra/issues/221)

- finatra-hello-world does not compile [Github Issue 219](https://github.com/twitter/finatra/issues/219)

- Add tags for Finatra 1.6.0 and 1.5.4 [Github Issue 216](https://github.com/twitter/finatra/issues/216)

- FeatureTest withJsonBody not working [Github Issue 215](https://github.com/twitter/finatra/issues/215)

- Disable admin [Github Issue 208](https://github.com/twitter/finatra/issues/208)

- Regexes in paths for route definitions [Github Issue 197](https://github.com/twitter/finatra/issues/197)

- AppService doesn't support POST of JSON containing % and then & [Github Issue 173](https://github.com/twitter/finatra/issues/173)

- fatjar includes unexpected assets in the public directory [Github Issue 147](https://github.com/twitter/finatra/issues/147)

- allow subclassing of request [Github Issue 116](https://github.com/twitter/finatra/issues/116)

- Builtin Compressor for static files [Github Issue 113](https://github.com/twitter/finatra/issues/113)

- bring back controller prefixes [Github Issue 104](https://github.com/twitter/finatra/issues/104)

- code coverage stats [Github Issue 98](https://github.com/twitter/finatra/issues/98)

- Add Aurora/Mesos support [Github Issue 94](https://github.com/twitter/finatra/issues/94)

- Simplify Cookie API with a CookieBuilder [Github Issue 93](https://github.com/twitter/finatra/issues/93)

- implement a routes.txt in admin [Github Issue 80](https://github.com/twitter/finatra/issues/80)

- support ETAGS and/or Cache-Control headers in file server [Github Issue 73](https://github.com/twitter/finatra/issues/73)

- asset pipeline filter [Github Issue 62](https://github.com/twitter/finatra/issues/62)

**Merged pull requests:**

- Scosenza update readmes [Github Issue 242](https://github.com/twitter/finatra/pull/242) ([scosenza](https://github.com/scosenza))

- Update warmup docs [Github Issue 238](https://github.com/twitter/finatra/pull/238) ([scosenza](https://github.com/scosenza))

- Change Google Analytics tracking to use Twitter OSS account [Github Issue 217](https://github.com/twitter/finatra/pull/217) ([travisbrown](https://github.com/travisbrown))

## [v2.0.0.M2](https://github.com/twitter/finatra/tree/v2.0.0.M2) (2015-06-12)

[Full Changelog](https://github.com/twitter/finatra/compare/v2.0.0.M1...v2.0.0.M2)

## Closed

- Issue with POST request [Github Issue 214](https://github.com/twitter/finatra/issues/214)

- error running example with sbt run: overloaded method value settings with alternatives. [Github Issue 207](https://github.com/twitter/finatra/issues/207)

- Was the 1.5.3 release retagged? [Github Issue 206](https://github.com/twitter/finatra/issues/206)

- Finatra 1.5.3 and dependencies at Travis CI [Github Issue 205](https://github.com/twitter/finatra/issues/205)

- Add an ADOPTERs.md [Github Issue 204](https://github.com/twitter/finatra/issues/204)

- connect finagle filter to specific controller [Github Issue 203](https://github.com/twitter/finatra/issues/203)

- Does Finatra support Scala 2.11? [Github Issue 196](https://github.com/twitter/finatra/issues/196)

- Support multipart PUT requests [Github Issue 194](https://github.com/twitter/finatra/issues/194)

-  Content-type custom settings do not work when render json [Github Issue 191](https://github.com/twitter/finatra/issues/191)

- FlatSpecHelper dependency missing in finagle 1.6.0 [Github Issue 189](https://github.com/twitter/finatra/issues/189)

- Allow other logging handlers [Github Issue 187](https://github.com/twitter/finatra/issues/187)

- ErrorHandler used by ControllerCollection depends on order Controllers are added [Github Issue 182](https://github.com/twitter/finatra/issues/182)

- Deployment for newly generated project does not work on heroku [Github Issue 180](https://github.com/twitter/finatra/issues/180)

- finatra doc typo [Github Issue 174](https://github.com/twitter/finatra/issues/174)

- Admin interface is showing a blank page. [Github Issue 171](https://github.com/twitter/finatra/issues/171)

- Update to scala 2.11.x [Github Issue 159](https://github.com/twitter/finatra/issues/159)

- Missing static resources report 500 Internal Server Error [Github Issue 157](https://github.com/twitter/finatra/issues/157)

- flag values are not resolved until server starts [Github Issue 148](https://github.com/twitter/finatra/issues/148)

- docs are wrong about default template path [Github Issue 143](https://github.com/twitter/finatra/issues/143)

- Static files can`t be found if finatra server starts at Windows [Github Issue 130](https://github.com/twitter/finatra/issues/130)

- Add support for parsing JSON request body [Github Issue 129](https://github.com/twitter/finatra/issues/129)

- Add test for unicode content-length [Github Issue 122](https://github.com/twitter/finatra/issues/122)

- Expose logger without having to include App and Logger traits in every class [Github Issue 121](https://github.com/twitter/finatra/issues/121)

- Make View class generic [Github Issue 118](https://github.com/twitter/finatra/issues/118)

- premain docs [Github Issue 114](https://github.com/twitter/finatra/issues/114)

- allow registration of custom jackson modules [Github Issue 110](https://github.com/twitter/finatra/issues/110)

- Add CONTRIBUTING.md [Github Issue 109](https://github.com/twitter/finatra/issues/109)

- expose server ip at startup time [Github Issue 108](https://github.com/twitter/finatra/issues/108)

- explore dynamic routing [Github Issue 103](https://github.com/twitter/finatra/issues/103)

- implement rails-like "flash" [Github Issue 100](https://github.com/twitter/finatra/issues/100)

- CSRF Support [Github Issue 89](https://github.com/twitter/finatra/issues/89)

- Session support [Github Issue 88](https://github.com/twitter/finatra/issues/88)

- Configurable Key/Value store [Github Issue 87](https://github.com/twitter/finatra/issues/87)

- apache-like directory browser for files [Github Issue 54](https://github.com/twitter/finatra/issues/54)

- benchmark suite with caliper [Github Issue 45](https://github.com/twitter/finatra/issues/45)

- RequestAdapter does not support multiple values for query params [Github Issue 22](https://github.com/twitter/finatra/issues/22)

**Merged pull requests:**

- Update README.md [Github Issue 202](https://github.com/twitter/finatra/pull/202) ([scosenza](https://github.com/scosenza))

## [v2.0.0.M1](https://github.com/twitter/finatra/tree/v2.0.0.M1) (2015-04-30)

[Full Changelog](https://github.com/twitter/finatra/compare/1.6.0...v2.0.0.M1)

## Closed

- UNRESOLVED DEPENDENCIES [Github Issue 199](https://github.com/twitter/finatra/issues/199)

- Changing port breaks embedded static file server [Github Issue 192](https://github.com/twitter/finatra/issues/192)

- Finatra cannot be built when Finagle's version is greater than 6.13.0 [Github Issue 153](https://github.com/twitter/finatra/issues/153)

**Merged pull requests:**

- 2.0.0.M1 [Github Issue 200](https://github.com/twitter/finatra/pull/200) ([cacoco](https://github.com/cacoco))

## [1.6.0](https://github.com/twitter/finatra/tree/1.6.0) (2015-01-08)

[Full Changelog](https://github.com/twitter/finatra/compare/1.5.4...1.6.0)

## Closed

- Finatra 1.5.4 with finagle-stats 6.22.0 throws an exception [Github Issue 184](https://github.com/twitter/finatra/issues/184)

- Document unit testing controllers by using MockApp [Github Issue 178](https://github.com/twitter/finatra/issues/178)

- maven.twttr.com not showing finatra [Github Issue 175](https://github.com/twitter/finatra/issues/175)

- Finatra 1.5.4 java.lang.RuntimeException with Finagle 6.22.0 [Github Issue 172](https://github.com/twitter/finatra/issues/172)

- Error while pushing on Heroku [Github Issue 170](https://github.com/twitter/finatra/issues/170)

- Finatra closes connection [Github Issue 161](https://github.com/twitter/finatra/issues/161)

- Spec test doesn't populate multiParams [Github Issue 155](https://github.com/twitter/finatra/issues/155)

- RequestAdapter fails to decode non-multipart POSTs [Github Issue 154](https://github.com/twitter/finatra/issues/154)

**Merged pull requests:**

- FIX: issue Github Issue 182, let controller's error handler handle its own errors. [Github Issue 188](https://github.com/twitter/finatra/pull/188) ([plaflamme](https://github.com/plaflamme))

- Update to use new Travis CI infrastructure [Github Issue 186](https://github.com/twitter/finatra/pull/186) ([caniszczyk](https://github.com/caniszczyk))

- Refactor FinatraServer to allow custom tlsConfig [Github Issue 183](https://github.com/twitter/finatra/pull/183) ([bpfoster](https://github.com/bpfoster))

- Fix heroku deployments for template project [Github Issue 181](https://github.com/twitter/finatra/pull/181) ([tomjadams](https://github.com/tomjadams))

- remove dependency on scalatest [Github Issue 179](https://github.com/twitter/finatra/pull/179) ([c089](https://github.com/c089))

- Update to twitter-server 1.8.0 and finagle 6.22.0 [Github Issue 176](https://github.com/twitter/finatra/pull/176) ([bpfoster](https://github.com/bpfoster))

- Add an apache style directory browser [Github Issue 169](https://github.com/twitter/finatra/pull/169) ([leeavital](https://github.com/leeavital))

- MultipartParsing should only be called for POST requests that are multipart [Github Issue 168](https://github.com/twitter/finatra/pull/168) ([manjuraj](https://github.com/manjuraj))

- fixed resource resolution not loading from dependencies, and consistent ... [Github Issue 167](https://github.com/twitter/finatra/pull/167) ([tptodorov](https://github.com/tptodorov))

- Fix type error in sample code [Github Issue 165](https://github.com/twitter/finatra/pull/165) ([leeavital](https://github.com/leeavital))

- added builder from ChannelBuffer  [Github Issue 164](https://github.com/twitter/finatra/pull/164) ([tptodorov](https://github.com/tptodorov))

- Do not log errors in the ErrorHandler [Github Issue 163](https://github.com/twitter/finatra/pull/163) ([eponvert](https://github.com/eponvert))

- Adding missing copyright headers to source files [Github Issue 162](https://github.com/twitter/finatra/pull/162) ([bdimmick](https://github.com/bdimmick))

- support use of templates from dependencies in development mode, by loadi... [Github Issue 160](https://github.com/twitter/finatra/pull/160) ([tptodorov](https://github.com/tptodorov))

- Update readme.md to reflect issues on installation [Github Issue 152](https://github.com/twitter/finatra/pull/152) ([comamitc](https://github.com/comamitc))

- Add code coverage support with coveralls [Github Issue 151](https://github.com/twitter/finatra/pull/151) ([caniszczyk](https://github.com/caniszczyk))

- Use HttpServerDispatcher to fix remoteAddress property of Request. [Github Issue 142](https://github.com/twitter/finatra/pull/142) ([pixell](https://github.com/pixell))

- Don't add .mustache extension to template file name if it already has an extension [Github Issue 138](https://github.com/twitter/finatra/pull/138) ([jliszka](https://github.com/jliszka))

- Pass the filename of the template to the factory [Github Issue 136](https://github.com/twitter/finatra/pull/136) ([jliszka](https://github.com/jliszka))

- path definitions on routes [Github Issue 131](https://github.com/twitter/finatra/pull/131) ([grandbora](https://github.com/grandbora))

- ObjectMapper reuse & config [Github Issue 126](https://github.com/twitter/finatra/pull/126) ([Xorlev](https://github.com/Xorlev))

## [1.5.4](https://github.com/twitter/finatra/tree/1.5.4) (2014-07-07)

[Full Changelog](https://github.com/twitter/finatra/compare/1.5.3...1.5.4)

## Closed

- Could add support for Windows? [Github Issue 145](https://github.com/twitter/finatra/issues/145)

- Sessions example [Github Issue 134](https://github.com/twitter/finatra/issues/134)

- No main class detected. [Github Issue 133](https://github.com/twitter/finatra/issues/133)

- Unresolved dependencies [Github Issue 132](https://github.com/twitter/finatra/issues/132)

**Merged pull requests:**

- Bumped twitter-server to 1.6.1 [Github Issue 150](https://github.com/twitter/finatra/pull/150) ([pcalcado](https://github.com/pcalcado))

- modify FileService handle conditional GETs for static assets [Github Issue 144](https://github.com/twitter/finatra/pull/144) ([tomcz](https://github.com/tomcz))

- remove duplicated `organization` config [Github Issue 140](https://github.com/twitter/finatra/pull/140) ([jalkoby](https://github.com/jalkoby))

- More render shortcuts [Github Issue 139](https://github.com/twitter/finatra/pull/139) ([grandbora](https://github.com/grandbora))

- mixing Router with Twitter App creates exitTimer thread per request [Github Issue 135](https://github.com/twitter/finatra/pull/135) ([manjuraj](https://github.com/manjuraj))

## [1.5.3](https://github.com/twitter/finatra/tree/1.5.3) (2014-04-16)

[Full Changelog](https://github.com/twitter/finatra/compare/1.5.2...1.5.3)

## Closed

- Response body truncated [Github Issue 120](https://github.com/twitter/finatra/issues/120)

- Add 2 methods in FinatraServer.scala for custom start stop Code [Github Issue 107](https://github.com/twitter/finatra/issues/107)

**Merged pull requests:**

- Adding shortcut methods to common http statuses [Github Issue 128](https://github.com/twitter/finatra/pull/128) ([grandbora](https://github.com/grandbora))

- maxRequestSize flag has no effect [Github Issue 127](https://github.com/twitter/finatra/pull/127) ([manjuraj](https://github.com/manjuraj))

- Add content-length: 0 for no content responses [Github Issue 124](https://github.com/twitter/finatra/pull/124) ([grandbora](https://github.com/grandbora))

- Updated SpecHelper to support a body for POST, PUT and OPTIONS methods [Github Issue 123](https://github.com/twitter/finatra/pull/123) ([mattweyant](https://github.com/mattweyant))

- Use bytes length for content-length instead of string length [Github Issue 117](https://github.com/twitter/finatra/pull/117) ([beenokle](https://github.com/beenokle))

- Add helper for setting contentType [Github Issue 115](https://github.com/twitter/finatra/pull/115) ([murz](https://github.com/murz))

## [1.5.2](https://github.com/twitter/finatra/tree/1.5.2) (2014-02-03)

[Full Changelog](https://github.com/twitter/finatra/compare/1.5.1...1.5.2)

## Closed

- multipart/form-data regression [Github Issue 101](https://github.com/twitter/finatra/issues/101)

- flight/bower and bootstrap built in [Github Issue 63](https://github.com/twitter/finatra/issues/63)

**Merged pull requests:**

- upgrade mustache to 0.8.14 [Github Issue 106](https://github.com/twitter/finatra/pull/106) ([murz](https://github.com/murz))

- set Content-Length on static file responses [Github Issue 102](https://github.com/twitter/finatra/pull/102) ([zuercher](https://github.com/zuercher))

- Add support for Bower and use default bootstrap.css in new projects [Github Issue 99](https://github.com/twitter/finatra/pull/99) ([armandocanals](https://github.com/armandocanals))

## [1.5.1](https://github.com/twitter/finatra/tree/1.5.1) (2014-01-13)

[Full Changelog](https://github.com/twitter/finatra/compare/1.5.0a...1.5.1)

## Closed

- 1.7.x [Github Issue 96](https://github.com/twitter/finatra/issues/96)

- Investigate automatic html escaping in mustache templating [Github Issue 91](https://github.com/twitter/finatra/issues/91)

- Missing share files? [Github Issue 90](https://github.com/twitter/finatra/issues/90)

- Stats broken after twitter-server upgrade [Github Issue 95](https://github.com/twitter/finatra/issues/95)

- Response tied to originating request [Github Issue 86](https://github.com/twitter/finatra/issues/86)

- Test/Harden logging [Github Issue 84](https://github.com/twitter/finatra/issues/84)

- LogLevel doesn't seem to work [Github Issue 83](https://github.com/twitter/finatra/issues/83)

- enable full admin endpoints besides metrics.json [Github Issue 74](https://github.com/twitter/finatra/issues/74)

- request.routeParams should be decoded [Github Issue 68](https://github.com/twitter/finatra/issues/68)

**Merged pull requests:**

- Fix unicode rendering in json. Correct size of response is now set [Github Issue 97](https://github.com/twitter/finatra/pull/97) ([yuzeh](https://github.com/yuzeh))

- enable HTML escaping in mustache templates [Github Issue 92](https://github.com/twitter/finatra/pull/92) ([zuercher](https://github.com/zuercher))

## [1.5.0a](https://github.com/twitter/finatra/tree/1.5.0a) (2014-01-08)

[Full Changelog](https://github.com/twitter/finatra/compare/1.5.0...1.5.0a)

## Closed

- 0 deprecation/warnings [Github Issue 17](https://github.com/twitter/finatra/issues/17)

## [1.5.0](https://github.com/twitter/finatra/tree/1.5.0) (2014-01-07)

[Full Changelog](https://github.com/twitter/finatra/compare/finatra-1.4.1...1.5.0)

## Closed

- filters for select routes only [Github Issue 85](https://github.com/twitter/finatra/issues/85)

- using websockets [Github Issue 81](https://github.com/twitter/finatra/issues/81)

- maven to sbt [Github Issue 78](https://github.com/twitter/finatra/issues/78)

- support in release scripts for dual publishing scala 2.9 and 2.10 [Github Issue 75](https://github.com/twitter/finatra/issues/75)

- PUT and PATCH command param issue [Github Issue 71](https://github.com/twitter/finatra/issues/71)

**Merged pull requests:**

- Add Content-Length header as part of building the request. [Github Issue 82](https://github.com/twitter/finatra/pull/82) ([BenWhitehead](https://github.com/BenWhitehead))

- FinatraServer should take the generic Filters, not SimpleFilters [Github Issue 76](https://github.com/twitter/finatra/pull/76) ([pcalcado](https://github.com/pcalcado))

## [finatra-1.4.1](https://github.com/twitter/finatra/tree/finatra-1.4.1) (2013-11-13)

[Full Changelog](https://github.com/twitter/finatra/compare/1.4.0...finatra-1.4.1)

## Closed

- 1.4.1 [Github Issue 72](https://github.com/twitter/finatra/issues/72)

- Filter invoked 4 times per single request? [Github Issue 69](https://github.com/twitter/finatra/issues/69)

- Filters not working [Github Issue 66](https://github.com/twitter/finatra/issues/66)

- libthrift outdated [Github Issue 65](https://github.com/twitter/finatra/issues/65)

**Merged pull requests:**

- Adding lazy service [Github Issue 67](https://github.com/twitter/finatra/pull/67) ([grandbora](https://github.com/grandbora))

- Fixed a bug with Inheritance using Mustache [Github Issue 64](https://github.com/twitter/finatra/pull/64) ([pranjaltech](https://github.com/pranjaltech))

## [1.4.0](https://github.com/twitter/finatra/tree/1.4.0) (2013-10-14)

[Full Changelog](https://github.com/twitter/finatra/compare/finatra-1.4.0...1.4.0)

## Closed

- port back apache's multiupload handler [Github Issue 43](https://github.com/twitter/finatra/issues/43)

- move to com.twitter.common.metrics instead of ostrich.stats [Github Issue 42](https://github.com/twitter/finatra/issues/42)

- move to twitter-server once published [Github Issue 41](https://github.com/twitter/finatra/issues/41)

- Add public/ dir in src/main/resources as new docroot [Github Issue 39](https://github.com/twitter/finatra/issues/39)

## [finatra-1.4.0](https://github.com/twitter/finatra/tree/finatra-1.4.0) (2013-10-14)

[Full Changelog](https://github.com/twitter/finatra/compare/1.3.9...finatra-1.4.0)

## [1.3.9](https://github.com/twitter/finatra/tree/1.3.9) (2013-10-14)

[Full Changelog](https://github.com/twitter/finatra/compare/finatra-1.3.9...1.3.9)

## [finatra-1.3.9](https://github.com/twitter/finatra/tree/finatra-1.3.9) (2013-10-14)

[Full Changelog](https://github.com/twitter/finatra/compare/1.3.8...finatra-1.3.9)

## [1.3.8](https://github.com/twitter/finatra/tree/1.3.8) (2013-09-22)

[Full Changelog](https://github.com/twitter/finatra/compare/finatra-1.3.8...1.3.8)

## [finatra-1.3.8](https://github.com/twitter/finatra/tree/finatra-1.3.8) (2013-09-22)

[Full Changelog](https://github.com/twitter/finatra/compare/1.3.7...finatra-1.3.8)

## Closed

- Make mustache factory use baseTemplatePath local docroot and template path [Github Issue 56](https://github.com/twitter/finatra/issues/56)

**Merged pull requests:**

- Concatenate local docroot and template path when forming mustacheFactory [Github Issue 57](https://github.com/twitter/finatra/pull/57) ([yuzeh](https://github.com/yuzeh))

## [1.3.7](https://github.com/twitter/finatra/tree/1.3.7) (2013-07-20)

[Full Changelog](https://github.com/twitter/finatra/compare/finatra-1.3.7...1.3.7)

## [finatra-1.3.7](https://github.com/twitter/finatra/tree/finatra-1.3.7) (2013-07-20)

[Full Changelog](https://github.com/twitter/finatra/compare/finatra-1.3.4...finatra-1.3.7)

## [finatra-1.3.4](https://github.com/twitter/finatra/tree/finatra-1.3.4) (2013-07-20)

[Full Changelog](https://github.com/twitter/finatra/compare/finatra-1.3.3...finatra-1.3.4)

## Closed

- handle param routing for static file handling [Github Issue 55](https://github.com/twitter/finatra/issues/55)

- make redirects RFC compliant [Github Issue 49](https://github.com/twitter/finatra/issues/49)

- Sending redirect require a body [Github Issue 48](https://github.com/twitter/finatra/issues/48)

- support a "rails style" render.action to render arbitrary actions from any other action without a redirect [Github Issue 44](https://github.com/twitter/finatra/issues/44)

- Startup / Shutdown hooks [Github Issue 37](https://github.com/twitter/finatra/issues/37)

**Merged pull requests:**

- Support OPTIONS HTTP method [Github Issue 53](https://github.com/twitter/finatra/pull/53) ([theefer](https://github.com/theefer))

- Stying pass across the codebase. Fixing conventions. [Github Issue 51](https://github.com/twitter/finatra/pull/51) ([twoism](https://github.com/twoism))

- closes Github Issue 49 - make redirects match the RFC [Github Issue 50](https://github.com/twitter/finatra/pull/50) ([twoism](https://github.com/twoism))

## [finatra-1.3.3](https://github.com/twitter/finatra/tree/finatra-1.3.3) (2013-06-14)

[Full Changelog](https://github.com/twitter/finatra/compare/finatra-1.3.2...finatra-1.3.3)

**Merged pull requests:**

- fixed typing of jsonGenerator so it can be actually overridden [Github Issue 47](https://github.com/twitter/finatra/pull/47) ([bmdhacks](https://github.com/bmdhacks))

## [finatra-1.3.2](https://github.com/twitter/finatra/tree/finatra-1.3.2) (2013-06-13)

[Full Changelog](https://github.com/twitter/finatra/compare/finatra-1.3.1...finatra-1.3.2)

**Merged pull requests:**

- allow json encoder to be overwritten [Github Issue 46](https://github.com/twitter/finatra/pull/46) ([bmdhacks](https://github.com/bmdhacks))

- shutdown the built server on shutdown [Github Issue 40](https://github.com/twitter/finatra/pull/40) ([sprsquish](https://github.com/sprsquish))

## [finatra-1.3.1](https://github.com/twitter/finatra/tree/finatra-1.3.1) (2013-03-12)

[Full Changelog](https://github.com/twitter/finatra/compare/finatra-1.3.0...finatra-1.3.1)

## Closed

- ./finatra update-readme no longer works [Github Issue 34](https://github.com/twitter/finatra/issues/34)

## [finatra-1.3.0](https://github.com/twitter/finatra/tree/finatra-1.3.0) (2013-03-10)

[Full Changelog](https://github.com/twitter/finatra/compare/finatra-1.2.2...finatra-1.3.0)

## [finatra-1.2.2](https://github.com/twitter/finatra/tree/finatra-1.2.2) (2013-03-10)

[Full Changelog](https://github.com/twitter/finatra/compare/finatra-1.2.0...finatra-1.2.2)

## Closed

- ./finatra generator doesnt work on linux [Github Issue 24](https://github.com/twitter/finatra/issues/24)

**Merged pull requests:**

- Handle downstream exceptions and display the error handler. [Github Issue 38](https://github.com/twitter/finatra/pull/38) ([bmdhacks](https://github.com/bmdhacks))

- Force mustache partials to be uncached from the local filesystem in development mode. [Github Issue 36](https://github.com/twitter/finatra/pull/36) ([morria](https://github.com/morria))

- Fixing call to the request logger [Github Issue 35](https://github.com/twitter/finatra/pull/35) ([morria](https://github.com/morria))

## [finatra-1.2.0](https://github.com/twitter/finatra/tree/finatra-1.2.0) (2013-01-22)

[Full Changelog](https://github.com/twitter/finatra/compare/finatra-1.1.1...finatra-1.2.0)

## [finatra-1.1.1](https://github.com/twitter/finatra/tree/finatra-1.1.1) (2012-12-06)

[Full Changelog](https://github.com/twitter/finatra/compare/finatra-1.1.0...finatra-1.1.1)

## Closed

- Custom error handlers [Github Issue 29](https://github.com/twitter/finatra/issues/29)

**Merged pull requests:**

- Fix Set-Cookier header bug in response [Github Issue 31](https://github.com/twitter/finatra/pull/31) ([hontent](https://github.com/hontent))

## [finatra-1.1.0](https://github.com/twitter/finatra/tree/finatra-1.1.0) (2012-11-20)

[Full Changelog](https://github.com/twitter/finatra/compare/finatra-1.0.3...finatra-1.1.0)

## Closed

- Publish to Maven Central [Github Issue 23](https://github.com/twitter/finatra/issues/23)

## [finatra-1.0.3](https://github.com/twitter/finatra/tree/finatra-1.0.3) (2012-11-13)

[Full Changelog](https://github.com/twitter/finatra/compare/finatra-1.0.2...finatra-1.0.3)

## [finatra-1.0.2](https://github.com/twitter/finatra/tree/finatra-1.0.2) (2012-11-13)

[Full Changelog](https://github.com/twitter/finatra/compare/finatra-1.0.1...finatra-1.0.2)

## Closed

- Serve static files [Github Issue 28](https://github.com/twitter/finatra/issues/28)

## [finatra-1.0.1](https://github.com/twitter/finatra/tree/finatra-1.0.1) (2012-11-11)

[Full Changelog](https://github.com/twitter/finatra/compare/finatra-1.0.0...finatra-1.0.1)

## Closed

- Unable to retrieve post parameters [Github Issue 26](https://github.com/twitter/finatra/issues/26)

**Merged pull requests:**

- fix of post parameters [Github Issue 27](https://github.com/twitter/finatra/pull/27) ([mairbek](https://github.com/mairbek))

- Immutable instead of mutable map in tests [Github Issue 25](https://github.com/twitter/finatra/pull/25) ([mairbek](https://github.com/mairbek))

## [finatra-1.0.0](https://github.com/twitter/finatra/tree/finatra-1.0.0) (2012-11-08)

[Full Changelog](https://github.com/twitter/finatra/compare/finatra-0.3.4...finatra-1.0.0)

## Closed

- an config [Github Issue 12](https://github.com/twitter/finatra/issues/12)

## [finatra-0.3.4](https://github.com/twitter/finatra/tree/finatra-0.3.4) (2012-11-07)

[Full Changelog](https://github.com/twitter/finatra/compare/finatra-0.3.3...finatra-0.3.4)

## Closed

- do a perf review [Github Issue 13](https://github.com/twitter/finatra/issues/13)

- update docs [Github Issue 8](https://github.com/twitter/finatra/issues/8)

## [finatra-0.3.3](https://github.com/twitter/finatra/tree/finatra-0.3.3) (2012-11-05)

[Full Changelog](https://github.com/twitter/finatra/compare/finatra-0.3.2...finatra-0.3.3)

## [finatra-0.3.2](https://github.com/twitter/finatra/tree/finatra-0.3.2) (2012-11-04)

[Full Changelog](https://github.com/twitter/finatra/compare/finatra-0.2.4...finatra-0.3.2)

## Closed

- allow insertion of userland filters into the finagle stack [Github Issue 15](https://github.com/twitter/finatra/issues/15)

- bubble up view/mustache errors [Github Issue 14](https://github.com/twitter/finatra/issues/14)

## [finatra-0.2.4](https://github.com/twitter/finatra/tree/finatra-0.2.4) (2012-08-18)

[Full Changelog](https://github.com/twitter/finatra/compare/finatra-0.2.3...finatra-0.2.4)

**Merged pull requests:**

- Add Controller method callback timing [Github Issue 21](https://github.com/twitter/finatra/pull/21) ([franklinhu](https://github.com/franklinhu))

## [finatra-0.2.3](https://github.com/twitter/finatra/tree/finatra-0.2.3) (2012-08-08)

[Full Changelog](https://github.com/twitter/finatra/compare/finatra-0.2.1...finatra-0.2.3)

**Merged pull requests:**

- Pass controllers into AppService [Github Issue 20](https://github.com/twitter/finatra/pull/20) ([franklinhu](https://github.com/franklinhu))

## [finatra-0.2.1](https://github.com/twitter/finatra/tree/finatra-0.2.1) (2012-07-20)

[Full Changelog](https://github.com/twitter/finatra/compare/finatra-0.2.0...finatra-0.2.1)

**Merged pull requests:**

- Fix FinatraServer register for AbstractFinatraController type change [Github Issue 19](https://github.com/twitter/finatra/pull/19) ([franklinhu](https://github.com/franklinhu))

## [finatra-0.2.0](https://github.com/twitter/finatra/tree/finatra-0.2.0) (2012-07-20)

[Full Changelog](https://github.com/twitter/finatra/compare/finatra-0.1.10...finatra-0.2.0)

## Closed

- regexed routes [Github Issue 11](https://github.com/twitter/finatra/issues/11)

- PID management [Github Issue 5](https://github.com/twitter/finatra/issues/5)

**Merged pull requests:**

- Add Travis CI status to README [Github Issue 18](https://github.com/twitter/finatra/pull/18) ([caniszczyk](https://github.com/caniszczyk))

## [finatra-0.1.10](https://github.com/twitter/finatra/tree/finatra-0.1.10) (2012-07-14)

[Full Changelog](https://github.com/twitter/finatra/compare/finatra-0.1.9...finatra-0.1.10)

## [finatra-0.1.9](https://github.com/twitter/finatra/tree/finatra-0.1.9) (2012-07-14)

[Full Changelog](https://github.com/twitter/finatra/compare/finatra-0.1.8...finatra-0.1.9)

## [finatra-0.1.8](https://github.com/twitter/finatra/tree/finatra-0.1.8) (2012-07-14)

[Full Changelog](https://github.com/twitter/finatra/compare/finatra-0.1.7...finatra-0.1.8)

## Closed

- mvn package doesnt fully package [Github Issue 16](https://github.com/twitter/finatra/issues/16)

- update gem [Github Issue 7](https://github.com/twitter/finatra/issues/7)

- verify heroku uploads works [Github Issue 6](https://github.com/twitter/finatra/issues/6)

## [finatra-0.1.7](https://github.com/twitter/finatra/tree/finatra-0.1.7) (2012-07-13)

[Full Changelog](https://github.com/twitter/finatra/compare/finatra-0.1.6...finatra-0.1.7)

## [finatra-0.1.6](https://github.com/twitter/finatra/tree/finatra-0.1.6) (2012-07-13)

[Full Changelog](https://github.com/twitter/finatra/compare/finatra-0.1.5...finatra-0.1.6)

## Closed

- unbreak file upload/form support [Github Issue 10](https://github.com/twitter/finatra/issues/10)

## [finatra-0.1.5](https://github.com/twitter/finatra/tree/finatra-0.1.5) (2012-07-13)

[Full Changelog](https://github.com/twitter/finatra/compare/finatra-0.1.3...finatra-0.1.5)

## Closed

- add logging [Github Issue 4](https://github.com/twitter/finatra/issues/4)

## [finatra-0.1.3](https://github.com/twitter/finatra/tree/finatra-0.1.3) (2012-07-13)

[Full Changelog](https://github.com/twitter/finatra/compare/finatra-0.1.2...finatra-0.1.3)

## [finatra-0.1.2](https://github.com/twitter/finatra/tree/finatra-0.1.2) (2012-07-13)

[Full Changelog](https://github.com/twitter/finatra/compare/finatra-0.1.1...finatra-0.1.2)

## Closed

- unbreak cookie support [Github Issue 9](https://github.com/twitter/finatra/issues/9)

## [finatra-0.1.1](https://github.com/twitter/finatra/tree/finatra-0.1.1) (2012-07-13)

[Full Changelog](https://github.com/twitter/finatra/compare/finatra-0.1.0...finatra-0.1.1)

## [finatra-0.1.0](https://github.com/twitter/finatra/tree/finatra-0.1.0) (2012-07-12)

[Full Changelog](https://github.com/twitter/finatra/compare/finatra-0.0.1...finatra-0.1.0)

## [finatra-0.0.1](https://github.com/twitter/finatra/tree/finatra-0.0.1) (2012-07-12)

**Merged pull requests:**

- Fix synchronization/correctness issues [Github Issue 3](https://github.com/twitter/finatra/pull/3) ([franklinhu](https://github.com/franklinhu))

- Fix HTTP response code for routes not found [Github Issue 2](https://github.com/twitter/finatra/pull/2) ([franklinhu](https://github.com/franklinhu))

- Fix template file resolving for packaged jarfiles [Github Issue 1](https://github.com/twitter/finatra/pull/1) ([franklinhu](https://github.com/franklinhu))
