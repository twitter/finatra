# Summary
inject-thrift-client is a library for configuring injectable thrift clients. The examples below demonstrate creating a thrift-client for the following thrift defined service:
```thrift
exception InvalidOperation {
  1: i32 what,
  2: string why
}

exception ByeOperation {
  1: i32 code
}

struct ByeResponse {
  1: double code;
  2: string msg;
}

service Greeter {

  /**
   * Say hi
   */
  string hi(
    1: string name
  ) throws (1:InvalidOperation invalidOperation)

  /**
   * Say bye
   */
  ByeResponse bye(
    1: string name
    2: i32 age
  ) throws (1:ByeOperation byeOperation)
}
```

## FilteredThriftClientModule Usage
FilteredThriftClientModule integrates with Scrooge 4 ["services-per-endpoint"](https://finagle.github.io/blog/2015/09/10/services-per-endpoint-in-scrooge/), supporting per-method filter chains which can retry on requests, successful responses, failed futures, and thrift-idl defined exceptions.

```scala
object GreeterThriftClientModule
  extends FilteredThriftClientModule[Greeter[Future], Greeter.ServiceIface] {

  override val label = "greeter-thrift-client"
  override val dest = "flag!greeter-thrift-service"
  override val sessionAcquisitionTimeout = 1.minute.toDuration

  override def filterServiceIface(
    serviceIface: Greeter.ServiceIface,
    filter: ThriftClientFilterBuilder) = {

    serviceIface.copy(
      hi = filter.method(Hi)
        .timeout(2.minutes)
        .constantRetry(
          requestTimeout = 1.minute,
          shouldRetryResponse = {
            case Return(Hi.Result(_, Some(e: InvalidOperation))) => true
            case Return(Hi.Result(Some(success), _)) => success == "ERROR"
            case Throw(NonFatal(_)) => true
          },
          start = 50.millis,
          retries = 3)
        .filter[HiLoggingThriftClientFilter]
        .andThen(serviceIface.hi),
      bye = filter.method(Bye)
        .exponentialRetry(
          shouldRetryResponse = PossiblyRetryableExceptions,
          requestTimeout = 1.minute,
          start = 50.millis,
          multiplier = 2,
          retries = 3)
        .andThen(serviceIface.bye))
  }
```

Then add GreeterThriftClientModule to your list of server modules, and inject the "Greeter" thrift-client as such:
```scala
class MyClass @Inject()(
  greeter: Greeter[Future])
```

## ThriftClientModule Usage (deprecated)
If you don't need client retries or other client filters, ThriftClientModule can be used to simply create an injectable thrift client as such:
```scala
object GreeterThriftClientModule extends ThriftClientModule[Greeter[Future]] {
  override val label = "greeter-thrift-client"
  override val dest = "flag!greeter-thrift-service"
}
```
