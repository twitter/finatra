# Finatra Tiny URL Example Application

* Finatra examples are built in different ways depending on the branch you are in:

If you're in master or a feature branch
----------------------------------------------------------
* Development from master or feature branches is not currently supported for this example. Please switch to a release branch and see the instructions below.

If you're in a tagged release branch (e.g. [finatra-2.2.0](https://github.com/twitter/finatra/tree/finatra-2.2.0))
----------------------------------------------------------

### Building

To build:

```
$ sbt compile stage
```

### Running

To run locally using foreman:

```
$ ➔ PORT=8080 REDIS_URL=redis://127.0.0.1:12345 foreman start web
15:15:53 web.1     | started with pid 7184
15:15:55 web.1     | 2015-06-28 15:15:55,802 INF                           TinyUrlServerMain$         Process started
15:15:56 web.1     | I 0628 22:15:56.348 THREAD1: HttpMuxer[/admin/metrics.json] = com.twitter.finagle.stats.MetricsExporter(<function1>)
15:15:56 web.1     | I 0628 22:15:56.407 THREAD1: HttpMuxer[/admin/per_host_metrics.json] = com.twitter.finagle.stats.HostMetricsExporter(<function1>)
15:15:56 web.1     | I 0628 22:15:56.533 THREAD1: /admin => com.twitter.server.handler.SummaryHandler
15:15:56 web.1     | I 0628 22:15:56.534 THREAD1: /admin/server_info => com.twitter.finagle.Filter$$anon$1
15:15:56 web.1     | I 0628 22:15:56.534 THREAD1: /admin/contention => com.twitter.finagle.Filter$$anon$1
15:15:56 web.1     | I 0628 22:15:56.534 THREAD1: /admin/threads => com.twitter.server.handler.ThreadsHandler
15:15:56 web.1     | I 0628 22:15:56.534 THREAD1: /admin/threads.json => com.twitter.server.handler.ThreadsHandler
15:15:56 web.1     | I 0628 22:15:56.535 THREAD1: /admin/announcer => com.twitter.finagle.Filter$$anon$1
15:15:56 web.1     | I 0628 22:15:56.535 THREAD1: /admin/dtab => com.twitter.finagle.Filter$$anon$1
15:15:56 web.1     | I 0628 22:15:56.535 THREAD1: /admin/pprof/heap => com.twitter.server.handler.HeapResourceHandler
15:15:56 web.1     | I 0628 22:15:56.535 THREAD1: /admin/pprof/profile => com.twitter.server.handler.ProfileResourceHandler
15:15:56 web.1     | I 0628 22:15:56.536 THREAD1: /admin/pprof/contention => com.twitter.server.handler.ProfileResourceHandler
15:15:56 web.1     | I 0628 22:15:56.536 THREAD1: /admin/ping => com.twitter.server.handler.ReplyHandler
15:15:56 web.1     | I 0628 22:15:56.537 THREAD1: /admin/shutdown => com.twitter.server.handler.ShutdownHandler
15:15:56 web.1     | I 0628 22:15:56.537 THREAD1: /admin/tracing => com.twitter.server.handler.TracingHandler
15:15:56 web.1     | I 0628 22:15:56.537 THREAD1: /admin/events => com.twitter.server.handler.EventsHandler
15:15:56 web.1     | I 0628 22:15:56.538 THREAD1: /admin/logging => com.twitter.server.handler.LoggingHandler
15:15:56 web.1     | I 0628 22:15:56.538 THREAD1: /admin/metrics => com.twitter.server.handler.MetricQueryHandler
15:15:56 web.1     | I 0628 22:15:56.539 THREAD1: /admin/clients/ => com.twitter.server.handler.ClientRegistryHandler
15:15:56 web.1     | I 0628 22:15:56.539 THREAD1: /admin/servers/ => com.twitter.server.handler.ServerRegistryHandler
15:15:56 web.1     | I 0628 22:15:56.540 THREAD1: /admin/files/ => com.twitter.server.handler.ResourceHandler
15:15:56 web.1     | I 0628 22:15:56.540 THREAD1: /admin/registry.json => com.twitter.server.handler.RegistryHandler
15:15:56 web.1     | I 0628 22:15:56.542 THREAD1: Serving admin http on 0.0.0.0/0.0.0.0:0
15:15:56 web.1     | I 0628 22:15:56.623 THREAD1: Finagle version 6.25.0 (rev=78909170b7cc97044481274e297805d770465110) built at 20150423-135046
15:15:57 web.1     | 2015-06-28 15:15:57,854 INF                           RedisClientModule         Configured Redis URL: redis://127.0.0.1:12345
15:15:58 web.1     | 2015-06-28 15:15:58,650 DEB                           MessageBodyModule$        Configuring MessageBodyManager
15:15:58 web.1     | 2015-06-28 15:15:58,692 INF                           TinyUrlServerMain$         Resolving Finagle clients before warmup
15:15:58 web.1     | 2015-06-28 15:15:58,700 INF                           finagle                   client resolved to Addr.Bound, current size=1
15:15:58 web.1     | 2015-06-28 15:15:58,705 INF                           TinyUrlServerMain$         Done resolving clients: [client].
15:15:58 web.1     | 2015-06-28 15:15:58,881 INF                           TinyUrlServerMain$         Warming up.
15:15:59 web.1     | 2015-06-28 15:15:59,002 INF                           HttpRouter                Adding routes
15:15:59 web.1     | POST    /url
15:15:59 web.1     | GET     /:id
15:15:59 web.1     | 2015-06-28 15:15:59,164 INF                           DefaultTracer$            Tracer: com.twitter.finagle.zipkin.thrift.SamplingTracer
15:15:59 web.1     | 2015-06-28 15:15:59,179 INF                           TinyUrlServerMain$         http server started on port: 5000
15:15:59 web.1     | 2015-06-28 15:15:59,180 INF                           TinyUrlServerMain$         Enabling health endpoint on port 50562
15:15:59 web.1     | 2015-06-28 15:15:59,180 INF                           TinyUrlServerMain$         App started.
15:15:59 web.1     | 2015-06-28 15:15:59,181 INF                           TinyUrlServerMain$         Startup complete, server ready.
```


To run in [Heroku](https://www.heroku.com): Make sure you have the [Heroku Toolbelt](https://toolbelt.heroku.com/) [installed](https://devcenter.heroku.com/articles/getting-started-with-scala#set-up).

Create a new app in Heroku:

```
$ heroku create
Creating nameless-lake-8055 in organization heroku... done, stack is cedar-14
http://nameless-lake-8055.herokuapp.com/ | https://git.heroku.com/nameless-lake-8055.git
Git remote heroku added
```

Then create a Redis add-on:

```
$ heroku addons:create heroku-redis:hobby-dev
```

Then deploy the example application to Heroku:

```
$ git push heroku master
Counting objects: 480, done.
Delta compression using up to 4 threads.
Compressing objects: 100% (376/376), done.
Writing objects: 100% (480/480), 27.68 MiB | 16.24 MiB/s, done.
Total 480 (delta 101), reused 0 (delta 0)
remote: Compressing source files... done.
remote: Building source:
...
```

Tail the Heroku logs:

```
$ heroku logs -t
```

### Using

To create a new shortened url: post a JSON body to the `/url` endpoint in the form of `{"url":"TO_SHORTEN_URL"}`

```
$ curl -i -H "Content-Type: application/json" -X POST -d '{"url":"http://www.nytimes.com/2012/05/06/travel/36-hours-in-barcelona-spain.html"}' http://127.0.0.1:8080/url
HTTP/1.1 201 Created
Content-Type: application/json; charset=utf-8
Content-Length: 44

{"tiny_url":"http://127.0.0.1:8080/9h5k4"}
```

Then in a browser paste the shortened URL to be redirected to the original URL or use [curl](http://curl.haxx.se/docs/manual.html):

```
$ curl -i http://127.0.0.1:8080/9h5k4
HTTP/1.1 301 Moved Permanently
Location: http://www.nytimes.com/2012/05/06/travel/36-hours-in-barcelona-spain.html
Content-Length: 0
```
