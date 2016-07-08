# Finatra Twitter Clone Example

Finatra examples are built in different ways depending on the branch you are in:

If you're in master or a feature branch
----------------------------------------------------------
Run sbt from the top-level Finatra directory, e.g.
```
$ cd ../../
$ sbt "project twitterClone" "run -firebase.host=finatra.firebaseio.com -com.twitter.server.resolverMap=firebase=finatra.firebaseio.com:443"
```
* Then browse to: [http://127.0.0.1:8888/tweet/04fa10f9-8188-4bd2-b4e0-46fd09b77aa9](http://127.0.0.1:8888/tweet/04fa10f9-8188-4bd2-b4e0-46fd09b77aa9)
* Or view the [twitter-server admin interface](https://twitter.github.io/twitter-server/Features.html#admin-http-interface): [http://localhost:9990/admin](http://localhost:9990/admin)
* Or build and run a deployable jar:
```
$ sbt twitterClone/assembly
$ java -jar -Dlog.service.output=twitter-clone.log -Dlog.access.output=access.log examples/benchmark-server/target/scala-2.11/finatra-benchmark-server-assembly-2.x.x-SNAPSHOT.jar -http.port=:8888 -admin.port=:9990
```
*Note*: adding the java args `-Dlog.service.output` and `-Dlog.access.output` is optional and they can be set to any location on disk or to `/dev/stdout` or `/dev/stderr` for capturing log output. When not set the [logback.xml](./src/main/resources/logback.xml) is parameterized with defaults of `service.log` and `access.log`, respectively.

If you're in a tagged release branch (e.g. [finatra-2.2.0](https://github.com/twitter/finatra/tree/finatra-2.2.0))
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
$ java -jar -Dlog.service.output=twitter-clone.log -Dlog.access.output=access.log target/scala-2.11/finatra-twitter-clone-assembly-2.2.0.jar -http.port=:8888 -admin.port=:9990 -firebase.host=finatra.firebaseio.com -com.twitter.server.resolverMap=firebase=finatra.firebaseio.com:443
```
