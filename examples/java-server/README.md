# Finatra Inject Java Server

* Note: Finatra examples are built in different ways depending on the branch.

[Master](https://github.com/twitter/finatra/tree/master) or a tagged release branch (e.g. [finatra-2.6.0](https://github.com/twitter/finatra/tree/finatra-2.6.0))
----------------------------------------------------------
Run sbt from **this** project's directory, e.g.
```
$ sbt run
```
Or build and run a deployable jar:
```
$ sbt assembly
$ java -jar -Dlog.service.output=java-server.log target/scala-2.11/java-server-assembly-2.6.0.jar -http.port=:8888 -admin.port=:9990
```

Any other branch
----------------
See the [CONTRIBUTING.md](../../CONTRIBUTING.md#building-dependencies) documentation on building Finatra dependencies in order to run the examples.

Run sbt from the top-level Finatra directory, e.g.
```
$ cd ../../
$ JAVA_OPTS="-Dlog.service.output=/dev/stdout -Dlog.access.output=/dev/stdout" ./sbt exampleInjectJavaServer/run
```

* Or build and run a deployable jar:
```
$ ./sbt exampleInjectJavaServer/assembly
$ java -jar -Dlog.service.output=java-server.log examples/java-server/target/scala-2.11/java-server-assembly-2.7.0-SNAPSHOT.jar -http.port=:8888 -admin.port=:9990
```
*Note*: adding the java args `-Dlog.service.output` and `-Dlog.access.output` is optional and they can be set to any location on disk or to `/dev/stdout` or `/dev/stderr` for capturing log output. When not set the [logback.xml](./src/main/resources/logback.xml) is parameterized with defaults of `service.log` and `access.log`, respectively.
