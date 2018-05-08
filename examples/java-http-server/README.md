# Finatra Hello World HttpServer Java Example Application

A simple "hello world" HTTP server example in Java.

### NOTE: this example requires [Java 8](https://www.oracle.com/technetwork/java/javase/overview/java8-2100321.html)

Note: All Finatra examples should be run from the base Finatra directory as they are defined as part 
of the root project.

Building
--------

For any branch that is not [Master](https://github.com/twitter/finatra/tree/master) or a tagged 
[release branch](https://github.com/twitter/finatra/releases) (or a branch based on one of those 
branches), see the [CONTRIBUTING.md](../../CONTRIBUTING.md#building-dependencies) documentation on 
building Finatra and its dependencies locally in order to run the examples.

Running
-------
```
[finatra] $ cd ../../
[finatra] $ ./sbt exampleHttpJavaServer/run
```
* Then browse to: [http://localhost:8888/hi?name=foo](http://localhost:8888/hi?name=foo)
* Or view the [twitter-server admin interface](https://twitter.github.io/twitter-server/Features.html#admin-http-interface): [http://localhost:9990/admin](http://localhost:9990/admin)
* Or build and run a deployable jar:
```
[finatra] $ ./sbt exampleHttpJavaServer/assembly
[finatra] $ java -jar -Dlog.service.output=java-http-server.log -Dlog.access.output=access.log examples/java-http-server/target/scala-2.XX/java-http-server-assembly-X.XX.X.jar -http.port=:8888 -admin.port=:9990
```
*Note*: adding the java args `-Dlog.service.output` and `-Dlog.access.output` is optional and they 
can be set to any location on disk or to `/dev/stdout` or `/dev/stderr` for capturing log output. 
When not set the [logback.xml](./src/main/resources/logback.xml) is parameterized with defaults of 
`service.log` and `access.log`, respectively.
