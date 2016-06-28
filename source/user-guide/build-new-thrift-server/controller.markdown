---
layout: user_guide
title: "Add a Thrift Controller"
comments: false
sharing: false
footer: true
---

<ol class="breadcrumb">
  <li><a href="/finatra/user-guide">User Guide</a></li>
  <li><a href="/finatra/user-guide/build-new-thrift-server">Building a New Thrift Server</a></li>
  <li class="active">Add a Controller</li>
</ol>

## Thrift Controller Basics
===============================

A *Thrift Controller* is an implementation of your thrift service. To create the controller, extend the `com.twitter.finatra.thrift.Controller` trait and mix-in the [Scrooge](http://twitter.github.io/scrooge/)-generated `BaseServiceIface` trait for your service. Scrooge generates a `ServiceIface` which is a case class containing a `Service` for each thrift method over the corresponding `Args` and `Result` structures for the method that extends from the `BaseServiceIface` trait. E.g,

```scala
case class ServiceIface(
  fetchBlob: Service[FetchBlob.Args, FetchBlob.Result]
) extends BaseServiceIface
```
<div></div>

For Thrift Controllers we use the `BaseServiceIface` trait since we are not able to extend the `ServiceIface` case class.

The Finatra `com.twitter.finatra.thrift.Controller` provides a DSL with which you can easily implement your thrift service methods via a `handle(ThriftMethod)` function that takes a callback from `ThriftMethod.Args => Future[ThriftMethod.Result]`.

For example, given the following thrift IDL: `example_service.thrift`

```
namespace java com.twitter.example.thriftjava
#@namespace scala com.twitter.example.thriftscala
namespace rb ExampleService

include "finatra-thrift/finatra_thrift_exceptions.thrift"

service ExampleService {
  i32 add1(
    1: i32 num
  ) throws (
    1: finatra_thrift_exceptions.ServerError serverError,
    2: finatra_thrift_exceptions.UnknownClientIdError unknownClientIdError
    3: finatra_thrift_exceptions.NoClientIdError noClientIdError
  )
}
```
<div></div>


We can implement the following Thrift Controller:

```scala
import com.twitter.example.thriftscala.ExampleService
import com.twitter.finatra.thrift.Controller
import com.twitter.util.Future

class ExampleThriftController
  extends Controller
  with ExampleService.BaseServiceIface {

  override val add1 = handle(Add1) { args: Add1.Args =>
    Future(args.num + 1)
  }
}
```
<div></div>

The `handle(ThriftMethod)` function may seem magical but it serves an important purpose. By implementing your service method via this function, it allows the framework to  apply the configured filter chain defined in your [server definition](/finatra/user-guide/build-new-thrift-server#server-definition) to your method implementation (passed as the callback to `handle(ThriftMethod)`).

That is to say, the `handle(ThriftMethod)` function captures your method implementation then exposes it for the [`ThriftRouter`](https://github.com/twitter/finatra/blob/develop/thrift/src/main/scala/com/twitter/finatra/thrift/routing/ThriftRouter.scala) to combine with the configured filter chain to build the [Finagle Service](http://twitter.github.io/finagle/guide/ServicesAndFilters.html) that represents your server. See the next [section](/finatra/user-guide/build-new-thrift-server/filter.html) for more information on adding filters to your server definition.

Note, in the example above we implement the `ExampleService.BaseServiceIface#add1` method to satisfy the `ExampleService.BaseServiceIface` interface -- however, the framework will not call the `add1` method in this way as it uses the implementation of the thrift method captured by the `handle(ThriftMethod)` function (as mentioned above this in order to apply the configured filter chain to requests). Thus if you were to directly call `ExampleThriftController.add1(request)` this would by-pass any configured [filters](/finatra/user-guide/build-new-thrift-server/filter.html) from the server definition.

We use `override val` since the computed [`ThriftMethodService`](https://github.com/twitter/finatra/blob/develop/thrift/src/main/scala/com/twitter/finatra/thrift/internal/ThriftMethodService.scala) instance returned [is effectively constant](https://github.com/twitter/finatra/blob/develop/thrift/src/main/scala/com/twitter/finatra/thrift/Controller.scala#L19).

As previously shown, the server can then be defined with this Thrift Controller:

```scala
class ExampleServer extends ThriftServer {
  ...
  override def configureThrift(router: ThriftRouter): Unit = {
    router
      .add[ExampleThriftController]
  }
}
```
<div></div>

## <a class="anchor" name="more-information" href="#more-information">More information</a>
===============================

For more information, see the [Finagle Integration](http://twitter.github.io/scrooge/Finagle.html) section of the [Scrooge](http://twitter.github.io/scrooge/index.html) documentation.

Next section: [Add Filters](/finatra/user-guide/build-new-thrift-server/filter.html).

<nav>
  <ul class="pager">
    <li class="previous"><a href="/finatra/user-guide/build-new-thrift-server"><span aria-hidden="true">&larr;</span>&nbsp;Building&nbsp;a&nbsp;New&nbsp;Thrift&nbsp;Server</a></li>
    <li class="next"><a href="/finatra/user-guide/build-new-thrift-server/filter.html">Add&nbsp;Filters&nbsp;<span aria-hidden="true">&rarr;</span></a></li>
  </ul>
</nav>
