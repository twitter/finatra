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
```
[finatra] $ cd ../../
[finatra] $ ./sbt "project thriftExampleServer" "run -thrift.port=:9999 -admin.port=:9990"
```
* Then browse the [twitter-server admin interface](https://twitter.github.io/twitter-server/Features.html#admin-http-interface): [http://localhost:9990/admin](http://localhost:9990/admin)
* Or build and run a deployable jar:
```
[finatra] $ ./sbt thriftExampleServer/assembly
[finatra] $ java -jar examples/thrift-server/thrift-example-server/target/scala-2.11/thrift-example-server-assembly-X.XX.X.jar -thrift.port=:9999 -admin.port=:9990
```
