#!/bin/bash
# 5 measurement/warmup iterations single fork
java -jar target/benchmarks.jar ".*" -wi 20 -i 10 -f 1
