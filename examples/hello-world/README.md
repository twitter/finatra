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
* Or view the [twitter-server admin interface](https://twitter.github.io/twitter-server/Features.html#http-admin-interface): [http://localhost:9990/admin](http://localhost:9990/admin)
* Or build and run a deployable jar:
```
$ sbt helloWorld/assembly
$ java -jar examples/hello-world/target/scala-2.11/finatra-hello-world-assembly-2.x.x-SNAPSHOT.jar -http.port=:8888 -admin.port=:9990
```

If you're in a tagged release branch (e.g. [v2.1.2](https://github.com/twitter/finatra/tree/v2.1.2))
----------------------------------------------------------
###SBT###
Run sbt from this project's directory, e.g.
```
$ sbt run
```
Or build and run a deployable jar:
```
$ sbt assembly
$ java -jar target/scala-2.11/finatra-hello-world-assembly-2.1.2.jar -http.port=:8888 -admin.port=:9990
```
With `sbt-revolver` plugin, server can be automatically restarted when code change.
```
sbt ~re-start
```

###Maven###
```
mvn clean install
```
