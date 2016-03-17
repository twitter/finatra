# Finatra Java Http Server Example Application

### NOTE: this example *only* works with Java 8

If you are trying to use this as an example of how to write an http server with Finatra, please note that the syntax **only** functions properly in JDK 8. Please see the [java-server](examples/java-server) for how to develop a java server for java versions < JDK 8.

This example is also not available in the root build, thus it's only

* A simple "hello world" example for JDK 8.

If you're in a tagged release branch (e.g. [v2.1.5](https://github.com/twitter/finatra/tree/v2.1.5))
----------------------------------------------------------
###SBT###
Run sbt from **this** project's directory, e.g.
```
$ sbt run
```
Or build and run a deployable jar:
```
$ sbt assembly
$ java -jar -Dlog.service.output=java-server.log inject/examples/java-http-server/target/scala-2.11/java-http-server-assembly-2.1.5.jar -http.port=:8888 -admin.port=:9990
```