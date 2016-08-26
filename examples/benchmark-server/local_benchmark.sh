#!/bin/bash

##
# Run from the top-level finatra project directory:
# e.g.,
# $ cd finatra
# $ ./examples/benchmark-server/local_benchmark.sh

./sbt clean test:compile benchmarkServer/assembly -batch

benchmarks_jar=`find . -type f -name 'benchmark-server-assembly-*.jar'`

java -Dcom.twitter.util.events.sinkEnabled=false -Xmx2000M -Xms2000M -Xmn1750M -XX:MetaspaceSize=128M -XX:ParallelGCThreads=4 -XX:+CMSScavengeBeforeRemark -XX:TargetSurvivorRatio=90 -XX:+UseConcMarkSweepGC -XX:CMSInitiatingOccupancyFraction=70 -XX:+UseCMSInitiatingOccupancyOnly -XX:TargetSurvivorRatio=60 -jar ${benchmarks_jar//.\//} -log.level=ERROR -http.response.charset.enabled=false &

sleep 10

wrk -t10 -c50 -d30s http://127.0.0.1:8888/plaintext
wrk -t10 -c50 -d30s http://127.0.0.1:8888/plaintext
wrk -t10 -c50 -d30s http://127.0.0.1:8888/plaintext
wrk -t10 -c50 -d30s http://127.0.0.1:8888/plaintext
wrk -t10 -c50 -d30s http://127.0.0.1:8888/plaintext
wrk -t10 -c50 -d30s http://127.0.0.1:8888/plaintext
wrk -t10 -c50 -d30s http://127.0.0.1:8888/plaintext
wrk -t10 -c50 -d30s http://127.0.0.1:8888/plaintext
