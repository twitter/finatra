# Finatra Twitter Clone Example

A more detailed example that builds a clone of Twitter.

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
[finatra] $ JAVA_OPTS="-Dlog.service.output=/dev/stdout -Dlog.access.output=/dev/stdout" ./sbt "project twitterClone" "run -firebase.host=finatra.firebaseio.com -com.twitter.server.resolverMap=firebase=finatra.firebaseio.com:443"
```
* Then browse to: [http://127.0.0.1:8888/tweet/04fa10f9-8188-4bd2-b4e0-46fd09b77aa9](http://127.0.0.1:8888/tweet/04fa10f9-8188-4bd2-b4e0-46fd09b77aa9)
* Or view the [twitter-server admin interface](https://twitter.github.io/twitter-server/Features.html#admin-http-interface): [http://localhost:9990/admin](http://localhost:9990/admin)
* Or build and run a deployable jar:
```
[finatra] $ ./sbt twitterClone/assembly
[finatra] $ java -jar -Dlog.service.output=twitter-clone.log -Dlog.access.output=access.log examples/twitter-clone/target/scala-2.XX/twitter-clone-assembly-X.XX.X.jar -http.port=:8888 -admin.port=:9990
```
*Note*: adding the java args `-Dlog.service.output` and `-Dlog.access.output` is optional and they 
can be set to any location on disk or to `/dev/stdout` or `/dev/stderr` for capturing log output. 
When not set the [logback.xml](./src/main/resources/logback.xml) is parameterized with defaults of 
`service.log` and `access.log`, respectively.
