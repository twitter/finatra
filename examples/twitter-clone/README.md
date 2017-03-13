# Finatra Twitter Clone Example

* Note: Finatra examples are built in different ways depending on the branch.

[Master](https://github.com/twitter/finatra/tree/master) or a tagged release branch (e.g. [finatra-2.9.0](https://github.com/twitter/finatra/tree/finatra-2.9.0))
----------------------------------------------------------
Run sbt from **this** project's directory, e.g.
```
$ sbt "run -firebase.host=finatra.firebaseio.com -com.twitter.server.resolverMap=firebase=finatra.firebaseio.com:443"
```
* Then browse to: [http://127.0.0.1:8888/tweet/04fa10f9-8188-4bd2-b4e0-46fd09b77aa9](http://127.0.0.1:8888/tweet/04fa10f9-8188-4bd2-b4e0-46fd09b77aa9)
* Or view the [twitter-server admin interface](https://twitter.github.io/twitter-server/Features.html#admin-http-interface): [http://localhost:9990/admin](http://localhost:9990/admin)
Or build and run a deployable jar:
```
$ sbt assembly
$ java -jar -Dlog.service.output=twitter-clone.log -Dlog.access.output=access.log target/scala-2.11/twitter-clone-assembly-2.9.0.jar -http.port=:8888 -admin.port=:9990 -firebase.host=finatra.firebaseio.com -com.twitter.server.resolverMap=firebase=finatra.firebaseio.com:443
```

Any other branch
----------------
See the [CONTRIBUTING.md](../../CONTRIBUTING.md#building-dependencies) documentation on building Finatra dependencies in order to run the examples.

Run sbt from the top-level Finatra directory, e.g.
```
$ cd ../../
$ JAVA_OPTS="-Dlog.service.output=/dev/stdout -Dlog.access.output=/dev/stdout" ./sbt "project twitterClone" "run -firebase.host=finatra.firebaseio.com -com.twitter.server.resolverMap=firebase=finatra.firebaseio.com:443"
```
* Then browse to: [http://127.0.0.1:8888/tweet/04fa10f9-8188-4bd2-b4e0-46fd09b77aa9](http://127.0.0.1:8888/tweet/04fa10f9-8188-4bd2-b4e0-46fd09b77aa9)
* Or view the [twitter-server admin interface](https://twitter.github.io/twitter-server/Features.html#admin-http-interface): [http://localhost:9990/admin](http://localhost:9990/admin)
* Or build and run a deployable jar:
```
$ ./sbt twitterClone/assembly
$ java -jar -Dlog.service.output=twitter-clone.log -Dlog.access.output=access.log examples/twitter-clone/target/scala-2.11/twitter-clone-assembly-2.10.0-SNAPSHOT.jar -http.port=:8888 -admin.port=:9990
```
*Note*: adding the java args `-Dlog.service.output` and `-Dlog.access.output` is optional and they can be set to any location on disk or to `/dev/stdout` or `/dev/stderr` for capturing log output. When not set the [logback.xml](./src/main/resources/logback.xml) is parameterized with defaults of `service.log` and `access.log`, respectively.
