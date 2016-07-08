# Finatra Streaming Example

Finatra examples are built in different ways depending on the branch you are in:

If you're in master or a feature branch
----------------------------------------------------------
Run sbt from the top-level Finatra directory, e.g.
```
$ cd ../../
$ sbt streamingExample/run
```
* Then browse the [twitter-server admin interface](https://twitter.github.io/twitter-server/Features.html#admin-http-interface): [http://localhost:9990/admin](http://localhost:9990/admin)
* Or build and run a deployable jar:
```
$ sbt streamingExample/assembly
$ java -jar examples/streaming-example/target/scala-2.11/finatra-benchmark-server-assembly-2.x.x-SNAPSHOT.jar -http.port=:8888 -admin.port=:9990
```

If you're in a tagged release branch (e.g. [finatra-2.2.0](https://github.com/twitter/finatra/tree/finatra-2.2.0))
----------------------------------------------------------
Run sbt from **this** project's directory, e.g.
```
$ sbt run
```
* Then browse to the [twitter-server admin interface](https://twitter.github.io/twitter-server/Features.html#admin-http-interface): [http://localhost:9990/admin](http://localhost:9990/admin)
Or build and run a deployable jar:
```
$ sbt assembly
$ java -jar target/scala-2.11/streaming-example-assembly-2.2.0.jar -http.port=:8888 -admin.port=:9990
```
