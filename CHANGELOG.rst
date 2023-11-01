.. Author notes: this file is formatted with restructured text
  (http://docutils.sourceforge.net/docs/user/rst/quickstart.html)
  as it is included in Finatra's documentation.

Note that ``RB_ID=#`` and ``PHAB_ID=#`` correspond to associated message in commits.

Unreleased
----------

23.11.0
-------


Runtime Behavior Changes
~~~~~~~~~~~~~~~~~~~~~~~~

* finatra: Bump version of Jackson to 2.14.2. ``PHAB_ID=D1049772``

* finatra: Bump version of nscala-time to 2.32.0. ``PHAB_ID=D1050087``

* finatra: Bump version of Guice to 5.1.0 and Joda-Convert to 2.2.3. ``PHAB_ID=D1050820``

* finatra: Bump version of Joda to 2.12.5 ``PHAB_ID=D1059830``

* finatra: Bump version of Jackson to 2.14.3. ``PHAB_ID=D1069160``

22.12.0
-------

Changed
~~~~~~~

* inject-app: (BREAKING CHANGE) ``EmbeddedApp`` now sets com.google.inject.Stage to ``DEVELOPMENT``
  ``PHAB_ID=D983261``

Runtime Behavior Changes
~~~~~~~~~~~~~~~~~~~~~~~~

* util: Bump version of logback to 1.2.11. ``PHAB_ID=D1026765``

22.7.0
------

Changed
~~~~~~~

* inject-utils|inject-thrift: Move package private methods `PossiblyRetryable#isCancellation` and
  `PossibleRetryable#isNonRetryable` in inject-thrift to inject-utils `ExceptionUtils` as publicly
  usable methods. These methods are generally useful when processing interrupts on Futures.
  ``PHAB_ID=D935287``

Runtime Behavior Changes
~~~~~~~~~~~~~~~~~~~~~~~~

* inject-server: Remove deprecated `c.t.inject.server.DeprecatedLogging` trait. This trait was
  introduced as a placeholder shim to ensure that JUL util-logging Flags were still defined within
  a Finatra server such that servers did not fail upon startup if Flag values were being set.
  The default behavior during Flag parsing is to error if a Flag value is passed for a Flag not
  defined within the application.  

  We have removed the shim and the trait (and thus the Flag definitions), as it is not expected 
  that users use util-logging JUL Flags for logging configuration with Finatra servers since Finatra 
  uses the SLF4J-API. Logging configuration should be done on the choosen SLF4J-API logging 
  implementation. If your server fails to start after this change, please ensure you are not passing 
  values for the JUL util-logging Flags. ``PHAB_ID=D850852``

Changed
~~~~~~~

* finatra: Removed ``kafka`` and ``kafka-streams`` modules from finatra core library. 
  
  Note: We published a stand-alone `finatra-kafka client <https://github.com/finagle/finatra-kafka>`_ 
  with deprecation announcement to serve as exit pathway for current users.
  
  Announcement: `finagle blog <https://finagle.github.io/blog/2022/06/01/announce-migrations/>`_ 
  ``PHAB_ID=D878136``

Added
~~~~~

* finatra: Introduce InMemoryTracer for inspecting Trace information via tests. ``PHAB_ID=D878616``

Runtime Behavior Changes
~~~~~~~~~~~~~~~~~~~~~~~~

finatra: Update Jackson library to version 2.13.3 ``PHAB_ID=D906005``

22.4.0
------

No Changes

22.3.0
------

Runtime Behavior Changes
~~~~~~~~~~~~~~~~~~~~~~~~

* inject-app: Remove the SLF4J-API logging bridges as dependencies. These were originally
  added as the framework was expressly opionated that users should use Logback as an SLF4J-API
  implementation, however specifying the bridges on the Finatra inject/inject-app library
  causes many issues with code that must use a different SLF4J-API logging implementation
  but still uses the Finatra framework. Users should note that if they do not include these
  bridges in some other manner that they may lose logging if they have any libraries which
  log with one of the formerly bridged implementations. Also note that servers using a
  `TwitterServer` logging implementation to support `dynamically changing log levels <https://twitter.github.io/twitter-server/Features.html#dynamically-change-log-levels>`__ will get the proper bridges as dependencies.
  ``PHAB_ID=D854393``

Runtime Behavior Changes
~~~~~~~~~~~~~~~~~~~~~~~~

* inject-server: Throw an `UnsupportedOperationException` when access to the `c.t.inject.server.DeprecatedLogging#log`
  instance is attempted. This is a JUL Logger instance which was provided only as a backward-compatible
  shim for Finatra services when the `c.t.server.TwitterServer` framework was moved to the `SLF4J-API`.
  The instance was marked `@deprecated` in hopes of alerting users to not use it. We have now updated
  it to throw an exception when accessed. Please refer to the Finatra documentation for more information
  on using the SLF4J-API for logging with the Finatra framework: https://twitter.github.io/finatra/user-guide/logging/index.html.
  ``PHAB_ID=D854365``

Added
~~~~~

* inject-app: Introduce test-friendly `c.t.inject.app.console.ConsoleWriter` and
  `c.t.inject.app.TestConsoleWriter`, which can be used to inspect the output of a command-line
  style `c.t.inject.app.App`. ``PHAB_ID=D856159``

Changed
~~~~~~~

* inject-modules: Remove deprecated `c.t.inject.modules.LoggerModule`. ``PHAB_ID=D853325``

* finatra: Bump version of Jackson to 2.13.2 ``PHAB_ID=D848592``

* inject-thrift-client: Deprecate `c.t.inject.thrift.AndThenService`, `c.t.inject.thrift.modules.AndThenServiceModule`, 
  and `c.t.inject.thrift.internal.DefaultAndThenServiceImpl`. These were plumbing for unreleased
  experimental record/replay functionality and currently do nothing with no plan for implementation.
  ``PHAB_ID=D845841``

22.2.0
------

* inject-modules: Deprecate `c.t.inject.modules.LoggerModule`. Update `c.t.inject.app.App` to
  mix in the `util/util-slf4j-jul-bridge` `Slf4jBridge` trait. The `LoggerModule` does
  not provide a solution inline with the best practices for bridging JUL to the SLF4J-API
  and users are encouraged to instead mix in the `c.t.util.logging.Slf4jBridge` into the
  main class of their application if necessary. The updates to `c.t.inject.app.App` will now
  properly bridge JUL to the SLF4J-API early in the constructor of the application catching any
  log messages emitted to JUL before where the bridging would have been attempted when using
  the `LoggerModule`.

  Note that the `Slf4jBridge` trait is already mixed into the `c.t.server.TwitterServer` trait and
  thus no further action is necessary for bridging JUL to the SLF4J-API in extensions of
  `c.t.server.TwitterServer`. ``PHAB_ID=D827584``

* inject-slf4j: Deprecate `c.t.inject.logging.Logging` trait and methods. Users are encouraged
  to use the `c.t.util.logging.Logging` trait directly. There are no replacements for
  `c.t.inject.logging.Logging#debugFutureResult` and `c.t.inject.logging.Logging#time`.
  ``PHAB_ID=D821722``

22.1.0
------

Added
~~~~~

* http-server: (BREAKING API CHANGE) Allow for customization of the building of the HTTP and HTTPS
  `ListeningServer` constructs. This allows users to specify any additional configuration over the
  Finagle `Service[-R, +R]` that is constructed by the `HttpRouter`. The
  `c.t.finatra.http.HttpServerTrait#build` method has been replaced by two more specific versions:
  `#buildHttpListeningServer` and `#buildHttpsListeningServer` which are used in `postWarmup` to
  create the appropriate `ListeningServer` given it has a defined port value.

  We also update the `EmbeddedHttpServer` and `EmbeddedHttpClient` to allow for being able to run both
  the HTTP and HTTPS listening servers in tests. This is done by setting the `httpsPortFlag` to the
  value of `https.port` which will enable the binding of the HTTPS listening server to the ephemeral
  port in tests. ``PHAB_ID=D809633``

* mysql-client: Add base client configuration in EmbeddedMysqlServer to enable for more robust
  testing setup. This would allow users to add configurations like `charset`.
  Added a overridable function `createRichClient` to MysqlClientModuleTrait to allow
  creating the mysql client in other ways like `newRichClient(Finagle.Name, String)`.
  ``PHAB_ID=D805245``

Changed
~~~~~~~

* finatra: Bump version of Jackson to 2.13.1 ``PHAB_ID=D808049``

21.12.0
-------

Changed
~~~~~~~

* inject-core: Move `runAfterAll` hook from `c.t.inject.IntegrationTestMixin` to
  `c.t.inject.TestMixin`. ``PHAB_ID=D784576``

21.11.0
-------

Added
~~~~~

* inject-core: Introduce a `runAfterAll` hook in `c.t.inject.IntegrationTestMixin` to allow for
  running logic to clean-up test resources in the `org.scalatest.BeforeAndAfterAll#afterAll` without
  needing to 1) override `org.scalatest.BeforeAndAfterAll#afterAll`, 2) ensure `super` is called for
  other resources clean-up, and 3) ensure all resources get cleaned up, regardless of non-fatal
  exceptions thrown as part of the clean-up logic and otherwise fail the TestSuite run.
  ``PHAB_ID=D707939``

Runtime Behavior Changes
~~~~~~~~~~~~~~~~~~~~~~~~

* http-server: Treat non-constant routes with or without trailing slash as 2 different routes.
  For example, "/user/:id" and "/user/:id/" are treated as different routes in Finatra. You can still
  use the optional trailing slash `/?` which will indicate Finatra to ignore the trailing slash while
  routing. We don't recommend to mix both use cases when defining your routes. For example: passing
  in both `/user/:id` and `/user/:id/?` at the same time. ``PHAB_ID=D787560``

Changed
~~~~~~~

* http-server (BREAKING API CHANGE): Will now serialize many self-referential Jackson types as "{}"
  instead of returning a serialization error.  See https://github.com/FasterXML/jackson-databind/commit/765e2fe1b7f6cdbc6855b32b39ba876fdff9fbcc
  for more details. ``PHAB_ID=D744627``

21.10.0
-------

* http-server: Add versions of `HttpRouter#filter` which accept a Guice `TypeLiteral` to
  aid Java users in being able to apply generically typed Filters obtained from the object graph.
  ``PHAB_ID=D768777``

21.9.0
------

Changed
~~~~~~~

* finatra-thrift (BREAKING API CHANGE): Removed c.t.finatra.thrift.ThriftClient#thriftClient, use
  #methodPerEndpoint. ``PHAB_ID=D747744``

* finatra: Bump version of Logback to 1.2.6. ``PHAB_ID=D742405``

* finatra: Bump version of Jackson to 2.11.4. ``PHAB_ID=D727879``

* finatra: Bump version of Joda-Time to 2.10.10. ``PHAB_ID=D729742``

* finatra: Bump version of logback to 1.2.5. ``PHAB_ID=D729767``

* finatra: Bump version of json4s to 3.6.11. ``PHAB_ID=D729764``

21.8.0 (No 21.7.0 Release)
--------------------------

Fixed
~~~~~

* inject-core: Fixed a bug where `c.t.inject.TestMixin#assertFailedFuture` would incorrectly pass
  for non-failed `c.t.util.Future` in some cases where the tested failure is a supertype of
  `org.scalatest.exceptions.TestFailedException`. ``PHAB_ID=D705002``

Changed
~~~~~~~

* inject-utils: Removed deprecated `c.t.inject.conversions.string`, use
  `c.t.conversions.StringOps` in the util/util-core project instead.
  ``PHAB_ID=D692729``

* inject-utils: Removed deprecated `c.t.inject.conversions.tuple`, use
 `c.t.conversions.TupleOps` in the util/util-core project instead.
  ``PHAB_ID=D692429``

* inject-utils: Removed deprecated `c.t.inject.conversions.seq`, use
  `c.t.conversions.SeqOps` in the util/util-core project instead.
  ``PHAB_ID=D692775``

* inject-utils: Removed implicit class RichMap from  `c.t.inject.conversions.map`, use
  `c.t.conversions.MapOps` in the util/util-core project instead. ``PHAB_ID=D699010``

* thrift: Update the test `c.t.finatra.thrift.ThriftClient` to close client and clean-up resources
  during the `EmbeddedTwitterServer` close. ``PHAB_ID=D707963``

* finatra: Update ScalaCheck to version 1.15.4 ``PHAB_ID=D691691``

21.6.0
------

Changed
~~~~~~~

* inject-thrift-client (BREAKING API CHANGE): Removed the deprecated
  `c.t.inject.thrift.modules.FilteredThriftClientModule`. Please use its successor
  `c.t.inject.thrift.modules.ThriftMethodBuilderClientModule` for per-method configuration of a
  Thrift client. ``PHAB_ID=D687663``

* thrift: Add `service_class` to Finatra library thrift registry entry. ``PHAB_ID=D687117``

* finatra (BREAKING API CHANGE): Update to use the new util/util-jackson `ScalaObjectMapper` for
  case class object mapping. We've removed the custom Finatra `c.t.finatra.jackson.ScalaObjectMapper`
  and instead now use the `c.t.util.jackson.ScalaObjectMapper`. Since the `c.t.util.jackson.ScalaObjectMapper`
  does not support `Joda-Time <https://www.joda.org/joda-time/>`__, backwards compatibility is
  maintained through usage of the Finatra `ScalaObjectMapperModule` for obtaining a configured
  `ScalaObjectMapper` which will be created with Joda-Time support, though support for Joda-Time in
  Finatra is deprecated and users should expect for Joda-Time support to be removed in an upcoming release.
  Users should prefer to use the JDK 8 `java.time` classes or `java.util.Date`.

  The finatra/inject `c.t.inject.domain.WrappedValue` has been removed and users should update to the
  util/util-core `c.t.util.WrappedValue` instead.

  The finatra/jackson JsonDiff utility is also removed. Users should switch to the improved version
  in util/util-jackson: `c.t.util.jackson.JsonDiff`.

  With the move to the util/util-jackson `ScalaObjectMapper` we're also able to clean up some awkward
  directory structures in Finatra which were necessary because of dependencies. Specifically, the
  `finatra/json-annotations` library no longer exists, as `@InjectableValue` is now an annotation in
  `util/util-jackson-annotations`, and the remaining binding annotations `@CamelCaseMapper` and `@SnakeCaseMapper`
  have been moved into `finatra/jackson`.

  Using the util/util-jackson `ScalaObjectMapper` also brings `Java 8 date/time <https://www.oracle.com/technical-resources/articles/java/jf14-date-time.html>`__
  (JSR310) support via inclusion of the Jackson `JavaTimeModule <https://github.com/FasterXML/jackson-modules-java8/tree/master/datetime>`__
  by default.

  Lastly, we've also added the `YamlScalaObjectMapperModule` which can be used in place of the
  `ScalaObjectMapperModule` in order to provide a `YAMLFactory` configured `ScalaObjectMapper`.
  ``PHAB_ID=D664955``

* inject-utils: Remove deprecated `c.t.inject.utils.StringUtils`. Users should prefer to use
  the corresponding methods in `com.twitter.conversions.StringOps` from util/util-core, instead.
  ``PHAB_ID=D684659``

* inject-utils: Remove deprecated `c.t.inject.utils.AnnotationUtils`. Users should instead prefer
  `c.t.util.reflect.Annotations` from util/util-reflect. ``PHAB_ID=D684662``

21.5.0
------

Fixed
~~~~~

* finatra-jackson: Do not enforce `CaseClassDeserializer` deserialization semantics for a
  field until after any deserializer annotation has been resolved. This fully allows a deserializer
  to specify how to deserialize a field completely independent of the `CaseClassDeserializer`
  requirements for fields. For example, if a user wanted to allow parsing of a JSON `null` value
  into a `null` field instance value, they could define a custom deserializer to do so and annotate
  the case class field with `@JsonDeserialize(using = classOf[CustomNullableDeserializer])`.

  Additionally, we've fix a bug in how String case class fields are handled when the incoming JSON is
  not a String-type. The current code incorrectly returns an empty string when the field value is
  parsed into Jackson ContainerNode or ObjectNode types and an incorrect `toString` representation
  for a PojoNode type. We now correctly represent the field value as a string in these cases to
  deserialize into the case class field. ``PHAB_ID=D676938``

* finatra-jackson: Properly handle Scala enumeration fields wrapped in an `Option` during
  deserialization failures in the `CaseClassDeserializer#isScalaEnumerationType` method.
  ``PHAB_ID=D665062``

Changed
~~~~~~~

* finatra-kafka: Deprecate `c.t.finatra.kafka.consumers.TracingKafkaConsumer`
  as it only produced single-span traces and there is no way to propagate the `TraceId` back to the
  caller without changing the entire API. Users should use the
  `c.t.finatra.kafka.consumers.KafkaConsumerTracer.trace` method instead to enable tracing for
  Kafka Consumers. Also added `c.t.finatra.kafka.producers.KafkaProducerTraceAnnotator` and
  `c.t.finatra.kafka.consumers.KafkaConsumerTraceAnnotator` services which will can be used to add
  custom trace annotations to the producer and consumer spans. ``PHAB_ID=D649655``

* finatra (BREAKING API CHANGE): Update to use the new util/util-validator `ScalaValidator` for case
  class field validations. We've removed the custom Finatra `c.t.finatra.validation.Validator` and
  instead now use the `c.t.util.validation.ScalaValidator`. Constraint annotations and validator
  implementations now use the standard `jakarta.validation` API interface classes instead of any
  custom Finatra types. We've deprecated the custom Finatra constraints as they are duplicative of
  already existing "standard" or otherwise provided constraints and validators. Additionally,
  `c.t.finatra.validation.ErrorCode` is deprecated with no replacement. The same data carried can be
  obtained via the standard `jakarta.validation.ConstraintViolation[T]`.

  Adapting the Finatra framework to use the util/util-validator also includes the framework Jackson
  integration. We're also taking this opportunity to clean up the error reporting interface of
  the `CaseClassFieldMappingException` to define a `CaseClassFieldMappingException.Reason` type to
  replace the usage of the (removed) `ValidationResult.Invalid` type. The `Reason` carries a message
  String as well as a `CaseClassFieldMappingException.Detail` which can be one of several possible
  types including a `CaseClassFieldMappingException.ValidationError` which carries any failed validation
  information including the emitted `ConstraintViolation[T]`.

  Lastly, we are deprecating support for JSON serialization/deserialization of JodaTime fields in
  case classes. This support will be dropped in an upcoming release. Users should prefer to use the
  JDK 8 `java.time` classes and we will be adding support for these types in the Finatra Jackson
  integration in the future. ``PHAB_ID=D659556``

* finatra-jackson: (BREAKING API CHANGE) JsonLogging should use the lazy Scala SLF4J logger
  and no longer return the passed in argument that's logged as JSON. ``PHAB_ID=D563699``

21.4.0
------

Changed
~~~~~~~

* http-core: Add support to build a multipart/form-data POST request in Finatra RequestBuilder.
  ``PHAB_ID=D648869``

* finatra-kafka-streams: Update AsyncTransformer to support threadpools. ``PHAB_ID=D611608``

* finatra-kafka-streams: Set kafka.producer.acks=all by default ``PHAB_ID=D643266``

21.3.0
------

Added
~~~~~

* inject-thrift-client: Add per-method retry configuration withMaxRetries in
  `com.twitter.inject.thrift.ThriftMethodBuilder` for customizing `configureServicePerEndpoint`.
  ``PHAB_ID=D619565``

Changed
~~~~~~~

* finatra (BREAKING API CHANGE): Deprecate `c.t.inject.utils.AnnotationUtils`, users should instead use
  `c.t.util.reflect.Annotations` from `com.twitter:util-reflect`. Deprecate
  `c.t.finatra.utils.ClassUtils`, users should instead use either
  `c.t.util.reflect.Classes#simpleName`, `c.t.util.reflect.Types#isCaseClass` or
  `c.t.util.reflect.Types#notCaseClass` from `com.twitter:util-reflect`. ``PHAB_ID=D638655``

* finatra (BREAKING API CHANGE): Builds are now only supported for Scala 2.12+ ``PHAB_ID=D631091``

* finatra: Revert to scala version 2.12.12 due to https://github.com/scoverage/sbt-scoverage/issues/319
  ``PHAB_ID=D635917``

* finatra: Bump scala version to 2.12.13 ``PHAB_ID=D632567``

* finatra: Move com.twitter.finatra.http.{jsonpatch,request} from the finatra/http-server project to
  finatra/http-core project. Please update your build artifact references accordingly.
  ``PHAB_ID=D623745``

* http-server|http-core|jackson|thrift|validation: Update to use `c.t.util.reflect.Types`
  in places for TypeTag reflection. ``PHAB_ID=D631819``

* finatra: Move c.t.finatra.http.{context,exceptions,response} from the finatra/http-server project
  to finatra/http-core project. Please update your build artifact references accordingly.
  ``PHAB_ID=D631772``

* finatra: Move c.t.finatra.http.streaming from the finatra/http-server project to
  finatra/http-core project. Please update your build artifact references accordingly.
  ``PHAB_ID=D631371``

* http-core: Introduce `c.t.finatra.http.marshalling.MessageBodyManager#builder` for creating an immutable
  `c.t.finatra.http.marshalling.MessageBodyManager`. The MessageBodyManager's constructor is now private.
  ``PHAB_ID=D621755``

* http-server: Move `c.t.finatra.http.modules.MessageBodyFlagsModule` to
  `c.t.finatra.http.marshalling.modules.MessageBodyFlagsModule`. ``PHAB_ID=D626600``

* validation: Remove deprecated constraint type aliases under `com.twitter.finatra.validation`, users
  should prefer the actual constraint annotations at `com.twitter.finatra.validation.constraints`.
  ``PHAB_ID=D625174``

* jackson: Remove deprecated `com.twitter.finatra.json.utils.CamelCasePropertyNamingStrategy`,
  users should prefer to use `PropertyNamingStrategy#LOWER_CAMEL_CASE` or an equivalent directly.
  Also remove the deprecated `com.twitter.finatra.json.annotations.JsonCamelCase`, users should
  use the `@JsonProperty` or `@JsonNaming` annotations or an appropriately configured
  Jackson `PropertyNamingStrategy` instead. ``PHAB_ID=D623807``

* inject-core: (BREAKING API CHANGE) Rename `c.t.inject.TwitterModule.closeOnExit` to `onExit` so
  it mirrors the API from `c.t.inject.App`.  ``PHAB_ID=D621095``

* http-client: Remove deprecated c.t.finatra.httpclient.modules.HttpClientModule.
  Use c.t.finatra.httpclient.modules.HttpClientModuleTrait instead.
  ``PHAB_ID=D619591``

* http-client: Remove deprecated c.t.finatra.httpclient.RichHttpClient. Use c.t.finagle.Http.Client
  or c.t.finatra.httpclient.modules.HttpClientModuleTrait instead. Additionally,
  `c.t.finatra.httpclient.modules.HttpClientModule.provideHttpService` has been removed. Use
  `c.t.finatra.httpclient.modules.HttpClientModuleTrait.newService(injector, statsReceiver)`
  instead. ``PHAB_ID=D619547``

* finatra: Move c.t.finatra.http.fileupload from the finatra/http-server project to
  finatra/http-core project. Please update your build artifact references accordingly.
  ``PHAB_ID=D620478``

* http-client: Remove deprecated method `get` from c.t.finatra.httpclient.HttpClient.
  Use HttpClient's `execute` instead. ``PHAB_ID=D618904``

* finatra: Create the finatra/http-core project, which is meant to contain common artifacts
  for the finatra/http-server and finatra/http-client project. As part of this
  change, the `com.twitter.finatra.httpclient.RequestBuilder` has been deprecated
  and should be updated to reference `com.twitter.finatra.http.request.RequestBuilder`.
  ```PHAB_ID=D618583```

* finatra: Rename the finatra/httpclient project to finatra/http-client. Please update your
  build artifact references (i.e. SBT, Maven) to use "finatra-http-client".
  ``PHAB_ID=D617614``

* kafkaStreams:  Switch the default Kafka client and Kafka Stream client to version 2.4.1.
  ``PHAB_ID=D606782``

* finatra: Rename the finatra/http project to finatra/http-server. Please update your
  build artifact references (i.e. SBT, Maven) to use "finatra-http-server". See the
  `Finatra User's Guide <https://twitter.github.io/finatra/user-guide/index.html>`__
  ``PHAB_ID=D616257``

21.2.0
------

Changed
~~~~~~~

* finatra: all subprojects cross-building with 2.13.1. ``PHAB_ID=D613483``

* kafkaStreams: Enables cross-build for 2.13.1 for projects kafkaStreamsStaticPartitioning,
  kafkaStreamsPrerestore, and kafkaStreamsQueryableThrift. ``PHAB_ID=D608958``

21.1.0
------

Changed
~~~~~~~

* kafka: Enables cross-build for 2.13.1. Note that kafka 2.5 is bundled with scala 2.13+
  and kafka 2.2 is bundled with scala 2.12-. ``PHAB_ID=D597065``

* kafkaStreams: Enables cross-build for 2.13.1. Note that kafka 2.5 is bundled with
  scala 2.13+ and kafka 2.2 is bundled with scala 2.12-. ``PHAB_ID=D597065``

* benchmarks: Enables cross-build for 2.13.1. ``PHAB_ID=D597288``

* inject-thrift-client-http-mapper: Enables cross-build for 2.13.1. ``PHAB_ID=D596470``

* http-mustache: Enables cross-build for 2.13.1. ``PHAB_ID=D596470``

* thrift: (BREAKING API CHANGE) Removed `JavaThriftRouter.add(controller, protocolFactory)` method.
  Use `AbstractThriftServer.configureThriftServer` to override Thrift-specific stack params
  (including `Thrift.param.ProtocolFactory`).  ``PHAB_ID=D593876``

* finatra-http: Remove deprecated `c.t.finatra.http.response.StreamingResponse`.
  Use `c.t.finatra.http.streaming.StreamingResponse` instead. ``PHAB_ID=D594642``

* finatra-kafka-streams: (BREAKING API CHANGE) Changed the `delayWithStore` DSL call to ensure that
  the store name is consistent across shards. Requires a new `storeName` parameter to allow
  for multiple delays in a single topology. ``PHAB_ID=D593593``

Fixed
~~~~~

* finatra-kafka-streams: Renamed implicit Kafka Streams DSL classes in order to
  permit multiple DSL extensions to be used in the same Kafka Streams topology.
  ``PHAB_ID=D598368``

* thrift: Fixed a bug where Thrift stack params (i.e., protocol factory) that are passed to
  `AbstractThriftServer.configureThriftServer` are ignored in `JavaThriftRouter`.
  ``PHAB_ID=D593876``

20.12.0
-------

Added
~~~~~

* finatra-kafka-streams: Add async map commands to Kafka Streams DSL (`flatMapAsync`,
  `flatMapValuesAsync`, `mapAsync`, and `mapValuesAsync`) ``PHAB_ID=D593995``

* finatra-kafka-streams: Allow String configuration to be null and set upgradefrom to null if it is
   running in 2.5 kafka client.  ``PHAB_ID=D592608``

* finatra-http: Allow injecting filtered controllers in HttpRouter from Java. ``PHAB_ID=D590707``

* inject-utils: Move deprecation warning from `c.t.inject.conversions.map` to
  `c.t.inject.conversions.map.RichMap`. ``PHAB_ID=D591979``

* kafka: Add an option `includePartitionMetrics` to `KafkaFinagleMetricsReporter` to not include
  metrics per partition of the `FinagleKafkaConsumer`. Defaults to true. ``PHAB_ID=D587636``

* finatra: Enables cross-build for 2.13.1 for inject-logback. ``PHAB_ID=D588586``

* finatra-kafka-streams: Add delay DSL calls to insert a delay into a Kafka Streams topology.

* finatra: Enables cross-build for 2.13.1 for inject-thrift-client. ``PHAB_ID=D583509``

* finatra-kafka-streams: Add `c.t.f.k.t.s.PersistentTimerValueStore` which stores a value in the
  timerstore that can be used when the timer is triggered. ``PHAB_ID=D583020``

* inject-core: Add ability to call `InMemoryStats#waitFor` with a fixed timeout
  ``PHAB_ID=D576147``

* finatra: Enables cross-build for 2.13.1 for httpclient, http, and jackson. ``PHAB_ID=D574391``

Changed
~~~~~~~

* inject-utils: Deprecate all methods in `c.t.inject.conversions.map.RichMap`, and move
  functionality to `c.t.conversions.MapOps` in the util/util-core project. ``PHAB_ID=D578819``

* inject-utils: Deprecate all methods in `c.t.inject.conversions.tuple`, and move functionality
  to `c.t.conversions.TupleOps` in the util/util-core project. ``PHAB_ID=D578804``

* inject-utils: Deprecate all methods in `c.t.inject.conversions.seq`, and move functionality
  to `c.t.conversions.SeqOps` in the util/util-core project. ``PHAB_ID=D578605``

* inject-utils: Remove deprecated `camelify`, `pascalify`, and `snakify` from
  `c.t.inject.conversions.string.RichString`. Additionally, deprecate `toOption` and
  `getOrElse` in `c.t.inject.conversions.string.RichString`, and move functionality to
  `c.t.conversions.StringOps` in the util/util-core project. ``PHAB_ID=D578549``

* c.t.finatra.http.exceptions.ExceptionMapperCollection changed from Traversable to Iterable
  for cross-building 2.12 and 2.13. ``PHAB_ID=D574391``

* inject-core: (BREAKING API CHANGE) Move the testing utility `InMemoryStatsReceiverUtility`
  and `InMemoryStats` into inject-core from inject-server. They can both be found under
  `com.twitter.inject`. ``PHAB_ID=D574643``

* validation: (BREAKING API CHANGE) Introduce new Validation Framework APIs which support
  cascading validation to nested case classes and other improvements which also closer align
  to JSR380. `Validator#validate` has changed from returning `Unit` and throwing an exception
  to model the JSR380 version that returns a Set of failed constraints. There is a new method
  which replicates the throwing behavior. ``PHAB_ID=D559644``

* kafka: Split `c.t.f.kafka.tracingEnabled` flag into `c.t.f.k.producers.producerTracingEnabled` and
  `c.t.f.k.consumers.consumerTracingEnabled` to selectively enable/disable tracing for
  producers/consumers. Producer tracing is turned on by default and consumer tracing is turned off
  by default now. ``PHAB_ID=D571064``

Fixed
~~~~~~~

* inject-server: Wire through HTTP method in AdminHttpClient so that POST requests can be made to
  HTTPAdmin endpoints. ``PHAB_ID=D584988``

20.10.0
-------

Added
~~~~~

* finatra-kafka-streams: Add toCluster DSL call to publish to another Kafka cluster.
 ``PHAB_ID=D562195``

* jackson: Add support for validating `@JsonCreator` annotated static (e.g., companion
  object defined apply methods) or secondary case class constructors. ``PHAB_ID=D552921``

* inject-app: Allow injecting flags without default values as both `scala.Option` and
  `java.util.Optional`. ``PHAB_ID=D526226``

Changed
~~~~~~~

* utils: Undo usage of TypesApi for help in determining if a class is a Scala case class
  as this fails for generic case classes in Scala 2.11, failing some supported cases for
  Jackson processing. ``PHAB_ID=D566596``

* utils: Update `ClassUtils#simpleName` to handle when package names have underscores
  followed by a number which throws an `InternalError`. Add tests. ``PHAB_ID=D566069``

* utils: Revamp `ClassUtils#isCaseClass` to use the TypesApi for help in determining
  if a class is a Scala case class. Add tests. ``PHAB_ID=D566069``

* http: The http server did not properly log the bound address on server startup. Fix this
  and make the thrift server consistent. ``PHAB_ID=D563758``

* utils: (BREAKING API CHANGE) Rename `maybeIsCaseClass` to `notCaseClass` in
  `ClassUtils` and change the scope of the method. ``PHAB_ID=D556169``

* http: Adding support for optionally passing chain in the TLS sever trait. ``PHAB_ID=D553718``

* finatra: Bump version of Joda-Time to 2.10.8. ``PHAB_ID=D570496``

Fixed
~~~~~

* finatra-kafka-streams: Revert AsyncTransformer to still use ConcurrentHashMap.
 ``PHAB_ID=D555422``

* inject-thrift-client: The `Singleton` annotation has been removed from the `DarkTrafficFilter` and
  the `JavaDarkTrafficFilter`. It was there in error. `PHAB_ID=D553539`

* inject-thrift-client: When using `RepRepServicePerEndpoint`, Finatra's `DarkTrafficFilter` would
  throw a `NoSuchMethodException` when trying to lookup an inherited Thrift endpoint.
  ``PHAB_ID=D553361``

20.9.0
------

Added
~~~~~

* finatra-inject: `TestInjector` has been reworked to allow users executing modules' lifecycle
  callbacks. Specifically, the `TestInjector` builder API has been moved under `TestInjector.Builder`
  to allow `TestInjector` extends `Injector` with two new methods: `start()` and `close()`.
  ``PHAB_ID=D537056``

Changed
~~~~~~~
* finatra-kafka-streams: Update and separate the Finatra kafka stream code base which has direct
  dependency on Kafka 2.2. Separate any code which cannot easily be upgraded to separate build
  target. ``PHAB_ID=D545900``

* inject-core: `c.t.inject.Injector` is now an abstract class. Use `Injector.apply` to create
  a new instance (versus the `new Injector(...)` before). ``PHAB_ID=D543297``

* http: Ensure HttpWarmer creates the request exactly the number of times requested and
  mutates the correct objects. ``PHAB_ID=D547310``

* kafka: Replaced the `com.twitter.finatra.kafka.TracingEnabled` toggle with a GlobalFlag enabling
  Zipkin tracing for Kafka clients. ``PHAB_ID=D525274``

* finatra: Bump version of Jackson to 2.11.2. ``PHAB_ID=D538440``

Fixed
~~~~~

* jackson: Fix issue in the handling of unknown properties. The `CaseClassDeserializer` only
  considered the case where the incoming JSON contained more fields than the case class and
  not the case where the incoming JSON contained less fields than specified in the case class.
  This has been fixed to ensure that when the fields of the JSON do not line up to the
  non-ignored case class fields the handling of unknown properties is properly invoked.
  ``PHAB_ID=D549353``

* validation: `c.t.f.validation.Validator` would throw an `IndexOutOfBoundsException` when
  trying to validate a case class which contained additional fields that are not included in the
  constructor parameters. ``PHAB_ID=D549253``

20.8.1
------

Added
~~~~~

* thrift: `JavaThriftRouter` now allows mounting controllers by value (as opposed to via DI).
  ``PHAB_ID=D528659``

* finatra-kafka: Expose delivery timeout duration in KafkaProducerConfig. ``PHAB_ID=D535761``

Changed
~~~~~~~

* inject-core: Remove deprecated `com.twitter.inject.Mockito` trait. Users are encouraged to
  switch to the `com.twitter.util.mock.Mockito` trait from util/util-mock. ``PHAB_ID=D529174``

Fixed
~~~~~

* inject-server: Ensure `Awaiter.any` does not try to block on an empty list of Awaitables. Add
  tests. ``PHAB_ID=D537727``

* finatra-jackson: Fix bugs around generic case class deserialization involving other generic
  types. Reported (with reproduction and pointers) on GitHub by @aatasiei
  (https://github.com/twitter/finatra/issues/547). Fixes #547. ``PHAB_ID=D532768``

* finatra-jackson: Fix a bug preventing JSON parsing of generic case classes, which in turn, contain
  fields with generic case classes. Reported (with a thorough reproducer and an analysis) on GitHub
  by @aatasiei (https://github.com/twitter/finatra/issues/548). Fixes #548. ``PHAB_ID=D531452``

20.8.0 (DO NOT USE)
-------------------

Added
~~~~~

* inject-app: Add more Java-friendly constructors for the TestInjector. ``PHAB_ID=D520900``

Changed
~~~~~~~

* inject-modules: Improve Java usability: rename `apply` to `get` for
  StatsReceiverModule and LoggerModule. 
  Add `get` methods for other TwitterModule singleton objects. 
  (BREAKING API CHANGE) ``PHAB_IB=D525696``

* inject-core: Deprecate `c.t.inject.Resettable` (no replacement) and `c.t.inject.TestTwitterModule`.
  Users should prefer the `#bind[T]` DSL over usage of the `TestTwitterModule`. ``PHAB_ID=D520889``

Fixed
~~~~~

* inject-server: Fix EmbeddedTwitterServer to return `StartupTimeoutException` when server under
  test fails to start within max startup time. ``PHAB_ID=D519318``

20.7.0
------

Added
~~~~~

* inject-app: Adding flag converters for `java.io.File` (including comma-separated variants).
  ``PHAB_ID=D516020``

* finatra-kafka-streams: Added `TracingKafkaClientSupplier` to provide `TracingKafkaProducer` and
  `TracingKafkaConsumer` to enable Zipkin tracing. Tracing can be enabled with the toggle
  `com.twitter.finatra.kafka.TracingEnabled`.  ``PHAB_ID=D502911``

* finatra-kafka: Added `TracingKafkaProducer` and `TracingKafkaConsumer` to enable Zipkin tracing
  for Kafka. `FinagleKafkaProducerBuilder.build()` and `FinagleKafkaConsumerBuilder.buildClient()`
  now return instances of `TracingKafkaProducer` and `TracingKafkaConsumer` respectively with
  tracing enabled by default. Tracing can be enabled with the toggle
  `com.twitter.finatra.kafka.TracingEnabled`.  ``PHAB_ID=D490711``

Changed
~~~~~~~

* finatra: Update `org.scalatest` dependency to 3.1.2 and introduce finer-grained dependencies on
  `org.scalatestplus` artifacts. ``PHAB_ID=D518553`` ``PHAB_ID=D518794``

* inject-thrift-client: Remove unused ClientId property from
  `ThriftMethodBuilderClientModule#provideServicePerEndpoint` method. ``PHAB_ID=D513491``

* inject-server: Improve startup time of `EmbeddedTwitterServer` by observing lifecycle events to
  determine startup, where previously we were doing 1 second polls. The `nonInjectableServerStarted`
  property is removed and `isStarted` should be referenced regardless of the type of underlying
  `twitterServer` type. The end result should see a faster test execution feedback loop. Our Finatra
  test targets range from a roughly 2x to 10x reduction in execution times.

  You may experience new test failures in cases where an exception is thrown as part of
  `c.t.inject.TwitterServer.start()` or `c.t.server.TwitterServer.main()` and the test would have
  expected a failure as part of startup. As the error takes place after the startup lifecycle,
  you may now need to `Await.result` the `EmbeddedTwitterServer.mainResult()` to assert the error.

  You may also experience some new non-deterministic behavior when testing against PubSub style
  logic. As the server may be started earlier, your tests may be relying on assumptions that
  an event would have occurred within the previous 1 second startup poll, which is no longer
  guaranteed. You may need to adjust your test logic to account for this behavior.

  ``PHAB_ID=D499999``

* finatra: Update `com.google.inject.guice` dependency to 4.2.3 and `net.codingwell.scala-guice`
  to version 4.2.11. The `net.codingwell.scala-guice` library has switched from Manifests to TypeTags 
  for transparent binding and injector key creation. The `c.t.inject.TwitterModule` has moved from its 
  custom bind DSL to the `scalaguice.ScalaModule` which brings the `TwitterModule` inline with both the 
  `TwitterPrivateModule` and the `bind[T]` test DSL to now have the same consistent binding DSL across 
  all three. Thus, there is no more confusing `bindSingleton` function in the `TwitterModule` bind API.

  Upgrading scalaguice helps move a necessary dependency of Finatra to a version which is Scala 2.13 
  compatible moving Finatra closer to Scala 2.13 support. ``PHAB_ID=D504559`` ``PHAB_ID=D515857``

Fixed
~~~~~

* inject-app: Having two sets of flag converters for primitive types (both Java and Scala) confuses
  the DI runtime, preventing the injection. We now have only a single set of converters, based off
  Scala primitive types.  ``PHAB_ID=D509192``

20.6.0
------

Added
~~~~~

* inject-app: You can now inject Flag values of any type (not just primitive types). Most of the
  common Flag types are already supported out of the box (e.g., `Seq[InetSocketAddress]`), but it's
  also possible to register your own converters derived from any Flaggable instance.
  ``PHAB_ID=D498210``

* inject-stack: Move `StackTransformer` from `inject/inject-core` to `inject/inject-stack` to
  remove the finagle-core dependency from `inject/inject-core`. ``PHAB_ID=D489604``

* inject-server: adding httpPostAdmin test method. ``PHAB_ID=D482624``

Changed
~~~~~~~

* thrift/http: Introduce a `Common Log Format <https://en.wikipedia.org/wiki/Common_Log_Format>`__ 
  type of formatting for Thrift access logging to replace the current `prelog` text. Ensure 
  the HTTP and Thrift access logging filters are aligned in functionality and behavior. 
  ``PHAB_ID=D497596``

* inject-slf4j: Remove Jackson dependency. Case classes which wish to use the slf4j Logging
  functionality should use the finatra/jackson `c.t.finatra.jackson.caseclass.SerdeLogging`
  trait which provides a `@JsonIgnoreProperties` to ignore logging fields. ``PHAB_ID=D487948``

Fixed
~~~~~

* finatra-validation: Added a `c.t.finatra.validation.ValidatorModule` to provide a default
  Validator. The new module is added to the default Finatra HttpServer modules, users can override
  it with a customized `ValidatorModule` by overriding the `validatorModule` field.
  ``PHAB_ID=D503417``

20.5.0
------

Added
~~~~~

* inject-mdc: Move MDC integration from `inject/inject-slf4j` to `inject/inject-mdc`.
  ``PHAB_ID=D485870``

* finatra-http|finatra-thrift: Update TraceIdMDCFilter to log traceSampled and traceSpanId
  ``PHAB_ID=472013``

* finatra-examples: Ensure there are Java and Scala examples for the different
  types of applications and servers which can be built with Finatra. Update `/examples`
  directory layout for discoverability and consistency. ``PHAB_ID=D469677``


Changed
~~~~~~~

* inject-slf4j: Move MDC integration from `inject/inject-slf4j` to `inject/inject-mdc`.
  ``PHAB_ID=D485870``

* finatra-http: Allow extensions of the `c.t.finatra.http.filters.HttpResponseFilter`
  to specify how to set the Location Header value into a Response. Additionally, don't
  allow exceptions resulting from the inability to set a non-compliant 'Location' response
  header escape the filter. ``PHAB_ID=D483793``

* inject-core: Make flag methods in `c.t.inject.TwitterModule` public an final. ``PHAB_ID=D484168``

* inject-core: `c.t.inject.Mockito` has been marked deprecated. Users are encouraged to prefer
  `mockito-scala <https://github.com/mockito/mockito-scala>`_ (or ScalaTest `MockitoSugar <http://doc.scalatest.org/3.1.1/#org.scalatestplus.mockito.MockitoSugar>`_
  which provides some basic syntax sugar for Mockito). ``PHAB_ID=D482531``

* http: (BREAKING API CHANGE) Update the `c.t.finatra.http.HttpResponseFilter` to optionally fully
  qualify response 'Location' header values. A `previous change <https://github.com/twitter/finatra/commit/ff9acc9fbf4e89b532df9daf2b9cba6d90b2df96>`_
  made the filter always attempt to fully qualify any response 'Location' header value. This updates
  the logic to be opt-in for the more strict returning of fully qualified 'Location' header values with
  the default being to allow relative values per the `RFC7231 <https://tools.ietf.org/html/rfc7231#section-7.1.2>`_
  which replaces the obsolete `RFC2616 <https://tools.ietf.org/html/rfc2616#section-14.30>`_. This is
  thus a breaking API change as the default is now to allow relative values. To enable the previous
  strict behavior, users should instantiate the filter with the constructor arg `fullyQualifyLocationHeader`
  set to 'true'. This addresses issue #524. ``PHAB_ID=D467909``

* jackson: Remove deprecated `FinatraObjectMapper` and `FinatraJacksonModule`. Users are encouraged
  to switch to the equivalent `c.t.finatra.jackson.ScalaObjectMapper` and
  `c.t.finatra.jackson.modules.ScalaObjectMapperModule`. ``PHAB_ID=D473177``

* finatra-http: Update `c.t.finatra.http.StreamingJsonTestHelper` to not use `Thread.sleep` for
  writing JSON elements on an artificial delay. ``PHAB_ID=D470793``

* inject-app: Remove finagle-core dependency. Introduce finatra/inject/inject-dtab.
  ``PHAB_ID=D474298``

* finatra: Bump version of Jackson to 2.11.0. ``PHAB_ID=D457496``

* finatra-http: Only create `EnrichedResponse` counters when needed. Any "service/failure"
  response counters will only be generated upon first failure and not eagerly for each
  response generated. This change impacts users who expect a counter value of 0 when no
  response failures have been encountered - now the counter will not exist until the first
  failure has been recorded. ``PHAB_ID=D474918``

* finatra: Bump version of Joda-Time to 2.10.6. ``PHAB_ID=D473522``

Fixed
~~~~~

* inject-thrift-client: Convert non-camel case `ThriftMethod` names, e.g., "get_tweets" to
  camelCase, e.g., "getTweets" for reflection lookup of generated `ReqRepServicePerEndpoint`
  interface methods in `c.t.inject.thrift.filters.DarkTrafficFilter`. ``PHAB_ID=D478104``

20.4.1
------

Added
~~~~~

* inject-app: Add default type conversions for `java.time.LocalTime`, `c.t.util.Time`,
  `java.net.InetSocketAddress`, and `c.t.util.StorageUnit`. This allows the injector to convert from
  a String representation to the given type. The type conversion for `java.net.InetSocketAddress`
  uses the `c.t.app.Flaggable.ofInetSocketAddress` implementation and the type conversion for
  `c.t.util.Time` uses the `c.t.app.Flaggable.ofTime` implementation for consistency with Flag parsing.
  Because of the current state of type erasure with `c.t.app.Flag` instances, Finatra currently binds
  a parsed `c.t.app.Flag` value as a String type, relying on registered Guice TypeConverters to convert
  from the bound String type to the requested type. These conversions now allow for a `c.t.app.Flag`
  defined over the type to be injected by the type as Guice now has a type conversion from the bound
  String type rather than as a String representation which then must be manually converted.
  ``PHAB_ID=D471545``

* finatra-http: Method in tests to return an absolute path URI with the https scheme and authority
  ``PHAB_ID=D466424``

* finatra: Java-friendly `bindClass` test APIs. The `bindClass` API calls from Java can be
  now chained with the `TestInjector`, `EmbeddedApp`, `EmbeddedTwitterServer`,
  `EmbeddedThriftServer`, and `EmbeddedHttpServer`. For example, the following is now possible:

  ```
  EmbeddedHttpServer server = new EmbeddedHttpServer(
      new HelloWorldServer(),
      Collections.emptyMap(),
      Stage.DEVELOPMENT)
      .bindClass(Integer.class, Flags.named("magic.number"), 42)
      .bindClass(Integer.class, Flags.named("module.magic.number"), 9999);

  return server;
  ```
  ``PHAB_ID=D463042``

Changed
~~~~~~~

* inject-app: Introduce consistent `c.t.app.Flag` creation methods for Java. Bring HTTP and Thrift
  server traits inline with each other to provide consistent Java support. Ensure Java examples in
  documentation. ``PHAB_ID=D471716``

* inject-core: Update the configuration of `c.t.app.Flag` instances created within a `c.t.inject.TwitterModule`
  to have `failFastUntilParsed` set to 'true' by default. While this is configurable for a given
  `c.t.inject.TwitterModule`, much like for the application itself, it is STRONGLY recommended that
  users adopt this behavior. ``PHAB_ID=D448047``

* inject-app: Update `c.t.inject.app.TestInjector` to always add the `InjectorModule`.
  ``PHAB_ID=D465943``

* inject-app: Reduce visibility of internal code in `c.t.inject.app.internal`. ``PHAB_ID=D465597``

* inject-modules: Updated BUILD files for Pants 1:1:1 layout. ``PHAB_ID=D442977``

Fixed
~~~~~

* finatra-kafka: Close a result observer when Namer.resolve fails. ``PHAB_ID=D416044``

20.4.0 (DO NOT USE)
-------------------

Added
~~~~~

* inject-app: Add Java-friendly `main` to `EmbeddedApp`. ``PHAB_ID=D458375``

* finatra-kafka: Expose timeout duration in KafkaProducerConfig dest(). ``PHAB_ID=D457356``

Changed
~~~~~~~

* finatra-validation|jackson: (BREAKING API CHANGE) Introduced new case class validation library
  inspired by JSR-380 specification. The new library can be used as its own to validate field and
  method annotations for a case class. The library is also automatically integrated with Finatra's
  custom `CaseClassDeserializer` to efficiently apply per field and method validations as request
  parsing is performed. However, Users can easily turn off validation during request parsing with
  the setting `noValidation` in their server configurations. For more information, please checkout
  `Finatra User's Guide <https://docbird.twitter.biz/finatra/user-guide/index.html>`__.
  ``PHAB_ID=D415743``

* finatra: guice is upgraded to 4.2.1 ``PHAB_ID=D457714``

20.3.0
------

Added
~~~~~

Changed
~~~~~~~

* finatra-validation|jackson: Remove Jackson dependency from finatra/validation. This
  was for `ErrorCode` reporting but can be moved to finatra/jackson. ``PHAB_ID=D445364``

* finatra-kafka-streams: (BREAKING API CHANGE) Update AsyncTransformer to preserve
  record context. ``PHAB_ID=D436227``

* finatra-jackson: Better handling of Scala enumeration mapping errors. Currently, if mapping
  of a Scala enumeration during deserialization fails a `java.util.NoSuchElementException` is
  thrown which escapes deserialization error handling. Update to instead handle this failure case
  in order to correctly translate into a `CaseClassFieldMappingException` which will be wrapped
  into a `CaseClassMappingException`. ``PHAB_ID=D442575``

Fixed
~~~~~

20.2.1
------

Added
~~~~~

* finatra-kafka-streams: Add method to `c.t.f.kafkastreams.test.TopologyTesterTopic` to write
  Kafka messages with custom headers to topics. ``PHAB_ID=D424440``

* finatra-http: Add `toBufReader` to get the underlying Reader of Buf from StreamingResponse.
  If the consumed Stream primitive is not Buf, the returned reader streams a serialized
  JSON array. ``PHAB_ID=D434448``

* inject-app: Add functions to `c.t.inject.app.AbstractApp` to provide better
  ergonomics for Java users to call and use basic `App` lifecycle callbacks. 
  ``PHAB_ID=D433874``

* inject-server: Add functions to `c.t.inject.server.AbstractTwitterServer` to provide 
  better ergonomics for Java users to call and use basic `TwitterServer` lifecycle 
  callbacks. ``PHAB_ID=D433874``

* inject-slf4j: Add a way to retrieve the currently stored Local Context map backing the
  MDC. ``PHAB_ID=D431148``

* finatra-jackson: Added new functionality in the `CaseClassDeserializer` to support more
  Jackson annotations during deserialization. See documentation for more information.
  ``PHAB_ID=D407284``

* finatra: Add NullKafkaProducer for unit tests to avoid network connection failures in the log.
  ``PHAB_ID=D429004``

Changed
~~~~~~~
* finatra: Update Google Guice version to 4.2.0 ``PHAB_ID=D372886``

* finatra: Bumped version of Joda to 2.10.2 and Joda-Convert to 1.5. ``PHAB_ID=D435987``

* finatra-jackson|finatra-http-annotations: Move http-releated Jackson "injectablevalues"
  annotations from `finatra/jackson` to `finatra/http-annotations`.

  Specifically the follow have changed packages,
  `c.t.finatra.request.QueryParam`       --> `c.t.finatra.http.annotations.QueryParam`
  `c.t.finatra.request.RouteParam`        --> `c.t.finatra.http.annotations.RouteParam`
  `c.t.finatra.request.FormParam`         --> `c.t.finatra.http.annotations.FormParam`
  `c.t.finatra.request.Header`                --> `c.t.finatra.http.annotations.Header`
  `c.t.finatra.request.JsonIgnoreBody` --> `c.t.finatra.http.annotations.JsonIgnoreBody`

  Users should update from `finatra/jackson/src/main/java` (`finatra-jackson_2.12`)
  to `finatra/http-annotations/src/main/java` (`finatra-http-annotations_2.12`).
  ``PHAB_ID=D418766``

* finatra-jackson: Updated Finatra Jackson integration to introduce a new `ScalaObjectMapper`
  and module to simplify configuration and creation of the mapper. See documentation for more
  information. ``PHAB_ID=D407284``

* finatra-jackson: (BREAKING API CHANGE) Moved the java binding annotations, `CamelCaseMapper` and
  `SnakeCaseMapper` from `c.t.finatra.annotations` in `finatra/jackson` to
  `c.t.finatra.json.annotations` in `finatra/json-annotations`. Moved
  `c.t.finatra.response.JsonCamelCase` to `c.t.finatra.json.annotations.JsonCamelCase` which is also
  now deprecated. Users are encouraged to use the standard Jackson annotations or a mapper with
  the desired property naming strategy configured.

  Many exceptions for case class deserialization were meant to be internal to the framework but are
  useful or necessary outside of the internals of JSON deserialization. As such we have cleaned up
  and made most JSON deserialization exceptions public. As a result, all the exceptions have been moved
  from `c.t.finatra.json.internal.caseclass.exceptions` to `c.t.finatra.jackson.caseclass.exceptions`.

  `c.t.finatra.json.internal.caseclass.exceptions.CaseClassValidationException` has been renamed to
  `c.t.finatra.jackson.caseclass.exceptions.CaseClassFieldMappingException`. `JsonInjectException`,
  `JsonInjectionNotSupportedException`, and `RequestFieldInjectionNotSupportedException` have all been
  deleted and replaced with `c.t.finatra.jackson.caseclass.exceptions.InjectableValuesException`
  which represents the same error cases.

  The `FinatraJsonMappingException` has been removed. Users are encouraged to instead use the general
  Jackson `JsonMappingException` (which the `FinatraJsonMappingException` extends).

  `RepeatedCommaSeparatedQueryParameterException` has been moved tom finatra/http.
  ``PHAB_ID=D407284``

Fixed
~~~~~

* finatra-jackson: Access to parameter names via Java reflection is not supported in Scala 2.11.
  Added a work around for the parsing of case class structures to support JSON deserialization in
  Scala 2.11 and forward. ``PHAB_ID=D431837``

* finatra-jackson: Fix for enforcing "fail on unknown properties" during deserialization. Previously,
  the `CaseClassDeserializer` was optimized to only read the fields in the case class constructor
  from the incoming JSON and thus ignored any unknown fields during deserialization. The fix will
  now properly fail if the `DeserializationFeature` is set or if the `JsonProperties` is configured
  accordingly. ``PHAB_ID=D407284``

20.1.0
------

Changed
~~~~~~~

* finatra-kafka-streams: Track `c.t.f.kafkastreams.flushing.AsyncProcessor` and
  `c.t.f.kafkastreams.flushing.AsyncTransformer` latencies with stat metrics ``PHAB_ID=D430688``

* finatra: Exposing Listening Server's bound address in Thrift and HTTP server traits
  ``PHAB_ID=D424745``

* finatra: Upgrade logback to 1.2.3 ``PHAB_ID=D415888``

Fixed
~~~~~

* inject-server: Fix issue in `c.t.inject.server.EmbeddedHttpClient` where assertion of an
  empty response body was incorrectly disallowed. This prevented asserting that a server
  was not yet healthy as the `/health` endpoint returns an empty string, thus even a not yet
  healthy server would report as "healthy" to the testing infrastructure as long as the `health`
  endpoint returned a `200 - OK` response. ``PHAB_ID=D422712``

19.12.0
-------

Changed
~~~~~~~

* finatra: Upgrade to jackson 2.9.10 and jackson-databind 2.9.10.1 ``PHAB_ID=D410846``

* finatra: Correctly track Ignorable Exceptions in per-method StatsFilter.  Responses
  marked as Ignorable are tracked in the global requests and exceptions metrics but
  were not counted under the per-method metrics.  There are now counts of `ignored`
  and total `requests` as well as ignored requests by Exception for each method. E.g.

  per_method_stats/foo/ignored 1
  per_method_stats/foo/ignored/java.lang.Exception 1
  per_method_stats/foo/requests 1

  ``PHAB_ID=D407474``

* finatra-http|jackson: (BREAKING API CHANGE) Move parsing of message body contents
  from `finatra/jackson` via the `FinatraObjectMapper` `#parseMessageBody`, `#parseRequestBody`,
  and `#parseResponseBody` methods to `finatra/http` with functionality replicated via an
  implicit which enhances a given `FinatraObjectMapper`. Additionally we have updated
  finatra-http the `MessageBodyComponent` API to use `c.t.finagle.http.Message` instead
  of `c.t.finagle.http.Request` and `c.t.finagle.http.Response`. This means that users can use the
  `MessageBodyComponent` API to read the body of Finagle HTTP requests or responses and all HTTP
  concerns are co-located in finatra-http instead of being partially implemented in finatra-jackson.

  In updating the `MessageBodyComponent` API we have removed support for polymorphic `MessageBodyReader`
  types, that is we have simplified the `MessageBodyReader` API to no longer express the `#parse`
  method parameterized to a subtype of the class type. This API allowed parsing a message body
  into a subtype solely through the presence of a given type parameter but the resulting API has
  proven to be extremely clunky. We feel that the same behavior is achievable in other ways (such
  as adapting the type after parsing) and the improvement and simplification of the
  `MessageBodyReader` API to be worth removing the awkward method signature.

  Lastly, we have fixed the returned charset encoding on response content-type header to be
  applicable only where appropriate instead of always being added when the
  `http.response.charset.enabled` flag is set to true. ``PHAB_ID=D400560``

* finatra: (BREAKING API CHANGE) move DarkTrafficFilter and related modules
  from `finatra/thrift` to `inject/inject-thrift-client`. The modules now extend
  from `c.t.inject.thrift.modules.ThriftClientModuleTrait` for more uniform configuration.
  The following changes were made:

    * c.t.finatra.thrift.filters.DarkTrafficFilter ->
      c.t.inject.thrift.filters.DarkTrafficFilter

    * c.t.finatra.thrift.modules.DarkTrafficFilterModule ->
      c.t.inject.thrift.modules.DarkTrafficFilterModule

    * c.t.finatra.thrift.modules.ReqRepDarkTrafficFilterModule ->
      c.t.inject.thrift.modules.ReqRepDarkTrafficFilterModule

    * c.t.finatra.thrift.modules.JavaDarkTrafficFilterModule ->
      c.t.inject.thrift.modules.JavaDarkTrafficFilterModule

  ``PHAB_ID=D401051``

* finatra: Update Google Guice version to 4.1.0, update ScalaTest to 3.0.8, and ScalaCheck
  to 1.14.0. ``PHAB_ID=D408309``

* finatra-http: Remove deprecated `c.t.finatra.http.HttpHeaders`. Users should use
  `com.twitter.finagle.http.Fields` instead. ``PHAB_ID=D407290``

* finatra-http: Remove deprecated `DocRootModule`. ``PHAB_ID=D404723``

* finatra-http: (BREAKING CHANGE) Remove automatic handling of Mustache rendering from
  `finatra/http` and break Mustache support into two separate libraries: `finatra/mustache`
  and `finatra/http-mustache`.

  HTTP services that want the framework to automatically negotiate Mustache template rendering
  via the Finatra HTTP `MessageBodyComponents` framework must now bring this concern into their
  HTTP services via the `finatra/http-mustache` `c.t.finatra.http.modules.MustacheModule` as the
  HTTP framework support for specifying a `MustacheModule` in the `HttpServer` has been removed.
  I.e., add this module to the server's list of modules.

  Additionally, it is also now possible to use Mustache templating completely independent of
  Finatra HTTP concerns by consuming and using only the `finatra/mustache` library which will
  render Strings via defined Mustache templates. ``PHAB_ID=D387629``

Fixed
~~~~~

* finatra-http: Fixed issue in the `DefaultMessageBodyReaderImpl` that determines if the incoming
  message is "json encoded". ``PHAB_ID=D412993``

* inject-modules: Removed the extra registration for closing a client, which used to log false
  warnings when startup a ClientModule. Only register close after materialized clients.
  ``PHAB_ID=D401288``

* inject-server: Addressed a race condition that could allow for an `AdminHttpServer` to be
  started, even when the `disableAdminHttpServer` property was set. The `AdminHttpServer` will
  no longer start prior to the warm-up phase if disabled. The `disableAdminHttpServer` property
  has also been moved to `com.twitter.server.AdminHttpServer`. `PHAB_ID=D397925``

* finatra: Remove `com.sun.activation` dependency from `build.sbt` file. The dependency
  duplicates the `javax.activation` dependency and as a result can cause a uber-JAR to fail
  to build. ``PHAB_ID=D396506``

Added
~~~~~

* finatra-jackson: (BREAKING API CHANGE) Move all Case Class annotation validation related logic to
  a new library in finatra-validation. Please update your library dependencies to the new library if
  you are using case class validations. ``PHAB_ID=D386969``

19.11.0
-------

Fixed
~~~~~

* finatra-http: Better handling of URI decoding issues when extracting path parameters for
  routing. If we cannot extract a path pattern, and the exception is not intercepted by a
  user-defined Exception Mapper, we will now explicitly return a `400 - BAD REQUEST`.
  Fixes #507. ``PHAB_ID=D381357``

Added
~~~~~

* finatra: Add initial support for JDK 11 compatibility. ``PHAB_ID=D365075``

* inject-core: Add support for optional binding in `c.t.inject.TwitterModule`.
  ``PHAB_ID=D386288``

Changed
~~~~~~~

* finatra-http: (BREAKING API CHANGE) `StreamingResponse[Reader, String]` and
  `StreamingResponse[AsyncStream, String]` now streaming `String` messages as JSON instead of
  returning as-is. Concatenating text messages need to be handled from the Controller by users.
  ``PHAB_ID=D394904``

* finatra-http: (BREAKING API CHANGE) `AsyncStream[Buf] => AsyncStream[String]` and
  `Reader[Buf] => Reader[String]` handlers will always be tread the output as a JSON arrays of
  `Strings`. Whereas, before, the incoming bytes would have been converted to `String` and
  returned as-is. ``PHAB_ID=D392551``

* finatra: Deprecate `c.t.finatra.http.modules.DocRootModule`. Introduce `FileResolverModule`.
  The `DocRootModule` defines configuration flags for the `FileResolver` which was moved from
  `finatra/http` to a more correctly generic location in `finatra/utils`. However, configuration for
  injection of a properly configured `FileResolver` is still incorrectly tied to HTTP because of the
  `DocRootModule`. Thus, we deprecate the `DocRootModule` and introduce the
  `c.t.finatra.modules.FileResolverModule` which is defined closer to the
  `c.t.finatra.utils.FileResolver` in `finatra/utils`. This allows the `FileResolver` to be properly
  configured outside of HTTP concerns. ``PHAB_ID=D390932``

* finatra-thrift: Updated BUILD files for Pants 1:1:1 layout. ``PHAB_ID=D388297``

* inject-ports: Add `finatra/inject/inject-ports` which has `c.t.inject.server.Ports` and
  `c.t.inject.server.PortUtils`. ``PHAB_ID=D388277``

* inject-utils: Move `AnnotationUtils` to `c.t.inject.utils.AnnotationUtils` and make public
  for use. ``PHAB_ID=D388241``

* finatra-http: Updated package structure for Pants 1:1:1 layout. Moved `META-INF/mime.types` file
  to finatra/utils which is where the `FileResolver` is located for proper resolution of mime types
  from file extension. ``PHAB_ID=D385792``

19.10.0
-------

Changed
~~~~~~~

* finatra-jackson: Update jackson reflection to use `org.json.reflect` instead of
  custom reflection. This enables support for parsing case classes defined over generic
  types, e.g., `case class Page[T](data: T)`. As a result of this change, use of `lazy val`
  for trait members which are mixed into case classes for use in deserialization is no
  longer supported. This addresses issue #480. ``PHAB_ID=D368822``

Fixed
~~~~~

* finatra-jackson: Add support for parsing of case classes defined over generic types
  (even nested, and multiple), e.g., `case class Page[T, U] (data: List[T], column: U)`.
  Fixes issue #408. ``PHAB_ID=D368822``

* finatra-kafka: Sanitize topic name in MonitoringConsumer stats scope
  ``PHAB_ID=D373402``

* inject-server: Fix printing of all stats from the underlying `InMemoryStatsReceiver` in
  the `eventually` loop for stat assertion. Address finatra/kafka test logging for
  finatra/kakfa-streams/kafka-streams and finatra/kafka. ``PHAB__ID=D372108``

* inject-logback: A `NullReferenceException` could be thrown during metrics
  collection due to an incorrect logback.xml configuration. This has been fixed.
  ``PHAB_ID=D369234``

19.9.0
------

Added
~~~~~

* finatra-kafka: Add `withConfig` method variant which takes a `Map[String, String]`
  to allow for more complex configurations ``PHAB_ID=D354389``

Changed
~~~~~~~

* finatra: Remove commons-lang as a dependency and replace it with alternatives from stdlib
  when possible. ``PHAB_ID=D354013``

* inject-server: Changed `c.t.inject.server.InMemoryStatsReceiverUtility` to show the expected and
  actual values as part of the error message when metric values do not match. ``PHAB_ID=D360470``

* finatra-kafka-streams: Improve StaticPartitioning error message ``PHAB_ID=D351368``

Fixed
~~~~~

* finatra-http: Support Http 405 response code, improve routing performance for non-constant route
  ``PHAB_ID=D278146``

* inject-app: Update `c.t.inject.app.App` to only recurse through modules once. We currently
  call `TwitterModule#modules` more than once in reading flags and parsing the list of modules
  over which to create the injector. When `TwitterModule#modules` is a function that inlines the
  instantiation of new modules we can end up creating multiple instances causing issues with the
  list of flags defined in the application. This is especially true in instances of `TwitterModule`
  implemented in Java as there is no way to implement the trait `TwitterModule#modules` method as a
  eagerly evaluated value. We also don't provide an ergonomic method for Java users to define
  dependent modules like we do in apps and servers via `App#javaModules`. Thus we also add a
  `TwitterModule#javaModules` function which expresses a better API for Java users. ``PHAB_ID=D349587``

Closed
~~~~~~

19.8.0
------

Added
~~~~~

* finatra-http: Introduce the new streaming request and response types:
  `c.t.finatra.http.streaming.StreamingRequest` ``PHAB_ID=D342728``,
  `c.t.finatra.http.streaming.StreamingResponse` ``PHAB_ID=D342703``.
  Examples are located in finatra/examples/streaming-example/.

* finatra-jackson: Add the ability to specify `fields` in the `MethodValidation` annotation.
  ``PHAB_ID=D338079``

Changed
~~~~~~~
* finatra-kafka: Make KafkaConsumerConfig config public from FinagleKafkaConsumerBuilder. ``PHAB_ID=D362058``

* inject-thrift-client: make ThriftClientModuleTrait extend StackClientModuleTrait for symmetry
  with other protocol client modules. ``PHAB_ID=D342710``

* finatra-http: Deprecated `c.t.finatra.http.response.StreamingResponse`, Use
  `c.t.finatra.http.response.ResponseBuilder.streaming` to construct a
  `c.t.finatra.http.streaming.StreamingResponse` instead. ``PHAB_ID=D342703``

* finatra: Upgrade to Jackson 2.9.9. ``PHAB_ID=D345969``

19.7.0
------

Added
~~~~~

* finatra-kafka-streams: Adding test/sample for `FinatraDslWindowedAggregations.aggregate`. ``PHAB_ID=D322558``

* finatra-jackson: Add `com.twitter.util.Time` deserializer with `JsonFormat` support.
  ``PHAB_ID=D330682``

Changed
~~~~~~~

* finatra-kafka: BUILD file update compile and runtime deps.
  ``PHAB_ID=D337742``

* finatra-httpclient: introduce new `HttpClientModuleTrait` and deprecate `HttpClientModule`.
  The `HttpClientModule` has been modified to extend from `HttpClientModuleTrait` to allow
  for bridging the two implementations. `c.t.f.httpclient.RichHttpClient` has also been deprecated
  as part of this change. The new `HttpClientModuleTrait` allows for direct configuration of the
  underling `c.t.finagle.Http.Client`. The new `HttpClientModuleTrait` does not provide any
  default bindings, so it is up to users to supply them - this allows for custom binding
  annotations and binding multiple `HttpClient`s, which was not previously possible with
  `HttpClientModule`. ``PHAB_ID=D338320``

  To migrate,

  ```
  class MyHttpClientModule extends HttpClientModule {
    override val dest = "flag!mydest"
    override val sslHostname = Some("sslHost")
  }

  ```

  becomes

  ```
  class MyHttpClientModule extends HttpClientModuleTrait {
    override val dest = "flag!mydest"
    override val label = "myhttpclient"
    val sslHostname = "sslHost"

    // we only override in this example for TLS configuration with the `sslHostname`
    override def configureClient(
      injector: Injector,
      client: Http.Client
    ): Http.Client = client.withTls(sslHostname)

    @Singleton
    @Provides
    final def provideHttpClient(
      injector: Injector,
      statsReceiver: StatsReceiver,
      mapper: FinatraObjectMapper
    ): HttpClient = newHttpClient(injector, statsReceiver, mapper)

    // Note that `provideHttpClient` no longer needs an injected `Service[Request, Response]` so
    // the following is only needed if you require a `Service[Request, Response]` elsewhere:

    @Singleton
    @Provides
    final def provideHttpService(
      injector: Injector,
      statsReceiver: StatsReceiver
    ): Service[Request, Response] = newService(injector, statsReceiver)

  }
  ```

19.6.0
------

Added
~~~~~

* inject-modules: Introduce a `StackClientModuleTrait` to aid in configuring modules for generic
  Finagle Stack Clients. ``PHAB_ID=D324891``

* finatra-http: Add `c.t.inject.server.InMemoryStatsReceiverUtility` which allows for testing
  assertions on metrics captured within an embedded server's `InMemoryStatsReceiver`. Update the
  Kafka tests and utilities to use the `InMemoryStatsReceiverUtility` and mark the
  `c.t.finatra.kafka.test.utilsInMemoryStatsUtil` as deprecated. ``PHAB_ID=D316806``

Changed
~~~~~~~

* finatra-http: Removed deprecated `response_size` stat from `c.t.finatra.http.filters.StatsFilter`.
  ``PHAB_ID=D328254``

* finatra-kafka: Update finatra exported metrics to contains KafkaMetrics 'rocksdb-window-state-id'.
  ``PHAB_ID=D326320``

* finatra-kafka: Deprecate in `c.t.finatra.kafka.consumers.FinagleKafkaConsumer`.
  Add `c.t.finatra.kafka.consumers.FinagleKafkaConsumer.buildClient` and
  `c.t.finatra.kafka.producters.FinagleKafkaProducer.buildClient`. ``PHAB_ID=D321699``

Fixed
~~~~~

* finatra: Add an explicit dependency on `com.sun.activation` to allow for using
  Finatra with JDK 11. This fixes #484. ``PHAB_ID=D328724``

19.5.1
------

Fixed
~~~~~

* finatra: The added `c.t.finatra.http.RouteHint` was missing from the test-jar sources and has
  been added. ``PHAB_ID=D317282``

19.5.0
------

Added
~~~~~

* inject-server/http/thrift: Allow users to specify a `StatsReceiver` implementation to use in the
  underlying `EmbeddedTwitterServer` instead of always providing an `InMemoryStatsReceiver`
  implementation. ``PHAB_ID=D315440``

* finatra-http: Add ability for Java HTTP Controllers to use the RouteDSL for per-route filtering
  and for route prefixing. ``PHAB_ID=D311625``

* inject-request-scope: Add a `Filter.TypeAgnostic` implementation for the `FinagleRequestScopeFilter`
  for better compatibility with Thrift servers. Update the `FinagleRequestScope` to make more idiomatic
  use of `Context` locals. ``PHAB_ID=D310395``

* finatra-http: Route params are now URL-decoded automatically. ``PHAB_ID=D309144``

* finatra-jackson: Add ability to bypass case class validation using the
  `NullValidationFinatraJacksonModule`. ``PHAB_ID=D307795``

* inject-app: Add `c.t.inject.app.DtabResolution` to help users apply supplemental Dtabs added by
  setting the dtab.add flag. This will append the supplemental Dtabs to the
  Dtab.base in a premain function. ``PHAB_ID=D303813``

Changed
~~~~~~~

* finatra-http: Move when admin routes are added to the AdminHttpServer to the `postInjectorStartup`
  phase, such that any admin routes are available to be hit during server warmup. Simplify `HttpWarmup`
  utility to make it clear that it can and should only be used for sending requests to endpoints added
  to the server's configured `HttpRouter`. The `forceRouteToAdminHttpMuxers` param has been renamed
  to `admin` to signal that the request should be sent to the `HttpRouter#adminRoutingService` instead
  of the `HttpRouter#externalRoutingService`. Routing to TwitterServer HTTP Admin Interface via this
  utility never worked properly and the (broken) support has been dropped. ``PHAB_ID=D314152``

* finatra-kafka: Update `com.twitter.finatra.kafka.test.KafkaTopic`, and
  `com.twitter.finatra.kafka.test.utils.PollUtils` methods to take
  `com.twitter.util.Duration` instead of `org.joda.time.Duration`. ``PHAB_ID=D314958``

* finatra: Removed Commons IO as a dependency. ``PHAB_ID=D314606``

* finatra-http: `com.twitter.finatra.http.EmbeddedHttpServer` methods which previously used the
  `routeToAdminServer` parameter have been changed to use a `RouteHint` instead for added
  flexibility in controlling where a test request is sent. ``PHAB_ID=D313984``

* finatra-inject: Feature tests no longer default to printing metrics after tests.
  This can be enabled on a per-test basis by overriding `FeatureTestMixin.printStats`
  and setting it to `true`. ``PHAB_ID=D314329``

* finatra-inject: Update `com.twitter.inject.utils.RetryPolicyUtils`,
  `com.twitter.inject.thrift.modules.FilteredThriftClientModule`,  and
  `com.twitter.inject.thrift.filters.ThriftClientFilterChain` methods to take
  `com.twitter.util.Duration` instead of `org.joda.time.Duration`. ``PHAB_ID=D313153``

* finatra: Fix Commons FileUpload vulnerability. Update `org.apache.commons-fileupload` from version
  1.3.1 to version 1.4. This closes #PR-497. ``PHAB_ID=D310470``

* finatra-http: Replace all usages of guava's `com.google.common.net.MediaType` with `String`.
  You can migrate by calling `MediaType#toString` everywhere you passed a `MediaType` before.  ``PHAB_ID=D308761``

* finatra-http: Add `http` scope to `shutdown.time` flag, making it `http.shutdown.time`.
  ``PHAB_ID=D307552``

* finatra-http: Remove deprecated `DefaultExceptionMapper`. Extend
  `c.t.finatra.http.exceptions.ExceptionMapper[Throwable]` directly instead. ``PHAB_ID=D307520``

* inject-app: Move override of `com.twitter.app.App#failfastOnFlagsNotParsed` up from
  `c.t.inject.server.TwitterServer` to `com.twitter.inject.app.App` such that all Finatra-based
  applications default to this behavior. ``PHAB_ID=D307858``

* inject-app|server: change capturing of flag ordering from Modules for adding to the App's `c.t.app.Flags`
  instance to match the semantics of directly calling `c.t.app.Flags#add`. Prefer `AtomicBoolean`
  instances over where we currently use mutable `Boolean` instances in `c.t.inject.app.App`, `c.t.inject.app.TestInjector`,
  and `c.t.inject.server.EmbeddedTwitterServer`. ``PHAB_ID=D306897``

* finatra-examples: Update "twitter-clone" example to use `Dtabs` instead of the deprecated `resolverMap`.
  Move the "hello-world" example to "http-server". ``PHAB_ID=D303813``

Fixed
~~~~~

* finatra-jackson: Properly account for timezone in Joda `DateTime` deserialization. ``PHAB_ID=D312027``

 * finatra-http: `EmbeddedHttpServer`'s `httpGetJson` method now properly passes
   all parameters through to the underlying client call. ``PHAB_ID=D312151``

Closed
~~~~~~

19.4.0
------

Added
~~~~~

* inject-server: Add `globalFlags` argument to EmbeddedTwitterServer, which will
  allow for scoping a `c.t.a.GlobalFlag` property change to the lifecycle of the
  underlying TwitterServer, as a `c.t.a.GlobalFlag` is normally scoped to the JVM/process.
  This change is also reflected in `EmbeddedHttpServer` and `EmbeddedThriftServer` constructors.
  ``PHAB_ID=D288032``

* inject-utils: add `toOrderedMap` implicit conversion for `java.util.Map` ``PHAB_ID=D295005``

* finatra-kafka-streams: Add flag `rocksdb.manifest.preallocation.size` with default value
  `4.megabytes` to `c.t.f.k.c.RocksDbFlags` and set value in
  `c.t.f.k.c.FinatraRocksDBConfig`. ``PHAB_ID=D290130``

* finatra-http: Add `commaSeparatedList` boolean parameter to QueryParams, for
  parsing comma-separated query parameters into collection types. ``PHAB_ID=D268989``

Changed
~~~~~~~

* finatra: Update snakeyaml to version 1.24. ``PHAB_ID=D232547``

* finatra-kafka: Upgraded kafka libraries from 2.0.0 to 2.2.0.
  - `Kafka 2.0.1 Release Notes <https://archive.apache.org/dist/kafka/2.0.1/RELEASE_NOTES.html>`__
  - `Kafka 2.1.0 Release Notes <https://archive.apache.org/dist/kafka/2.1.0/RELEASE_NOTES.html>`__
  - `Kafka 2.1.1 Release Notes <https://archive.apache.org/dist/kafka/2.1.1/RELEASE_NOTES.html>`__
  - `Kafka 2.2.0 Release Notes <https://archive.apache.org/dist/kafka/2.2.0/RELEASE_NOTES.html>`__
  ``PHAB_ID=D248171``

* finatra-thrift: Removed `c.t.finatra.thrift.exceptions.FinatraThriftExceptionMapper`,
  `c.t.finatra.thrift.filters.ClientIdAcceptlistFilter`,
  `c.t.finatra.thrift.modules.ClientIdAcceptlistModule`,
  `c.t.finatra.thrift.filters.ClientIdWhitelistFilter`,
  `c.t.finatra.thrift.modules.ClientIdWhitelistModule`,
  and the `finatra/finatra_thrift_exceptions.thrift` IDL. ``PHAB_ID=D278231``

* finatra-thrift: Constructing a `ThriftRouter` now requires `serverName`. ``PHAB_ID=D294345``

* finatra-examples: Updated `StreamingController` to use `Reader` instead of `AsyncStream`
  ``PHAB_ID=D295227``

* finatra-kafka-streams: Implement FinatraKeyValueStore as custom store. ``PHAB_ID=D277612``

* finatra-thrift: Constructing a `ThriftRouter` now requires `c.t.f.StackTransformer`.
  ``PHAB_ID=D277493``

Fixed
~~~~~

 * finatra-kafka: Ensure that `EmbeddedKafka` implementation of `beforeAll()` makes
   call to `super.beforeAll()` so hooks registered in super class get executed. ``PHAB_ID=D296643``

 * finatra-kafka-streams: `FinatraTransformer.timerStore` config object references immutable
   map which causes exception thrown if user code calls `AbstractStoreBuilder.withLoggingDisabled`.
   Fixed `FinatraTransformer.timerStore` to convert from immutable map to mutable map before
   forwarding config object to kafka library. ``PHAB_ID=D293979``

19.3.0
------

Added
~~~~~

* finatra-kafka-streams: Exposing additional consumer configuration flags.
  - `kafka.consumer.max.poll.interval.ms` Maximum delay between invocations of poll() when using
  consumer group management.
  - `kafka.consumer.max.partition.fetch.bytes` Consumer's maximum amount of data per-partition the
  server will return.
  - `kafka.consumer.request.timeout.ms` Consumer's maximum amount of time to wait for the response
  of a request.
  - `kafka.consumer.connections.max.idle.ms` Consumer's maximum idle time for connections before
  closing the connection.
  ``PHAB_ID=D287371``

* finatra-kafka: FinagleKafka clients pass correct deadline for close to
  underlying Kafka clients. ``PHAB_ID=D261115``

* finatra-kafka-streams: (BREAKING API CHANGE) Create flags for common consumer and producer
  configs. KafkaFlagUtils#kafkaDocumentation and getKafkaDefault are no longer public methods.
  ``PHAB_ID=D277044``

* finatra-kafka: Added support to fetch end offset for a given partition. ``PHAB_ID=D283813``

* finatra-http: Added `HttpServerTrait` which allows for a simple way to serve a
  Finagle `Service[Request, Response]` on an external interface without the need to
  configure the Finatra `HttpRouter`. ``PHAB_ID=D280896``

* finatra-http: Added support to serve `c.t.io.Reader` as a streaming request.
  ``PHAB_ID=D278988``

Changed
~~~~~~~

* finatra-kafka-streams: finatra-kafka-streams: Refactor queryable state management  ``PHAB_ID=D277594``

* finatra-kafka-streams: Improve querying of windowed stores. ``PHAB_ID=D277553``

* inject-utils: Mark `c.t.inject.utils.StringUtils#snakify,camelify,pascalify` as
  deprecated as their implementations have moved to util/util-core `c.t.conversions.StringOps`.
  Encourage users to switch usages to `c.t.conversions.StringOps#toSnakeCase,toCamelCase,toPascalCase`.
  ``PHAB_ID=D280886``

* finatra-thrift: Changed `c.t.finatra.thrift.ThriftServerTrait#service` to `#thriftService` to
  not collide with the serving of a Finagle service from the `HttpServer` when a server extends
  both `HttpServer` and `ThriftServer`. ``PHAB_ID=D280896``

Fixed
~~~~~

Closed
~~~~~~

19.2.0
------

Added
~~~~~

* finatra-kafka: Expose timeout duration in FinagleKafkaConsumerBuilder dest(). ``PHAB_ID=D269701``

* finatra-kafka-streams: Expose all existing RocksDb configurations. See
  `c.t.f.k.config.FinatraRocksDBConfig` for details on flag names,
  descriptions and default values. ``PHAB_ID=D272068``

* finatra-kafka-streams: Added two RocksDB flags related to block cache tuning,
  `cache_index_and_filter_blocks` and `pin_l0_filter_and_index_blocks_in_cache`.
  ``PHAB_ID=D269516``

* finatra-kafka: Adding an implicit implementation of
  `c.t.app.Flaggable[c.t.finatra.kafka.domain.SeekStrategy]`
  and `c.t.app.Flaggable[org.apache.kafka.clients.consumer.OffsetResetStrategy]`.
  ``PHAB_ID=D271098``

* finatra-http: Added support to serve `c.t.io.Reader` as a streaming response in
  `c.t.finatra.http.internal.marshalling.CallbackConverter`. ``PHAB_ID=D266863``

* finatra-kafka: Expose endOffsets() in FinagleKafkaConsumer. ``PHAB_ID=D263573``

* finatra-kafka-streams: Adding missing ScalaDocs. Adding metric for elapsed state
  restore time. RocksDB configuration now contains a flag for adjusting the number
  of cache shard bits, `rocksdb.block.cache.shard.bits`. ``PHAB_ID=D255771``

* finatra-jackson: Added @Pattern annotation to support finatra/jackson for regex pattern
  validation on string values. ``PHAB_ID=D259719``

Changed
~~~~~~~
* finatra-kafka-streams: Refactor package names. All classes moved from
  com.twitter.finatra.streams to com.twitter.finatra.kafkastreams. ``PHAB_ID=D268027``

* finatra-kafka-streams: Delete deprecated and unused classes. ``PHAB_ID=D267921``

* finatra-kafka-streams: `c.t.finatra.streams.transformer.domain.Time` is now the canonical
   representation of time for watermarks and timers. `RichLong` implicit from
   `com.twitter.finatra.streams.converters.time` has been renamed to `RichFinatraKafkaStreamsLong`.
   ``PHAB_ID=D255736``

* finatra-jackson: Fix `CaseClassField` annotation reflection for Scala 2.12. ``PHAB_ID=D264423``

* finatra-kafka-streams: Combine FinatraTransformer with FinatraTransformerV2. ``PHAB_ID=D254411``

* finatra-thrift: The return type of `ReqRepDarkTrafficFilterModule#newFilter` has been changed from
  `DarkTrafficFilter[MethodIface]` to `Filter.TypeAgnostic`. ``PHAB_ID=D261868``

* finatra-kafka: Add lookupBootstrapServers function that takes timeout as a parameter.
  ``PHAB_ID=D256997``

* finatra-thrift: If a Controller is not configured with exactly one endpoint
  per method, it will throw an AssertionError instead of logging an error message.
  An attempt to use non-legacy functionality with a legacy Controller will throw
  an AssertionError. ``PHAB_ID=D260230``

* finatra-kafka: Add flags for controlling rocksdb internal LOG file growth.
  - `rocksdb.log.info.level` Allows the setting of rocks log levels
    `DEBUG_LEVEL`, `INFO_LEVEL`, `WARN_LEVEL`, `ERROR_LEVEL`, `FATAL_LEVEL`,
    `HEADER_LEVEL`.
  - `rocksdb.log.max.file.size` The maximal size of the info log file.
  - `rocksdb.log.keep.file.num` Maximal info log files to be kept.
  ``PHAB_ID=D259579``

* finatra-kafka: Add admin routes for properties and topology information
  - `/admin/kafka/streams/properties` Dumps the
    `KafkaStreamsTwitterServer#properties` as plain text in the TwitterServer
    admin page.
  - `/admin/kafka/streams/topology` Dumps the
    `KafkaStreamsTwitterServer#topology` as plain text in the TwitterServer
    admin page.
  ``PHAB_ID=D259597``

* inject-server: EmbeddedTwitterServer that fails to start will now continue to
  throw the startup failure on calls to methods that require a successfully started server.
  ``PHAB_ID=D265543``

Fixed
~~~~~

* finatra-kafka-streams: `FinatraTopologyTester` did not set
  `TopologyTestDriver#initialWallClockTimeMs` on initialization causing diverging wall clock time
  when `TopologyTestDriver#advanceWallClockTime` advanced time. The divergence was between
  system time set by `org.joda.time.DateTimeUtils.setCurrentMillisFixed` and internal mock timer
  `TopologyTestDriver#mockWallClockTime`. `FinatraTopologyTester.inMemoryStatsReceiver` is reset on
  `TopologyFeatureTest#beforeEach` for all test that extend `TopologyFeatureTest`.
  ``PHAB_ID=D269013``

* finatra-kafka-streams: Improve watermark assignment/propagation upon reading the first
  message and when caching key value stores are used. ``PHAB_ID=D262054``

* finatra-jackson: Support inherited annotations in case class deserialization. Case class
  deserialization support does not properly find inherited Jackson annotations. This means
  that code like this:

  ```
  trait MyTrait {
    @JsonProperty("differentName")
    def name: String
  }
  case class MyCaseClass(name: String) extends MyTrait
  ```

  would not properly expect an incoming field with name `differentName` to parse into the
  case class `name` field. This commit provides support for capturing inherited annotations
  on case class fields. Annotations processed in order, thus if the same annotation appears
  in the class hierarchy multiple times, the value configured on the class will win otherwise
  will be in the order of trait linearization with the "last" declaration prevailing.
  ``PHAB_ID=D260376``

* finatra: Remove extraneous dependency on old `javax.servlet` ServletAPI dependency.
  The fixes #478. ``PHAB_ID=D259671``

Closed
~~~~~~

19.1.0
------

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
