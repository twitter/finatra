# Finatra

[![Build Status](https://secure.travis-ci.org/twitter/finatra.png?branch=master)](http://travis-ci.org/twitter/finatra?branch=master)
[![Test Coverage](http://codecov.io/github/twitter/finatra/coverage.svg?branch=master)](http://codecov.io/github/twitter/finatra?branch=master)
[![Project status](https://img.shields.io/badge/status-active-brightgreen.svg)](#status)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.twitter.finatra/finatra-http_2.11/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.twitter.finatra/finatra-http_2.11)
[![Gitter](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/twitter/finatra)

## Status

This project is used in production at Twitter (and many other organizations),
and is being actively developed and maintained.

![finatra logo](finatra_logo.png)

#### Fast, testable, Scala services built on Twitter-Server and Finagle.

## Getting involved

* Website: https://twitter.github.io/finatra/
* Source: https://github.com/twitter/finatra/
* Mailing List: [finatra@googlegroups.com](https://groups.google.com/forum/#!forum/finatra)


## Features

* Production use as Twitter’s HTTP framework
* ~50 times faster than v1.6 in several benchmarks
* Powerful feature and integration test support
* Optional JSR-330 Dependency Injection using [Google Guice][guice]
* [Jackson][jackson] based JSON parsing supporting required fields, default values, and custom validations
* [Logback][logback] [MDC][mdc] integration with [com.twitter.util.Local][local] for contextual logging across futures

## Presentations

Check out our list of recent presentations: [Finatra Presentations](http://twitter.github.io/finatra/presentations/)

## News

* Finatra is now built against the latest Finagle v6.33.0 and Twitter Server v1.18.0 releases.
* Please take a look at our new [User Guide][user-guide]!
* Keep up with the latest news [here](http://twitter.github.io/finatra/blog/archives/) on our blog.

## <a name="quick-start">Quick Start</a>

To get started we'll focus on building an HTTP API for posting and getting tweets:

### Domain

```Scala
case class TweetPostRequest(
  @Size(min = 1, max = 140) message: String,
  location: Option[TweetLocation],
  nsfw: Boolean = false)

case class TweetGetRequest(
  @RouteParam id: TweetId)
```

Then, let's create a [`Controller`][Controller]:

### Controller

```Scala
@Singleton
class TweetsController @Inject()(
  tweetsService: TweetsService)
  extends Controller {

  post("/tweet") { requestTweet: TweetPostRequest =>
    for {
      savedTweet <- tweetsService.save(requestTweet)
      responseTweet = TweetResponse.fromDomain(savedTweet)
    } yield {
      response
        .created(responseTweet)
        .location(responseTweet.id)
    }
  }

  get("/tweet/:id") { request: TweetGetRequest =>
    tweetsService.getResponseTweet(request.id)
  }
}
```

Next, let's create a server:

### Server

```Scala
class TwitterCloneServer extends HttpServer {
  override val modules = Seq(FirebaseHttpClientModule)

  override def configureHttp(router: HttpRouter): Unit = {
    router
      .filter[CommonFilters]
      .add[TweetsController]
  }
}
```

And finally, we can write a Feature Test:

### Feature Test

```Scala
class TwitterCloneFeatureTest extends FeatureTest with Mockito {

  override val server = new EmbeddedHttpServer(new TwitterCloneServer)

  @Bind val firebaseClient = smartMock[FirebaseClient]

  @Bind val idService = smartMock[IdService]

  "tweet creation" in {
    //Setup mocks
    idService.getId returns Future(StatusId("123"))

    val tweetResponse = TweetResponse(...)
    firebaseClient.put("/tweets/123.json", tweetResponse) returns Future.Unit
    firebaseClient.get("/tweets/123.json")(manifest[TweetResponse]) returns Future(Option(tweetResponse))

    //Assert tweet post
    val result = server.httpPost(
      path = "/tweet",
      postBody = """
        {
          "message": "Hello #FinagleCon",
          "location": {
            "lat": "37.7821120598956",
            "long": "-122.400612831116"
          },
          "nsfw": false
        }""",
      andExpect = Created,
      withJsonBody = """
        {
          "id": "123",
          "message": "Hello #FinagleCon",
          "location": {
            "lat": "37.7821120598956",
            "long": "-122.400612831116"
          },
          "nsfw": false
        }""")

    server.httpGetJson[TweetResponse](
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
          "nsfw": "abc"
        }""",
      andExpect = BadRequest,
      withJsonBody = """
        {
          "errors" : [
            "location.lat: [9999.0] is not between -85 and 85",
            "location.long: field is required",
            "message: size [0] is not between 1 and 140",
            "nsfw: 'abc' is not a valid boolean"
          ]
        }
        """)
  }
}
```

## Detailed Documentation

The Finatra project is composed of several libraries. You can find details in a project's README or see the [User Guide][user-guide] for detailed information on building applications with Finatra.

## Example Projects

For more detailed information see the README.md within each example project.

### [hello-world](./examples/hello-world/README.md)
A barebones "Hello World" service.

### [hello-world-heroku](./examples/hello-world-heroku/README.md)
A barebones service that is deployable to [Heroku](https://heroku.com).

### [tiny-url](examples/tiny-url/README.md)
A url shortening example that is deployable to [Heroku](https://heroku.com).

### [twitter-clone](examples/twitter-clone/README.md)
An example Twitter-like API for creating and retrieving Tweets.

### [benchmark-server](examples/benchmark-server/README.md)
A server used for benchmarking performance compared to a raw finagle-http service.

### [streaming](examples/streaming-example/README.md)
A proof-of-concept streaming JSON service.

## Authors

* Steve Cosenza <https://github.com/scosenza>
* Christopher Coco <https://github.com/cacoco>

A full list of [contributors](https://github.com/twitter/finatra/graphs/contributors?type=a) can be found on GitHub.

Follow [@finatra](http://twitter.com/finatra) on Twitter for updates.


## License

Copyright 2013-2016 Twitter, Inc.

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
[twitter-clone-example]: examples/twitter-clone/
[maven-central]: http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.twitter.finatra%22
[user-guide]: http://twitter.github.io/finatra/user-guide/
