# Finatra Benchmarks

We use [JMH](http://openjdk.java.net/projects/code-tools/jmh/) as our benchmarking framework.

## Tests

The current benchmarks are defined with corresponding tests and you can run a specific test to 
exercise the benchmark code (though obviously, this will only run the benchmark logic once). 
E.g., with sbt:

```
[finatra]$ ./sbt "project benchmarks" test
[info] Loading global plugins from .sbt/1.0/plugins
[info] Loading settings from plugins.sbt,build.sbt ...
[info] Loading project definition from finatra/project
[info] Loading settings from build.sbt ...
[info] Loading settings from build.sbt ...
[info] Resolving key references (23330 settings) ...
[info] Set current project to root (in build file:finatra/)
[info] Set current project to finatra-benchmarks (in build file:finatra/)
[info] Compiling 10 Scala sources to finatra/benchmarks/target/scala-2.12/test-classes ...
[info] Done compiling.
[pool-1-thread-1] INFO com.twitter.finatra.http.routing.HttpRouter - Adding routes
GET     /plaintext
GET     /json
[info] ControllerBenchmarkTest:
[info] - test plaintext
[info] - test json
[info] RouteBenchmarkTest:
[info] - routes
[info] RoutingServiceBenchmarkTest:
[info] - routing service
[info] FinagleRequestScopeBenchmarkTest:
[info] - finagle request scope
[info] JsonBenchmarkTest:
[info] - json serde
[info] ScalaTest
[info] Run completed in 6 seconds, 48 milliseconds.
[info] Total number of tests run: 6
[info] Suites: completed 5, aborted 0
[info] Tests: succeeded 6, failed 0, canceled 0, ignored 0, pending 0
[info] All tests passed.
[info] Passed: Total 6, Failed 0, Errors 0, Passed 6
[success] Total time: 22 s, completed May 0, 0000 00:00:00 PM
```

Or you can run the tests manually in an IDE.

## Running

[JMH](http://openjdk.java.net/projects/code-tools/jmh/) is integrated via the 
[`sbt-jmh`](https://github.com/ktoso/sbt-jmh) plugin.

Benchmarks can be run using [SBT](https://www.scala-sbt.org/) via the plugin, e.g., from the
top-level Finatra directory to run all benchmarks:

```
[finatra]$ ./sbt 'project benchmarks' jmh:run
```

or to run a specific benchmark:

```
[finatra]$ ./sbt 'project benchmarks' 'jmh:run JsonBenchmark'
```

which will run the benchmark with the default configuration specified by the class annotations. 

To pass in JMH options:

```
[finatra]$ ./sbt 'project benchmarks' 'jmh:run -i 3 -wi 3 -f1 -t1 JsonBenchmark'
```

### Quickstart on JMH command-line configuration

`-i`  : iterations  
`-wi` : warmup iterations  
`-f`  : forks  
`-t`  : threads 

Note: it is recommended that benchmarks should be usually executed at least in 10 iterations 
(as a rule of thumb), but more is better. For more information see the [`sbt-jmh`](https://github.com/ktoso/sbt-jmh) 
documentation.
