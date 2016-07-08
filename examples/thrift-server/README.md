# Finatra Thrift Server Example Application

* A simple thrift server example.
* Note: Finatra examples are built in different ways depending on the branch you are in:

If you are in master or a feature branch
----------------------------------------------------------
Run sbt from the top-level Finatra directory, e.g.
```
$ cd ../../
$ sbt thriftExampleServer/run
```

* Or build and run a deployable jar:
```
$ sbt thriftExampleServer/assembly
$ java -jar -Dlog.service.output=thrift-server.log -Dlog.access.output=access.log examples/thrift-server/thrift-example-server/target/scala-2.11/thrift-example-server-assembly-2.x.x-SNAPSHOT.jar -thrift.port=:9999 -admin.port=:9990
```
*Note*: adding the java args `-Dlog.service.output` and `-Dlog.access.output` is optional and they can be set to any location on disk or to `/dev/stdout` or `/dev/stderr` for capturing log output. When not set the [logback.xml](./thrift-example-server/src/main/resources/logback.xml) is parameterized with defaults of `service.log` and `access.log`, respectively.

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
$ java -jar -Dlog.service.output=thrift-server.log -Dlog.access.output=access.log target/scala-2.11/thrift-example-server-assembly-2.2.0.jar -thrift.port=:9999 -admin.port=:9990
```
