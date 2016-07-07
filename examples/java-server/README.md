# Finatra Inject Java Server

Finatra examples are built in different ways depending on the branch you are in:

If you're in master or a feature branch
----------------------------------------------------------
Run sbt from the top-level Finatra directory, e.g.
```
$ cd ../../
$ sbt exampleInjectJavaServer/run
```

* Or build and run a deployable jar:
```
$ sbt exampleInjectJavaServer/assembly
$ java -jar -Dlog.service.output=java-server.log inject/examples/java-server/target/scala-2.11/java-server-assembly-2.x.x-SNAPSHOT.jar -http.port=:8888 -admin.port=:9990
```
*Note*: adding the java args `-Dlog.service.output` and `-Dlog.access.output` is optional and they can be set to any location on disk or to `/dev/stdout` or `/dev/stderr` for capturing log output. When not set the [logback.xml](./src/main/resources/logback.xml) is parameterized with defaults of `service.log` and `access.log`, respectively.

If you're in a tagged release branch (e.g. [finatra-2.2.0](https://github.com/twitter/finatra/tree/finatra-2.2.0))
----------------------------------------------------------
###SBT###
Run sbt from **this** project's directory, e.g.
```
$ sbt run
```
Or build and run a deployable jar:
```
$ sbt assembly
$ java -jar -Dlog.service.output=java-server.log inject/examples/java-server/target/scala-2.11/java-server-assembly-2.2.0.jar -http.port=:8888 -admin.port=:9990
```
