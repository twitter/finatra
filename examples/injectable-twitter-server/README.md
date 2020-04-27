# TwitterServer Example

A simple TwitterServer modeling a Publisher/Subscriber example.

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
[finatra] $ ./sbt "project javaInjectableTwitterServer" "run -admin.port=:9990"
```
* Then browse the [twitter-server admin interface](https://twitter.github.io/twitter-server/Features.html#admin-http-interface): [http://localhost:9990/admin](http://localhost:9990/admin)
* Or build and run a deployable jar:
```
[finatra] $ ./sbt javaInjectableTwitterServer/assembly
[finatra] $ java -jar examples/java-twitter-server/java/target/scala-2.XX/java-twitter-server-assembly-X.XX.X.jar -admin.port=:9990
```

Scala version:
```
[finatra] $ ./sbt "project scalaInjectableTwitterServer" "run -admin.port=:9990"
```
* Then browse the [twitter-server admin interface](https://twitter.github.io/twitter-server/Features.html#admin-http-interface): [http://localhost:9990/admin](http://localhost:9990/admin)
* Or build and run a deployable jar:
```
[finatra] $ ./sbt scalaInjectableTwitterServer/assembly
[finatra] $ java -jar examples/scala-twitter-server/scala/target/scala-2.XX/scala-twitter-server-assembly-X.XX.X.jar -admin.port=:9990
```
