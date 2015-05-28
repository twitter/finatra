# Finatra Hello World Example Application

An example finatra application that highlights features of the [finatra-http](../../http) framework.

Run the example server locally with sbt:
-----------------------------------------------------------

```
$ cd finatra/examples/finatra-hello-world
$ ./sbt -Dlogback.configurationFile=src/test/resources/logback-test.xml run
```

Browse to: [http://localhost:8888/hi?name=foo](http://localhost:8888/hi?name=foo)

Or to view the [twitter-server admin interface](https://twitter.github.io/twitter-server/Features.html#http-admin-interface): [http://localhost:9990/admin](http://localhost:9990/admin)

Build and run a deployable jar:
-----------------------------------------------------------

```
$ ./sbt assembly
$ java -Dlogback.configurationFile=src/test/resources/logback-test.xml -jar target/scala-2.11/finatra-hello-world-assembly-1.0.0-SNAPSHOT.jar -http.port=:8888 -admin.port=:9990
```

