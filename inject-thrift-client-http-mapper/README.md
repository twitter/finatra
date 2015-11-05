Inject Thrift Client Http Mapper
==========================================================
inject-thrift-client-http-mapper is a library for generating fine-grain stats of which thrift client errors resulted in a HTTP failed response. This allows services to do better alerting as to which internal error or dependent system error is causing a drop in a finatra-http server's SLA. The following is an example of the per-route stats that are generated:
```
route/add1String/GET/status/503/handled/ThriftClientException/Adder/add1String/com.twitter.finatra.thrift.thriftscala.ServerError
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