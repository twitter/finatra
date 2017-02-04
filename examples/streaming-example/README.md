# Finatra Streaming Example

* Note: Finatra examples are built in different ways depending on the branch.

[Master](https://github.com/twitter/finatra/tree/master) or a tagged release branch (e.g. [finatra-2.8.0](https://github.com/twitter/finatra/tree/finatra-2.8.0))
----------------------------------------------------------
Run sbt from **this** project's directory, e.g.
```
$ sbt run
```
* Then browse to the [twitter-server admin interface](https://twitter.github.io/twitter-server/Features.html#admin-http-interface): [http://localhost:9990/admin](http://localhost:9990/admin)
Or build and run a deployable jar:
```
$ sbt assembly
$ java -jar target/scala-2.11/streaming-example-assembly-2.8.0.jar -http.port=:8888 -admin.port=:9990
```

Any other branch
----------------
See the [CONTRIBUTING.md](../../CONTRIBUTING.md#building-dependencies) documentation on building Finatra dependencies in order to run the examples.

Run sbt from the top-level Finatra directory, e.g.
```
$ cd ../../
$ JAVA_OPTS="-Dlog.service.output=/dev/stdout -Dlog.access.output=/dev/stdout" ./sbt streamingExample/run
```
* Then browse the [twitter-server admin interface](https://twitter.github.io/twitter-server/Features.html#admin-http-interface): [http://localhost:9990/admin](http://localhost:9990/admin)
* Or build and run a deployable jar:
```
$ ./sbt streamingExample/assembly
$ java -jar examples/streaming-example/target/scala-2.11/streaming-example-assembly-2.9.0-SNAPSHOT.jar -http.port=:8888 -admin.port=:9990
```
