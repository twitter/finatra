# Finatra Benchmark Server

A simple server for running microbenchmarks using [JMH](http://openjdk.java.net/projects/code-tools/jmh/).

Note: All Finatra examples should be run from the base Finatra directory as they are defined as part 
of the root project.

Building
--------

For any branch that is not [Master](https://github.com/twitter/finatra/tree/master) or a tagged 
[release branch](https://github.com/twitter/finatra/releases) (or a branch based on one of those 
branches), see the [CONTRIBUTING.md](../../CONTRIBUTING.md#building-dependencies) documentation on 
building Finatra and it's dependencies locally in order to run the examples.

Running
-------
```
[finatra] $ ./sbt benchmarkServer/run
```
* Then browse to: [http://localhost:8888/json](http://localhost:8888/json)
* Or view the [twitter-server admin interface](https://twitter.github.io/twitter-server/Features.html#admin-http-interface): [http://localhost:9990/admin](http://localhost:9990/admin)
* Or build and run a deployable jar:
```
[finatra] $ ./sbt benchmarkServer/assembly
[finatra] $ java -jar examples/benchmark-server/target/scala-2.XX/benchmark-server-assembly-X.XX.X.jar -http.port=:8888 -admin.port=:9990
```

Run Local Benchmark
-------------------

To run an example benchmarking, you can run the [local_benchmark.sh](./local_benchmark.sh) 
script. Note, results will vary based on your machine performance. Again, from the base 
Finatra directory:

```
[finatra] $ ./examples/benchmark-server/local_benchmark.sh
...
Running 30s test @ http://127.0.0.1:8888/plaintext
  10 threads and 50 connections
  ...
```
