# Finatra Hello World HttpServer Java Example Application

### NOTE: this example *only* works with Java 8

* A simple "hello world" HTTP server example in Java.
* Note: Finatra examples are built in different ways depending on the branch you are in:

If you're in master or a feature branch
----------------------------------------------------------
Run sbt from the top-level Finatra directory, e.g.
```
$ cd ../../
$ ./sbt exampleHttpJavaServer/run
```
* Then browse to: [http://localhost:8888/hi?name=foo](http://localhost:8888/hi?name=foo)
* Or view the [twitter-server admin interface](https://twitter.github.io/twitter-server/Features.html#admin-http-interface): [http://localhost:9990/admin](http://localhost:9990/admin)
* Or build and run a deployable jar:
```
$ ./sbt exampleHttpJavaServer/assembly
$ java -jar -Dlog.service.output=java-http-server.log -Dlog.access.output=access.log examples/java-http-server/target/scala-2.11/java-http-server-assembly-2.5.0-SNAPSHOT.jar -http.port=:8888 -admin.port=:9990
```
*Note*: adding the java args `-Dlog.service.output` and `-Dlog.access.output` is optional and they can be set to any location on disk or to `/dev/stdout` or `/dev/stderr` for capturing log output. When not set the [logback.xml](./src/main/resources/logback.xml) is parameterized with defaults of `service.log` and `access.log`, respectively.

If you're in a tagged release branch (e.g. [finatra-2.5.0](https://github.com/twitter/finatra/tree/finatra-2.5.0))
----------------------------------------------------------
###SBT###
Run sbt from **this** project's directory, e.g.
```
$ sbt run
```

Or build and run a deployable jar:
```
$ sbt assembly
$ java -jar -Dlog.service.output=java-http-server.log -Dlog.access.output=access.log target/scala-2.11/java-http-server-assembly-2.5.0.jar -http.port=:8888 -admin.port=:9990
```
