Inject Thrift Client Http Mapper
==========================================================
inject-thrift-client-http-mapper is a library for generating fine-grain stats of which thrift client errors resulted in a HTTP failed response. This allows services to do better alerting as to which internal error or dependent system error is causing a drop in a finatra-http server's SLA. The following is an example of the per-route stats that are generated:

```
route/add1String/GET/failure/adder-thrift/Adder/add1String/com.twitter.finatra.thrift.thriftscala.ServerError
```

Quick Start
-----------------------------------------------------------
* Depend on the `com.twitter.finatra:inject-thrift-client-http-mapper` library.
* Add the ThriftClientExceptionMapper to your server like so:

```scala
override def configureHttp(router: HttpRouter) {
  router
    .exceptionMapper[ThriftClientExceptionMapper]
    ...
}
```

* Use a FilteredThriftClientModule to configure your thrift clients. See [Adder1ThriftClientModule](./src/test/com/twitter/finatra/multiserver/Add1HttpServer/Adder1ThriftClientModule.scala) for an example.

Note:
-----------------------------------------------------------
Classes/objects in internal packages, e.g. `com.twitter.finatra.thrift.internal.*` are Finatra framework internal implementation details.
These are meant to be private to the framework and not intended as publicly accessible as they are details specific to the framework and
are thus more subject to breaking changes. You should not depend on their implementations remaining constant since they are not intended
for use outside of the framework itself.