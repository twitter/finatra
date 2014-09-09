#!/bin/bash
# 5 measurement/warmup iterations single fork
java -jar target/benchmarks.jar ".*" -wi 5 -i 5 -f 1
