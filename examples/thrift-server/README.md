# Finatra Thrift Server Example Application

A simple thrift server example.

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

Run all commands from the base `/finatra` directory.

Java version:
```
[finatra] $ ./sbt "project javaThriftServer" "run -thrift.port=:9999 -admin.port=:9990"
```
* Then browse the [twitter-server admin interface](https://twitter.github.io/twitter-server/Features.html#admin-http-interface): [http://localhost:9990/admin](http://localhost:9990/admin)
* Or build and run a deployable jar:
```
[finatra] $ ./sbt javaThriftServer/assembly
[finatra] $ java -jar examples/thrift/java/target/scala-2.11/java-thrift-server-assembly-X.XX.X.jar -thrift.port=:9999 -admin.port=:9990
```

Scala version:
```
[finatra] $ ./sbt "project scalaThriftServer" "run -thrift.port=:9999 -admin.port=:9990"
```
* Then browse the [twitter-server admin interface](https://twitter.github.io/twitter-server/Features.html#admin-http-interface): [http://localhost:9990/admin](http://localhost:9990/admin)
* Or build and run a deployable jar:
```
[finatra] $ ./sbt scalaThriftServer/assembly
[finatra] $ java -jar examples/thrift/scala-thrift-server/target/scala-2.11/scala-thrift-server-assembly-X.XX.X.jar -thrift.port=:9999 -admin.port=:9990
```
