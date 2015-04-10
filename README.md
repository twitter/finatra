Finatra
==========================================================

The scala service framework inspired by [Sinatra](http://www.sinatrarb.com/) powered by [`twitter-server`][twitter-server]

Current version: `2.0.0.M1` 

[![Build Status](https://secure.travis-ci.org/twitter/finatra.png?branch=master)](http://travis-ci.org/twitter/finatra?branch=master) [![Coverage Status](https://coveralls.io/repos/twitter/finatra/badge.png?branch=master)](https://coveralls.io/r/twitter/finatra?branch=master)

Features
-----------------------------------------------------------
* Significant performance improvements over v1.6.0
* Powerful feature and integration test support
* JSR-330 Dependency Injection using [Google Guice][guice]
* [Jackson][jackson] based JSON parsing with additional support for required fields, default values, and custom validations
* [Logback][logback] [MDC][mdc] integration with [com.twitter.util.Local][local] for contextual logging across futures

## Libraries

* Scala 2.10 and 2.11 compatible libraries published to [Maven central][maven-central]
 
### [Inject](https://github.com/twitter/finatrav2/blob/master/inject/README.md) `com.twitter.inject`
Inject provides libraries for integration [`twitter-server`][twitter-server] and [`util-app`][util-app] with [Google Guice][guice] 

[Detailed documentation](https://github.com/twitter/finatrav2/blob/master/inject/README.md)

* inject-core_2.11
* inject-app_2.11
* inject-server_2.11
* inject-modules_2.11
* inject-thrift-client_2.11
* inject-request-scope_2.11

### [Finatra](https://github.com/twitter/finatrav2/blob/master/finatra/README.md) `com.twitter.finatra` 

[Detailed documentation](https://github.com/twitter/finatrav2/blob/master/finatra/README.md)

* finatra-http_2.11
* finatra-jackson_2.11
* finatra-logback_2.11
* finatra-httpclient_2.11
* finatra-utils_2.11

Quick Start
-----------------------------------------------------------
To get started we'll focus on building an HTTP API for a simple "Todo" list application which will support adding tasks to a todo list.
The full example can be found [here][todo-example].


First, we define our `TaskRequest` domain object:

## Domain

```scala
import com.twitter.finatra.validation.{NotEmpty, Size}

case class TaskRequest(
  @NotEmpty name: String,
  @Size(min = 10, max = 140) description: String)
```

Then, assuming we already have a `TaskRepository` (configured with a `TaskRepositoryModule`) to store tasks, let's create a [`Controller`][Controller]:

### Controller

```scala
import com.twitter.finagle.http.Request
import com.twitter.finatra.Controller
import com.twitter.todo.domain.{Task, GetTaskRequest, PostTaskRequest, TaskRepository}
import javax.inject.{Inject, Singleton}

@Singleton
class TasksController @Inject()(
  repository: TaskRepository) 
  extends Controller {
  
  post("/todo/tasks") { request: TaskRequest =>
    val task = repository.add(request.name, request.description)
    response
      .created(task)
      .location(task.id)
  }
}
```

Next, let's create a [HttpServer][HttpServer]:

### Server

```scala
import com.twitter.finatra.HttpServer
import com.twitter.finatra.filters.CommonFilters
import com.twitter.finatra.routing.HttpRouter
import com.twitter.todo.modules.TaskRepositoryModule

class TodoServer extends HttpServer {
  override val modules = Seq(
    TaskRepositoryModule)

  override def configureHttp(router: HttpRouter) {
    router
      .filter[CommonFilters]
      .add[TasksController]
  }
}
```

And finally, we can write a Feature Test:

### Feature Test

```scala
import com.twitter.finagle.http.Status.{Created, BadRequest}
import com.twitter.finatra.test.EmbeddedHttpServer
import com.twitter.inject.server.FeatureTest
import com.twitter.todo.domain.Task

class TodoFeatureTest extends FeatureTest {
  
  override val server = new EmbeddedHttpServer(new TodoServer)

  "post good task" in {
    server.httpPost(
      "/todo/tasks",
      """
      {
        "name": "my-task",
        "description": "pick up milk"
      }
      """,
      andExpect = Created)
  }
      
  "post bad task" in {
    server.httpPost(
      "/todo/tasks",
      """
      {
        "name": "",
        "description": "short"
      }
      """,
      andExpect = BadRequest,
      withJsonBody = 
      """
      {
        "errors": [
          "name cannot be empty",
          "description size [5] is not between 10 and 140"
        ]
      }
      """)
  }
}
```

## Authors:
* Steve Cosenza <https://github.com/scosenza>
* Christopher Coco <https://github.com/cacoco>
* Jason Carey <https://github.com/jcarey03>
* Eugene Ma <https://github.com/edma2>

A full list of [contributors](https://github.com/twitter/finatra/graphs/contributors?type=a) can be found on GitHub.

Follow [@finatra](http://twitter.com/finatra) on Twitter for updates.


## License
Copyright 2015 Twitter, Inc.

Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0

[twitter-server]: https://github.com/twitter/twitter-server
[finagle]: https://github.com/twitter/finagle
[util-app]: https://github.com/twitter/util/tree/master/util-app
[util-core]: https://github.com/twitter/util/blob/master/util-core/src/main/scala/com/twitter/util/Local.scala#L90
[guice]: https://github.com/google/guice
[jackson]: https://github.com/FasterXML/jackson
[logback]: http://logback.qos.ch/
[slf4j]: http://www.slf4j.org/manual.html
[grizzled-slf4j]: http://software.clapper.org/grizzled-slf4j/
[local]: https://github.com/twitter/util/blob/master/util-core/src/main/scala/com/twitter/util/Local.scala
[mdc]: http://logback.qos.ch/manual/mdc.html
[Controller]: https://github.com/twitter/finatra/blob/master/finatra/finatra-http/src/main/scala/com/twitter/finatra/Controller.scala
[HttpServer]: https://github.com/twitter/finatra/blob/master/finatra/finatra-http/src/main/scala/com/twitter/finatra/HttpServer.scala
[todo-example]: https://github.com/twitter/finatra/blob/master/finatra/finatra-examples/finatra-todo/
[maven-central]: http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.twitter.finatra%22
