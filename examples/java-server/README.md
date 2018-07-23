# Finatra Java Server and Java Application Examples

A simple "Hello World" TwitterServer and application both written in Java.

Note: All Finatra examples should be run from the base Finatra directory as they are defined as part 
of the root project.

Building
--------

For any branch that is not [Master](https://github.com/twitter/finatra/tree/master) or a tagged 
[release branch](https://github.com/twitter/finatra/releases) (or a branch based on one of those 
branches), see the [CONTRIBUTING.md](../../CONTRIBUTING.md#building-dependencies) documentation on 
building Finatra and its dependencies locally in order to run the examples.

Running the [TwitterServer](https://twitter.github.io/twitter-server/)
----------------------------------------------------------------------
```
[finatra] $ cd ../../
[finatra] $ ./sbt "project exampleInjectJavaServer" "runMain com.twitter.hello.server.HelloWorldServerMain -admin.port=:9990"
```
* Then browse the [twitter-server admin interface](https://twitter.github.io/twitter-server/Features.html#admin-http-interface): [http://localhost:9990/admin](http://localhost:9990/admin)
* Or build and run a deployable jar:
```
[finatra] $ ./sbt exampleInjectJavaServer/assembly
[finatra] $ java -jar examples/java-server/target/scala-2.XX/java-server-assembly-X.XX.X.jar -admin.port=:9990
```

Running the [Application](https://github.com/twitter/util/blob/develop/util-app/src/main/scala/com/twitter/app/App.scala)
-------------------------------------------------------------------------------------------------------------------------
```
[finatra] $ cd ../../
[finatra] $ ./sbt "project exampleInjectJavaServer" "runMain com.twitter.hello.app.HelloWorldAppMain"
```
