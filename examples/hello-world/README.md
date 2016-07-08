# Finatra Hello World Example Application

* A simple "hello world" example.
* Note: Finatra examples are built in different ways depending on the branch you are in:

If you're in master or a feature branch
----------------------------------------------------------
Run sbt from the top-level Finatra directory, e.g.
```
$ cd ../../
$ sbt helloWorld/run
```
* Then browse to: [http://localhost:8888/hi?name=foo](http://localhost:8888/hi?name=foo)
* Or view the [twitter-server admin interface](https://twitter.github.io/twitter-server/Features.html#admin-http-interface): [http://localhost:9990/admin](http://localhost:9990/admin)
* Or build and run a deployable jar:
```
$ sbt helloWorld/assembly
$ java -jar -Dlog.service.output=hello-world.log -Dlog.access.output=access.log examples/hello-world/target/scala-2.11/finatra-hello-world-assembly-2.x.x-SNAPSHOT.jar -http.port=:8888 -admin.port=:9990
```
*Note*: adding the java args `-Dlog.service.output` and `-Dlog.access.output` is optional and they can be set to any location on disk or to `/dev/stdout` or `/dev/stderr` for capturing log output. When not set the [logback.xml](./src/main/resources/logback.xml) is parameterized with defaults of `service.log` and `access.log`, respectively.

If you're in a tagged release branch (e.g. [finatra-2.2.0](https://github.com/twitter/finatra/tree/finatra-2.2.0))
----------------------------------------------------------
###SBT###
Run sbt from **this** project's directory, e.g.
```
$ sbt run
```
Or with [sbt-revolver](https://github.com/spray/sbt-revolver):
```
$ sbt "~re-start"
```
Which is "triggered restart" mode via the sbt-revolver plugin. Your application starts up and sbt watches for changes in your source (or resource) files.
If a change is detected sbt re-compiles the required classes and the sbt-revolver plugin automatically restarts your application.
When you press <ENTER> sbt leaves "triggered restart" mode and returns to the normal prompt keeping your application running.
For more information on "triggered restart" mode see the sbt-revolver documentation [here](https://github.com/spray/sbt-revolver/blob/master/README.md#triggered-restart).

Or build and run a deployable jar:
```
$ sbt assembly
$ java -jar -Dlog.service.output=hello-world.log -Dlog.access.output=access.log target/scala-2.11/finatra-hello-world-assembly-2.2.0.jar -http.port=:8888 -admin.port=:9990
```

###Maven###
```
mvn clean install
```
