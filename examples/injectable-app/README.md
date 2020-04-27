# Finatra Hello World HttpServer Example Application

A simple "hello world" HTTP example.

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
[finatra] $ ./sbt "project javaInjectableApp" "run -username=Alice"
```
* Or build and run a deployable jar:
```
[finatra] $ ./sbt javaInjectableApp/assembly
[finatra] $ java -jar examples/injectable-app/java/target/scala-2.XX/java-app-assembly-X.XX.X.jar -username=Alice
``` 

Scala version:
```
[finatra] $ ./sbt "project scalaInjectableApp" "run -username=Alice"
```
* Or build and run a deployable jar:
```
[finatra] $ ./sbt scalaHttpServer/assembly
[finatra] $ java -jar examples/injectable-app/scala/target/scala-2.XX/scala-app-assembly-X.XX.X.jar -username=Alice
```
