Finatra
==========================================================
Fast, testable Scala services inspired by [Sinatra](http://www.sinatrarb.com/) and powered by [`twitter-server`][twitter-server].

Current version: `2.0.0.M1`

[![Gitter](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/twitter/finatra)

Announcing the first milestone release of Finatra version 2!
-----------------------------------------------------------

Documentation for prior versions can be found [here](https://github.com/twitter/finatra/tree/5d1d1cbb7640d8c4b1d11a85b53570d11a323e55).

Features
-----------------------------------------------------------
* Production use as Twitter’s HTTP framework
* ~50 times faster than v1.6 in several benchmarks
* Powerful feature and integration test support
* JSR-330 Dependency Injection using [Google Guice][guice]
* [Jackson][jackson] based JSON parsing supporting required fields, default values, and custom validations
* [Logback][logback] [MDC][mdc] integration with [com.twitter.util.Local][local] for contextual logging across futures
• Guice request scope integration with Futures

<a name="quick-start">Quick Start</a>
-----------------------------------------------------------
To get started we'll focus on building an HTTP API for posting and getting tweets:

### Domain

```scala
case class PostedTweet(
  @Size(min = 1, max = 140) message: String,
  location: Option[Location],
  sensitive: Boolean = false) {
  
case class GetTweet(
  @RouteParam id: StatusId)
```

Then, let's create a [`Controller`][Controller]:

### Controller

```scala
@Singleton
class TweetsController @Inject()(
  tweetsService: TweetsService)
  extends Controller {

  post("/tweet") { postedTweet: PostedTweet =>
    tweetsService.save(postedTweet) map { savedTweet =>
      response
        .created(savedTweet)
        .location(savedTweet.id)
    }
  }

  get("/tweet/:id") { request: GetTweet =>
    tweetsService.get(request.id)
  }
}
```

Next, let's create a server:

### Server

```scala
class TwitterCloneServer extends HttpServer {
  
  override val modules = Seq(FirebaseHttpClientModule)

  override def configureHttp(router: HttpRouter): Unit = {
    router
      .register[StatusMessageBodyWriter]
      .filter[CommonFilters]
      .add[TweetsController]
  }
}
```

And finally, we can write a Feature Test:

### Feature Test

```scala
class TwitterCloneFeatureTest extends FeatureTest with Mockito {

  override val server = new EmbeddedHttpServer(
    twitterServer = new TwitterCloneServer {
      override val overrideModules = Seq(integrationTestModule)
    })

  @Bind val firebaseClient = smartMock[FirebaseClient]

  @Bind val idService = smartMock[IdService]

  "tweet creation" in {
    //Setup mocks
    idService.getId returns Future(StatusId("123"))

    val mockStatus = Status(
      id = StatusId("123"),
      text = "Hello #SFScala",
      lat = Some(37.7821120598956),
      long = Some(-122.400612831116),
      sensitive = false)

    firebaseClient.put("/statuses/123.json", mockStatus) returns Future.Unit
    firebaseClient.get("/statuses/123.json")(manifest[Status]) returns Future(Option(mockStatus))

    //Assert tweet post
    val result = server.httpPost(
      path = "/tweet",
      postBody = """
        {
          "message": "Hello #SFScala",
          "location": {
            "lat": "37.7821120598956",
            "long": "-122.400612831116"
          },
          "sensitive": false
        }""",
      andExpect = Created,
      withJsonBody = """
        {
          "id": "123",
          "message": "Hello #SFScala",
          "location": {
            "lat": "37.7821120598956",
            "long": "-122.400612831116"
          },
          "sensitive": false
        }""")

    //Assert tweet get
    server.httpGet(
      path = result.location.get,
      andExpect = Ok,
      withJsonBody = result.contentString)
  }

  "Post bad tweet" in {
    server.httpPost(
      path = "/tweet",
      postBody = """
        {
          "message": "",
          "location": {
            "lat": "9999"
          },
          "sensitive": "abc"
        }""",
      andExpect = BadRequest,
      withJsonBody = """
        {
          "errors" : [
            "message size [0] is not between 1 and 140",
            "location.lat [9999.0] is not between -85 and 85",
            "location.long is a required field",
            "sensitive's value 'abc' is not a valid boolean"
          ]
        }
        """)
  }
}
```

Libraries
-----------------------------------------------------------

We are publishing Scala 2.10 and 2.11 compatible libraries to [Maven central][maven-central].
The Finatra project is currently split up into multiple components: (Twitter Inject and Finatra libraries).

### Twitter Inject (`com.twitter.inject`)
Inject provides libraries for integrating [`twitter-server`][twitter-server] and [`util-app`][util-app] with [Google Guice][guice].

[Detailed documentation](inject/README.md)

### Finatra (`com.twitter.finatra`)  
Finatra is a framework for easily building API services on top of Twitter’s Scala stack (twitter-server, finagle, twitter-util)

[Detailed documentation](http/README.md)

Authors
-----------------------------------------------------------
* Steve Cosenza <https://github.com/scosenza>
* Christopher Coco <https://github.com/cacoco>
* Jason Carey <https://github.com/jcarey03>
* Eugene Ma <https://github.com/edma2>

A full list of [contributors](https://github.com/twitter/finatra/graphs/contributors?type=a) can be found on GitHub.

Follow [@finatra](http://twitter.com/finatra) on Twitter for updates.


License
-----------------------------------------------------------
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
[Controller]: http/src/main/scala/com/twitter/finatra/http/Controller.scala
[HttpServer]: http/src/main/scala/com/twitter/finatra/http/HttpServer.scala
[twitter-clone-example]: examples/finatra-twitter-clone/
[maven-central]: http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.twitter.finatra%22
