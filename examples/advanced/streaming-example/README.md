# Finatra Streaming Example

A simple HTTP server which streams responses.

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
[finatra] $ ./sbt "project streamingExample" "run -http.port=:8888 -admin.port=:9990"
```
* Then browse the [twitter-server admin interface](https://twitter.github.io/twitter-server/Features.html#admin-http-interface): [http://localhost:9990/admin](http://localhost:9990/admin)
* Or build and run a deployable jar:
```
[finatra] $ ./sbt streamingExample/assembly
[finatra] $ java -jar examples/streaming-example/target/scala-2.XX/streaming-example-assembly-X.XX.X.jar -http.port=:8888 -admin.port=:9990
```
