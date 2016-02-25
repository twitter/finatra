# Change Log

## [v2.1.4](https://github.com/twitter/finatra/tree/v2.1.4) (2016-02-25)

[Full Changelog](https://github.com/twitter/finatra/compare/v2.1.3...2.1.4)

**Fixed bugs:**

- Some Scaladoc links are broken on twitter.github.io/finatra [\#298](https://github.com/twitter/finatra/issues/298)

**Closed issues:**

- LoggingMDCFilter lacks documentation [\#303](https://github.com/twitter/finatra/issues/303)

- bug in finatra/examples/hello-world/src/main/resources/logback.xml [\#289](https://github.com/twitter/finatra/issues/289)

- Improve error message when @Header field is missing [\#263](https://github.com/twitter/finatra/issues/263)

## [v2.1.3](https://github.com/twitter/finatra/tree/v2.1.3) (2016-02-05)

[Full Changelog](https://github.com/twitter/finatra/compare/v2.1.2...v2.1.3)

**Closed issues:**

- Is it possible to have different modules listen in different ports? [\#295](https://github.com/twitter/finatra/issues/295)

- Asynchronous method validations [\#292](https://github.com/twitter/finatra/issues/292)

- if the Cookie contain version='' ,can't get the request.cookies [\#290](https://github.com/twitter/finatra/issues/290)

- Failed to auto configure default logger context [\#288](https://github.com/twitter/finatra/issues/288)

- Inject properties [\#287](https://github.com/twitter/finatra/issues/287)

- sbt compile error on master [\#284](https://github.com/twitter/finatra/issues/284)

- Optionally announce server location on startup [\#241](https://github.com/twitter/finatra/issues/241)

## [v2.1.2](https://github.com/twitter/finatra/tree/v2.1.2) (2015-12-09)

[Full Changelog](https://github.com/twitter/finatra/compare/v2.1.1...v2.1.2)

**Fixed bugs:**

- Missing Scaladoc [\#279](https://github.com/twitter/finatra/issues/279)

**Closed issues:**

- Finatra + Protobuf [\#277](https://github.com/twitter/finatra/issues/277)

- Simple hello-world example does not compiled [\#274](https://github.com/twitter/finatra/issues/274)

- Allow overriding of the http service name [\#270](https://github.com/twitter/finatra/issues/270)

- Bump to latest finagle? [\#266](https://github.com/twitter/finatra/issues/266)

- ClassCastException: com.twitter.finatra.logging.FinagleMDCAdapter cannot be cast to ch.qos.logback.classic.util.LogbackMDCAdapter [\#256](https://github.com/twitter/finatra/issues/256)

## [v2.1.1](https://github.com/twitter/finatra/tree/v2.1.1) (2015-10-29)

[Full Changelog](https://github.com/twitter/finatra/compare/v2.1.0...v2.1.1)

**Closed issues:**

- Update Startup Test on doc [\#261](https://github.com/twitter/finatra/issues/261)

- Error with simple test using httpPutJson [\#257](https://github.com/twitter/finatra/issues/257)

- appfrog problem with admin server, I only can use one port [\#252](https://github.com/twitter/finatra/issues/252)

- Streaming content every X seconds [\#250](https://github.com/twitter/finatra/issues/250)

- Mustache templates getting stripped  [\#112](https://github.com/twitter/finatra/issues/112)

**Merged pull requests:**

- Remove unneccesary files [\#265](https://github.com/twitter/finatra/pull/265) ([cacoco](https://github.com/cacoco))

## [v2.1.0](https://github.com/twitter/finatra/tree/v2.1.0) (2015-10-01)

[Full Changelog](https://github.com/twitter/finatra/compare/v2.0.1...v2.1.0)

**Merged pull requests:**

- finatra/inject - Rename InjectUtils to more specific PoolUtils [\#258](https://github.com/twitter/finatra/pull/258) ([cacoco](https://github.com/cacoco))

## [v2.0.1](https://github.com/twitter/finatra/tree/v2.0.1) (2015-09-21)

[Full Changelog](https://github.com/twitter/finatra/compare/v2.0.0...v2.0.1)

**Closed issues:**

- Split code into packages/modules [\#254](https://github.com/twitter/finatra/issues/254)

- Support for Scala Future's [\#249](https://github.com/twitter/finatra/issues/249)

- Override TwitterModule in FeatureTest [\#233](https://github.com/twitter/finatra/issues/233)

**Merged pull requests:**

- Update TweetsControllerIntegrationTest.scala [\#251](https://github.com/twitter/finatra/pull/251) ([scosenza](https://github.com/scosenza))

- Update Travis CI to build with java8 fix. [\#244](https://github.com/twitter/finatra/pull/244) ([cacoco](https://github.com/cacoco))

## [v2.0.0](https://github.com/twitter/finatra/tree/v2.0.0) (2015-09-09)

[Full Changelog](https://github.com/twitter/finatra/compare/v2.0.0.M2...v2.0.0)

**Closed issues:**

- Singleton classes [\#236](https://github.com/twitter/finatra/issues/236)

- com.twitter.finatra.utils.ResponseUtils for 2.0.0.M2 missing functions used in examples [\#235](https://github.com/twitter/finatra/issues/235)

- Warmup example in README seems to be using non-existent features [\#234](https://github.com/twitter/finatra/issues/234)

- Unable to resolve finatra-slf4j artifact [\#232](https://github.com/twitter/finatra/issues/232)

- Unable to resolve some of the dependencies :\( [\#231](https://github.com/twitter/finatra/issues/231)

- How to render static webpage in finatra2 [\#230](https://github.com/twitter/finatra/issues/230)

- When running a FeatureTest a lot of data is dumped to stdout and stderr [\#226](https://github.com/twitter/finatra/issues/226)

- Mapping a header by name to a case class requires additional metadata [\#225](https://github.com/twitter/finatra/issues/225)

- Missing scaladoc documentation [\#221](https://github.com/twitter/finatra/issues/221)

- finatra-hello-world does not compile [\#219](https://github.com/twitter/finatra/issues/219)

- Add tags for Finatra 1.6.0 and 1.5.4 [\#216](https://github.com/twitter/finatra/issues/216)

- FeatureTest withJsonBody not working [\#215](https://github.com/twitter/finatra/issues/215)

- Disable admin [\#208](https://github.com/twitter/finatra/issues/208)

- Regexes in paths for route definitions [\#197](https://github.com/twitter/finatra/issues/197)

- AppService doesn't support POST of JSON containing % and then & [\#173](https://github.com/twitter/finatra/issues/173)

- fatjar includes unexpected assets in the public directory [\#147](https://github.com/twitter/finatra/issues/147)

- allow subclassing of request [\#116](https://github.com/twitter/finatra/issues/116)

- Builtin Compressor for static files \(css/js\) [\#113](https://github.com/twitter/finatra/issues/113)

- bring back controller prefixes [\#104](https://github.com/twitter/finatra/issues/104)

- code coverage stats [\#98](https://github.com/twitter/finatra/issues/98)

- Add Aurora/Mesos support [\#94](https://github.com/twitter/finatra/issues/94)

- Simplify Cookie API with a CookieBuilder [\#93](https://github.com/twitter/finatra/issues/93)

- implement a routes.txt in admin [\#80](https://github.com/twitter/finatra/issues/80)

- support ETAGS and/or Cache-Control headers in file server [\#73](https://github.com/twitter/finatra/issues/73)

- asset pipeline filter [\#62](https://github.com/twitter/finatra/issues/62)

**Merged pull requests:**

- Scosenza update readmes [\#242](https://github.com/twitter/finatra/pull/242) ([scosenza](https://github.com/scosenza))

- Update warmup docs [\#238](https://github.com/twitter/finatra/pull/238) ([scosenza](https://github.com/scosenza))

- Change Google Analytics tracking to use Twitter OSS account [\#217](https://github.com/twitter/finatra/pull/217) ([travisbrown](https://github.com/travisbrown))

## [v2.0.0.M2](https://github.com/twitter/finatra/tree/v2.0.0.M2) (2015-06-12)

[Full Changelog](https://github.com/twitter/finatra/compare/v2.0.0.M1...v2.0.0.M2)

**Closed issues:**

- Issue with POST request [\#214](https://github.com/twitter/finatra/issues/214)

- error running example with sbt run: overloaded method value settings with alternatives. [\#207](https://github.com/twitter/finatra/issues/207)

- Was the 1.5.3 release retagged? [\#206](https://github.com/twitter/finatra/issues/206)

- Finatra 1.5.3 and dependencies at Travis CI [\#205](https://github.com/twitter/finatra/issues/205)

- Add an ADOPTERs.md [\#204](https://github.com/twitter/finatra/issues/204)

- connect finagle filter to specific controller [\#203](https://github.com/twitter/finatra/issues/203)

- Does Finatra support Scala 2.11? [\#196](https://github.com/twitter/finatra/issues/196)

- Support multipart PUT requests [\#194](https://github.com/twitter/finatra/issues/194)

-  Content-type custom settings do not work when render json [\#191](https://github.com/twitter/finatra/issues/191)

- FlatSpecHelper dependency missing in finagle 1.6.0 [\#189](https://github.com/twitter/finatra/issues/189)

- Allow other logging handlers [\#187](https://github.com/twitter/finatra/issues/187)

- ErrorHandler used by ControllerCollection depends on order Controllers are added [\#182](https://github.com/twitter/finatra/issues/182)

- Deployment for newly generated project does not work on heroku [\#180](https://github.com/twitter/finatra/issues/180)

- finatra doc typo [\#174](https://github.com/twitter/finatra/issues/174)

- Admin interface is showing a blank page. [\#171](https://github.com/twitter/finatra/issues/171)

- Update to scala 2.11.x [\#159](https://github.com/twitter/finatra/issues/159)

- Missing static resources report 500 Internal Server Error [\#157](https://github.com/twitter/finatra/issues/157)

- flag values are not resolved until server starts [\#148](https://github.com/twitter/finatra/issues/148)

- docs are wrong about default template path [\#143](https://github.com/twitter/finatra/issues/143)

- Static files can`t be found if finatra server starts at Windows [\#130](https://github.com/twitter/finatra/issues/130)

- Add support for parsing JSON request body [\#129](https://github.com/twitter/finatra/issues/129)

- Add test for unicode content-length [\#122](https://github.com/twitter/finatra/issues/122)

- Expose logger without having to include App and Logger traits in every class [\#121](https://github.com/twitter/finatra/issues/121)

- Make View class generic [\#118](https://github.com/twitter/finatra/issues/118)

- premain docs [\#114](https://github.com/twitter/finatra/issues/114)

- allow registration of custom jackson modules [\#110](https://github.com/twitter/finatra/issues/110)

- Add CONTRIBUTING.md [\#109](https://github.com/twitter/finatra/issues/109)

- expose server ip at startup time [\#108](https://github.com/twitter/finatra/issues/108)

- explore dynamic routing [\#103](https://github.com/twitter/finatra/issues/103)

- implement rails-like "flash" [\#100](https://github.com/twitter/finatra/issues/100)

- CSRF Support [\#89](https://github.com/twitter/finatra/issues/89)

- Session support [\#88](https://github.com/twitter/finatra/issues/88)

- Configurable Key/Value store [\#87](https://github.com/twitter/finatra/issues/87)

- apache-like directory browser for files [\#54](https://github.com/twitter/finatra/issues/54)

- benchmark suite with caliper [\#45](https://github.com/twitter/finatra/issues/45)

- RequestAdapter does not support multiple values for query params [\#22](https://github.com/twitter/finatra/issues/22)

**Merged pull requests:**

- Update README.md [\#202](https://github.com/twitter/finatra/pull/202) ([scosenza](https://github.com/scosenza))

## [v2.0.0.M1](https://github.com/twitter/finatra/tree/v2.0.0.M1) (2015-04-30)

[Full Changelog](https://github.com/twitter/finatra/compare/1.6.0...v2.0.0.M1)

**Closed issues:**

- UNRESOLVED DEPENDENCIES [\#199](https://github.com/twitter/finatra/issues/199)

- Changing port breaks embedded static file server [\#192](https://github.com/twitter/finatra/issues/192)

- Finatra cannot be built when Finagle's version \> 6.13.0 [\#153](https://github.com/twitter/finatra/issues/153)

**Merged pull requests:**

- 2.0.0.M1 [\#200](https://github.com/twitter/finatra/pull/200) ([cacoco](https://github.com/cacoco))

## [1.6.0](https://github.com/twitter/finatra/tree/1.6.0) (2015-01-08)

[Full Changelog](https://github.com/twitter/finatra/compare/1.5.4...1.6.0)

**Closed issues:**

- Finatra 1.5.4 with finagle-stats 6.22.0 throws an exception [\#184](https://github.com/twitter/finatra/issues/184)

- Document unit testing controllers by using MockApp [\#178](https://github.com/twitter/finatra/issues/178)

- maven.twttr.com not showing finatra [\#175](https://github.com/twitter/finatra/issues/175)

- Finatra 1.5.4 java.lang.RuntimeException with Finagle 6.22.0 [\#172](https://github.com/twitter/finatra/issues/172)

- Error while pushing on Heroku [\#170](https://github.com/twitter/finatra/issues/170)

- Finatra closes connection [\#161](https://github.com/twitter/finatra/issues/161)

- Spec test doesn't populate multiParams [\#155](https://github.com/twitter/finatra/issues/155)

- RequestAdapter fails to decode non-multipart POSTs [\#154](https://github.com/twitter/finatra/issues/154)

**Merged pull requests:**

- FIX: issue \#182, let controller's error handler handle its own errors. [\#188](https://github.com/twitter/finatra/pull/188) ([plaflamme](https://github.com/plaflamme))

- Update to use new Travis CI infrastructure [\#186](https://github.com/twitter/finatra/pull/186) ([caniszczyk](https://github.com/caniszczyk))

- Refactor FinatraServer to allow custom tlsConfig [\#183](https://github.com/twitter/finatra/pull/183) ([bpfoster](https://github.com/bpfoster))

- Fix heroku deployments for template project [\#181](https://github.com/twitter/finatra/pull/181) ([tomjadams](https://github.com/tomjadams))

- remove dependency on scalatest [\#179](https://github.com/twitter/finatra/pull/179) ([c089](https://github.com/c089))

- Update to twitter-server 1.8.0 \(finagle 6.22.0\) [\#176](https://github.com/twitter/finatra/pull/176) ([bpfoster](https://github.com/bpfoster))

- Add an apache style directory browser [\#169](https://github.com/twitter/finatra/pull/169) ([leeavital](https://github.com/leeavital))

- MultipartParsing should only be called for POST requests that are multipart [\#168](https://github.com/twitter/finatra/pull/168) ([manjuraj](https://github.com/manjuraj))

- fixed resource resolution not loading from dependencies, and consistent ... [\#167](https://github.com/twitter/finatra/pull/167) ([tptodorov](https://github.com/tptodorov))

- Fix type error in sample code [\#165](https://github.com/twitter/finatra/pull/165) ([leeavital](https://github.com/leeavital))

- added builder from ChannelBuffer  [\#164](https://github.com/twitter/finatra/pull/164) ([tptodorov](https://github.com/tptodorov))

- Do not log errors in the ErrorHandler [\#163](https://github.com/twitter/finatra/pull/163) ([eponvert](https://github.com/eponvert))

- Adding missing copyright headers to source files [\#162](https://github.com/twitter/finatra/pull/162) ([bdimmick](https://github.com/bdimmick))

- support use of templates from dependencies in development mode, by loadi... [\#160](https://github.com/twitter/finatra/pull/160) ([tptodorov](https://github.com/tptodorov))

- Update readme.md to reflect issues on installation [\#152](https://github.com/twitter/finatra/pull/152) ([comamitc](https://github.com/comamitc))

- Add code coverage support with coveralls [\#151](https://github.com/twitter/finatra/pull/151) ([caniszczyk](https://github.com/caniszczyk))

- Use HttpServerDispatcher to fix remoteAddress property of Request. [\#142](https://github.com/twitter/finatra/pull/142) ([pixell](https://github.com/pixell))

- Don't add .mustache extension to template file name if it already has an extension [\#138](https://github.com/twitter/finatra/pull/138) ([jliszka](https://github.com/jliszka))

- Pass the filename of the template to the factory [\#136](https://github.com/twitter/finatra/pull/136) ([jliszka](https://github.com/jliszka))

- path definitions on routes [\#131](https://github.com/twitter/finatra/pull/131) ([grandbora](https://github.com/grandbora))

- ObjectMapper reuse & config [\#126](https://github.com/twitter/finatra/pull/126) ([Xorlev](https://github.com/Xorlev))

## [1.5.4](https://github.com/twitter/finatra/tree/1.5.4) (2014-07-07)

[Full Changelog](https://github.com/twitter/finatra/compare/1.5.3...1.5.4)

**Closed issues:**

- Could add support for Windows? [\#145](https://github.com/twitter/finatra/issues/145)

- Sessions example [\#134](https://github.com/twitter/finatra/issues/134)

- No main class detected. [\#133](https://github.com/twitter/finatra/issues/133)

- Unresolved dependencies [\#132](https://github.com/twitter/finatra/issues/132)

**Merged pull requests:**

- Bumped twitter-server to 1.6.1 [\#150](https://github.com/twitter/finatra/pull/150) ([pcalcado](https://github.com/pcalcado))

- modify FileService handle conditional GETs for static assets [\#144](https://github.com/twitter/finatra/pull/144) ([tomcz](https://github.com/tomcz))

- remove duplicated `organization` config [\#140](https://github.com/twitter/finatra/pull/140) ([jalkoby](https://github.com/jalkoby))

- More render shortcuts [\#139](https://github.com/twitter/finatra/pull/139) ([grandbora](https://github.com/grandbora))

- mixing Router with Twitter App creates exitTimer thread per request [\#135](https://github.com/twitter/finatra/pull/135) ([manjuraj](https://github.com/manjuraj))

## [1.5.3](https://github.com/twitter/finatra/tree/1.5.3) (2014-04-16)

[Full Changelog](https://github.com/twitter/finatra/compare/1.5.2...1.5.3)

**Closed issues:**

- Response body truncated [\#120](https://github.com/twitter/finatra/issues/120)

- Add 2 methods in FinatraServer.scala for custom start\(\) stop\(\) Code [\#107](https://github.com/twitter/finatra/issues/107)

**Merged pull requests:**

- Adding shortcut methods to common http statuses [\#128](https://github.com/twitter/finatra/pull/128) ([grandbora](https://github.com/grandbora))

- maxRequestSize flag has no effect [\#127](https://github.com/twitter/finatra/pull/127) ([manjuraj](https://github.com/manjuraj))

- Add content-length: 0 for no content responses [\#124](https://github.com/twitter/finatra/pull/124) ([grandbora](https://github.com/grandbora))

- Updated SpecHelper to support a body for POST, PUT and OPTIONS methods [\#123](https://github.com/twitter/finatra/pull/123) ([mattweyant](https://github.com/mattweyant))

- Use bytes length for content-length instead of string length [\#117](https://github.com/twitter/finatra/pull/117) ([beenokle](https://github.com/beenokle))

- Add helper for setting contentType [\#115](https://github.com/twitter/finatra/pull/115) ([murz](https://github.com/murz))

## [1.5.2](https://github.com/twitter/finatra/tree/1.5.2) (2014-02-03)

[Full Changelog](https://github.com/twitter/finatra/compare/1.5.1...1.5.2)

**Closed issues:**

- multipart/form-data regression [\#101](https://github.com/twitter/finatra/issues/101)

- flight/bower and bootstrap built in [\#63](https://github.com/twitter/finatra/issues/63)

**Merged pull requests:**

- upgrade mustache to 0.8.14 [\#106](https://github.com/twitter/finatra/pull/106) ([murz](https://github.com/murz))

- set Content-Length on static file responses [\#102](https://github.com/twitter/finatra/pull/102) ([zuercher](https://github.com/zuercher))

- Add support for Bower and use default bootstrap.css in new projects [\#99](https://github.com/twitter/finatra/pull/99) ([armandocanals](https://github.com/armandocanals))

## [1.5.1](https://github.com/twitter/finatra/tree/1.5.1) (2014-01-13)

[Full Changelog](https://github.com/twitter/finatra/compare/1.5.0a...1.5.1)

**Closed issues:**

- 1.7.x [\#96](https://github.com/twitter/finatra/issues/96)

- Investigate automatic html escaping in mustache templating [\#91](https://github.com/twitter/finatra/issues/91)

- Missing share files? [\#90](https://github.com/twitter/finatra/issues/90)

- Stats broken after twitter-server upgrade [\#95](https://github.com/twitter/finatra/issues/95)

- Response tied to originating request [\#86](https://github.com/twitter/finatra/issues/86)

- Test/Harden logging [\#84](https://github.com/twitter/finatra/issues/84)

- LogLevel doesn't seem to work [\#83](https://github.com/twitter/finatra/issues/83)

- enable full admin endpoints besides metrics.json [\#74](https://github.com/twitter/finatra/issues/74)

- request.routeParams should be decoded [\#68](https://github.com/twitter/finatra/issues/68)

**Merged pull requests:**

- Fix unicode rendering in json. Correct size of response is now set [\#97](https://github.com/twitter/finatra/pull/97) ([yuzeh](https://github.com/yuzeh))

- enable HTML escaping in mustache templates [\#92](https://github.com/twitter/finatra/pull/92) ([zuercher](https://github.com/zuercher))

## [1.5.0a](https://github.com/twitter/finatra/tree/1.5.0a) (2014-01-08)

[Full Changelog](https://github.com/twitter/finatra/compare/1.5.0...1.5.0a)

**Closed issues:**

- 0 deprecation/warnings [\#17](https://github.com/twitter/finatra/issues/17)

## [1.5.0](https://github.com/twitter/finatra/tree/1.5.0) (2014-01-07)

[Full Changelog](https://github.com/twitter/finatra/compare/finatra-1.4.1...1.5.0)

**Closed issues:**

- filters for select routes only [\#85](https://github.com/twitter/finatra/issues/85)

- using websockets [\#81](https://github.com/twitter/finatra/issues/81)

- maven =\> sbt [\#78](https://github.com/twitter/finatra/issues/78)

- support in release scripts for dual publishing scala 2.9 and 2.10 [\#75](https://github.com/twitter/finatra/issues/75)

- PUT and PATCH command param issue [\#71](https://github.com/twitter/finatra/issues/71)

**Merged pull requests:**

- Add Content-Length header as part of building the request. [\#82](https://github.com/twitter/finatra/pull/82) ([BenWhitehead](https://github.com/BenWhitehead))

- FinatraServer should take the generic Filters, not SimpleFilters [\#76](https://github.com/twitter/finatra/pull/76) ([pcalcado](https://github.com/pcalcado))

## [finatra-1.4.1](https://github.com/twitter/finatra/tree/finatra-1.4.1) (2013-11-13)

[Full Changelog](https://github.com/twitter/finatra/compare/1.4.0...finatra-1.4.1)

**Closed issues:**

- 1.4.1 [\#72](https://github.com/twitter/finatra/issues/72)

- Filter invoked 4 times per single request? [\#69](https://github.com/twitter/finatra/issues/69)

- Filters not working [\#66](https://github.com/twitter/finatra/issues/66)

- libthrift outdated [\#65](https://github.com/twitter/finatra/issues/65)

**Merged pull requests:**

- Adding lazy service [\#67](https://github.com/twitter/finatra/pull/67) ([grandbora](https://github.com/grandbora))

- Fixed a bug with Inheritance using Mustache [\#64](https://github.com/twitter/finatra/pull/64) ([pranjaltech](https://github.com/pranjaltech))

## [1.4.0](https://github.com/twitter/finatra/tree/1.4.0) (2013-10-14)

[Full Changelog](https://github.com/twitter/finatra/compare/finatra-1.4.0...1.4.0)

**Closed issues:**

- port back apache's multiupload handler [\#43](https://github.com/twitter/finatra/issues/43)

- move to com.twitter.common.metrics instead of ostrich.stats [\#42](https://github.com/twitter/finatra/issues/42)

- move to twitter-server once published [\#41](https://github.com/twitter/finatra/issues/41)

- Add public/ dir in src/main/resources as new docroot [\#39](https://github.com/twitter/finatra/issues/39)

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

**Closed issues:**

- Make mustache factory use baseTemplatePath local\_docroot and template\_path [\#56](https://github.com/twitter/finatra/issues/56)

**Merged pull requests:**

- Concatenate local\_docroot and template\_path when forming mustacheFactory [\#57](https://github.com/twitter/finatra/pull/57) ([yuzeh](https://github.com/yuzeh))

## [1.3.7](https://github.com/twitter/finatra/tree/1.3.7) (2013-07-20)

[Full Changelog](https://github.com/twitter/finatra/compare/finatra-1.3.7...1.3.7)

## [finatra-1.3.7](https://github.com/twitter/finatra/tree/finatra-1.3.7) (2013-07-20)

[Full Changelog](https://github.com/twitter/finatra/compare/finatra-1.3.4...finatra-1.3.7)

## [finatra-1.3.4](https://github.com/twitter/finatra/tree/finatra-1.3.4) (2013-07-20)

[Full Changelog](https://github.com/twitter/finatra/compare/finatra-1.3.3...finatra-1.3.4)

**Closed issues:**

- handle param routing for static file handling [\#55](https://github.com/twitter/finatra/issues/55)

- make redirects RFC compliant [\#49](https://github.com/twitter/finatra/issues/49)

- Sending redirect require a body [\#48](https://github.com/twitter/finatra/issues/48)

- support a "rails style" render.action to render arbitrary actions from any other action without a redirect [\#44](https://github.com/twitter/finatra/issues/44)

- Startup / Shutdown hooks [\#37](https://github.com/twitter/finatra/issues/37)

**Merged pull requests:**

- Support OPTIONS HTTP method [\#53](https://github.com/twitter/finatra/pull/53) ([theefer](https://github.com/theefer))

- Stying pass across the codebase. Fixing conventions. [\#51](https://github.com/twitter/finatra/pull/51) ([twoism](https://github.com/twoism))

- \[closes \#49\] make redirects match the RFC [\#50](https://github.com/twitter/finatra/pull/50) ([twoism](https://github.com/twoism))

## [finatra-1.3.3](https://github.com/twitter/finatra/tree/finatra-1.3.3) (2013-06-14)

[Full Changelog](https://github.com/twitter/finatra/compare/finatra-1.3.2...finatra-1.3.3)

**Merged pull requests:**

- fixed typing of jsonGenerator so it can be actually overridden [\#47](https://github.com/twitter/finatra/pull/47) ([bmdhacks](https://github.com/bmdhacks))

## [finatra-1.3.2](https://github.com/twitter/finatra/tree/finatra-1.3.2) (2013-06-13)

[Full Changelog](https://github.com/twitter/finatra/compare/finatra-1.3.1...finatra-1.3.2)

**Merged pull requests:**

- allow json encoder to be overwritten [\#46](https://github.com/twitter/finatra/pull/46) ([bmdhacks](https://github.com/bmdhacks))

- shutdown the built server on shutdown [\#40](https://github.com/twitter/finatra/pull/40) ([sprsquish](https://github.com/sprsquish))

## [finatra-1.3.1](https://github.com/twitter/finatra/tree/finatra-1.3.1) (2013-03-12)

[Full Changelog](https://github.com/twitter/finatra/compare/finatra-1.3.0...finatra-1.3.1)

**Closed issues:**

- ./finatra update-readme no longer works [\#34](https://github.com/twitter/finatra/issues/34)

## [finatra-1.3.0](https://github.com/twitter/finatra/tree/finatra-1.3.0) (2013-03-10)

[Full Changelog](https://github.com/twitter/finatra/compare/finatra-1.2.2...finatra-1.3.0)

## [finatra-1.2.2](https://github.com/twitter/finatra/tree/finatra-1.2.2) (2013-03-10)

[Full Changelog](https://github.com/twitter/finatra/compare/finatra-1.2.0...finatra-1.2.2)

**Closed issues:**

- ./finatra generator doesnt work on linux [\#24](https://github.com/twitter/finatra/issues/24)

**Merged pull requests:**

- Handle downstream exceptions and display the error handler. [\#38](https://github.com/twitter/finatra/pull/38) ([bmdhacks](https://github.com/bmdhacks))

- Force mustache partials to be uncached from the local filesystem in development mode. [\#36](https://github.com/twitter/finatra/pull/36) ([morria](https://github.com/morria))

- Fixing call to the request logger [\#35](https://github.com/twitter/finatra/pull/35) ([morria](https://github.com/morria))

## [finatra-1.2.0](https://github.com/twitter/finatra/tree/finatra-1.2.0) (2013-01-22)

[Full Changelog](https://github.com/twitter/finatra/compare/finatra-1.1.1...finatra-1.2.0)

## [finatra-1.1.1](https://github.com/twitter/finatra/tree/finatra-1.1.1) (2012-12-06)

[Full Changelog](https://github.com/twitter/finatra/compare/finatra-1.1.0...finatra-1.1.1)

**Closed issues:**

- Custom error handlers [\#29](https://github.com/twitter/finatra/issues/29)

**Merged pull requests:**

- Fix Set-Cookier header bug in response [\#31](https://github.com/twitter/finatra/pull/31) ([hontent](https://github.com/hontent))

## [finatra-1.1.0](https://github.com/twitter/finatra/tree/finatra-1.1.0) (2012-11-20)

[Full Changelog](https://github.com/twitter/finatra/compare/finatra-1.0.3...finatra-1.1.0)

**Closed issues:**

- Publish to Maven Central [\#23](https://github.com/twitter/finatra/issues/23)

## [finatra-1.0.3](https://github.com/twitter/finatra/tree/finatra-1.0.3) (2012-11-13)

[Full Changelog](https://github.com/twitter/finatra/compare/finatra-1.0.2...finatra-1.0.3)

## [finatra-1.0.2](https://github.com/twitter/finatra/tree/finatra-1.0.2) (2012-11-13)

[Full Changelog](https://github.com/twitter/finatra/compare/finatra-1.0.1...finatra-1.0.2)

**Closed issues:**

- Serve static files [\#28](https://github.com/twitter/finatra/issues/28)

## [finatra-1.0.1](https://github.com/twitter/finatra/tree/finatra-1.0.1) (2012-11-11)

[Full Changelog](https://github.com/twitter/finatra/compare/finatra-1.0.0...finatra-1.0.1)

**Closed issues:**

- Unable to retrieve post parameters [\#26](https://github.com/twitter/finatra/issues/26)

**Merged pull requests:**

- fix of post parameters [\#27](https://github.com/twitter/finatra/pull/27) ([mairbek](https://github.com/mairbek))

- Immutable instead of mutable map in tests [\#25](https://github.com/twitter/finatra/pull/25) ([mairbek](https://github.com/mairbek))

## [finatra-1.0.0](https://github.com/twitter/finatra/tree/finatra-1.0.0) (2012-11-08)

[Full Changelog](https://github.com/twitter/finatra/compare/finatra-0.3.4...finatra-1.0.0)

**Closed issues:**

- an config [\#12](https://github.com/twitter/finatra/issues/12)

## [finatra-0.3.4](https://github.com/twitter/finatra/tree/finatra-0.3.4) (2012-11-07)

[Full Changelog](https://github.com/twitter/finatra/compare/finatra-0.3.3...finatra-0.3.4)

**Closed issues:**

- do a perf review [\#13](https://github.com/twitter/finatra/issues/13)

- update docs [\#8](https://github.com/twitter/finatra/issues/8)

## [finatra-0.3.3](https://github.com/twitter/finatra/tree/finatra-0.3.3) (2012-11-05)

[Full Changelog](https://github.com/twitter/finatra/compare/finatra-0.3.2...finatra-0.3.3)

## [finatra-0.3.2](https://github.com/twitter/finatra/tree/finatra-0.3.2) (2012-11-04)

[Full Changelog](https://github.com/twitter/finatra/compare/finatra-0.2.4...finatra-0.3.2)

**Closed issues:**

- allow insertion of userland filters into the finagle stack [\#15](https://github.com/twitter/finatra/issues/15)

- bubble up view/mustache errors [\#14](https://github.com/twitter/finatra/issues/14)

## [finatra-0.2.4](https://github.com/twitter/finatra/tree/finatra-0.2.4) (2012-08-18)

[Full Changelog](https://github.com/twitter/finatra/compare/finatra-0.2.3...finatra-0.2.4)

**Merged pull requests:**

- Add Controller method callback timing [\#21](https://github.com/twitter/finatra/pull/21) ([franklinhu](https://github.com/franklinhu))

## [finatra-0.2.3](https://github.com/twitter/finatra/tree/finatra-0.2.3) (2012-08-08)

[Full Changelog](https://github.com/twitter/finatra/compare/finatra-0.2.1...finatra-0.2.3)

**Merged pull requests:**

- Pass controllers into AppService [\#20](https://github.com/twitter/finatra/pull/20) ([franklinhu](https://github.com/franklinhu))

## [finatra-0.2.1](https://github.com/twitter/finatra/tree/finatra-0.2.1) (2012-07-20)

[Full Changelog](https://github.com/twitter/finatra/compare/finatra-0.2.0...finatra-0.2.1)

**Merged pull requests:**

- Fix FinatraServer register for AbstractFinatraController type change [\#19](https://github.com/twitter/finatra/pull/19) ([franklinhu](https://github.com/franklinhu))

## [finatra-0.2.0](https://github.com/twitter/finatra/tree/finatra-0.2.0) (2012-07-20)

[Full Changelog](https://github.com/twitter/finatra/compare/finatra-0.1.10...finatra-0.2.0)

**Closed issues:**

- regexed routes [\#11](https://github.com/twitter/finatra/issues/11)

- PID management [\#5](https://github.com/twitter/finatra/issues/5)

**Merged pull requests:**

- Add Travis CI status to README [\#18](https://github.com/twitter/finatra/pull/18) ([caniszczyk](https://github.com/caniszczyk))

## [finatra-0.1.10](https://github.com/twitter/finatra/tree/finatra-0.1.10) (2012-07-14)

[Full Changelog](https://github.com/twitter/finatra/compare/finatra-0.1.9...finatra-0.1.10)

## [finatra-0.1.9](https://github.com/twitter/finatra/tree/finatra-0.1.9) (2012-07-14)

[Full Changelog](https://github.com/twitter/finatra/compare/finatra-0.1.8...finatra-0.1.9)

## [finatra-0.1.8](https://github.com/twitter/finatra/tree/finatra-0.1.8) (2012-07-14)

[Full Changelog](https://github.com/twitter/finatra/compare/finatra-0.1.7...finatra-0.1.8)

**Closed issues:**

- mvn package doesnt fully package [\#16](https://github.com/twitter/finatra/issues/16)

- update gem [\#7](https://github.com/twitter/finatra/issues/7)

- verify heroku uploads works [\#6](https://github.com/twitter/finatra/issues/6)

## [finatra-0.1.7](https://github.com/twitter/finatra/tree/finatra-0.1.7) (2012-07-13)

[Full Changelog](https://github.com/twitter/finatra/compare/finatra-0.1.6...finatra-0.1.7)

## [finatra-0.1.6](https://github.com/twitter/finatra/tree/finatra-0.1.6) (2012-07-13)

[Full Changelog](https://github.com/twitter/finatra/compare/finatra-0.1.5...finatra-0.1.6)

**Closed issues:**

- unbreak file upload/form support [\#10](https://github.com/twitter/finatra/issues/10)

## [finatra-0.1.5](https://github.com/twitter/finatra/tree/finatra-0.1.5) (2012-07-13)

[Full Changelog](https://github.com/twitter/finatra/compare/finatra-0.1.3...finatra-0.1.5)

**Closed issues:**

- add logging [\#4](https://github.com/twitter/finatra/issues/4)

## [finatra-0.1.3](https://github.com/twitter/finatra/tree/finatra-0.1.3) (2012-07-13)

[Full Changelog](https://github.com/twitter/finatra/compare/finatra-0.1.2...finatra-0.1.3)

## [finatra-0.1.2](https://github.com/twitter/finatra/tree/finatra-0.1.2) (2012-07-13)

[Full Changelog](https://github.com/twitter/finatra/compare/finatra-0.1.1...finatra-0.1.2)

**Closed issues:**

- unbreak cookie support [\#9](https://github.com/twitter/finatra/issues/9)

## [finatra-0.1.1](https://github.com/twitter/finatra/tree/finatra-0.1.1) (2012-07-13)

[Full Changelog](https://github.com/twitter/finatra/compare/finatra-0.1.0...finatra-0.1.1)

## [finatra-0.1.0](https://github.com/twitter/finatra/tree/finatra-0.1.0) (2012-07-12)

[Full Changelog](https://github.com/twitter/finatra/compare/finatra-0.0.1...finatra-0.1.0)

## [finatra-0.0.1](https://github.com/twitter/finatra/tree/finatra-0.0.1) (2012-07-12)

**Merged pull requests:**

- Fix synchronization/correctness issues [\#3](https://github.com/twitter/finatra/pull/3) ([franklinhu](https://github.com/franklinhu))

- Fix HTTP response code for routes not found [\#2](https://github.com/twitter/finatra/pull/2) ([franklinhu](https://github.com/franklinhu))

- Fix template file resolving for packaged jarfiles [\#1](https://github.com/twitter/finatra/pull/1) ([franklinhu](https://github.com/franklinhu))



\* *This Change Log was automatically generated by [github_changelog_generator](https://github.com/skywinder/Github-Changelog-Generator)*