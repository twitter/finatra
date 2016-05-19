#!/bin/bash

set -e

################################################################################
# A script for building all of the example projects.
################################################################################

javaVersion=`java -version 2>&1 | grep "java version" | awk '{print $3}' | tr -d \"`
echo -e "[info] Detected java version: $javaVersion"

# run all the example sbt builds
for directory in $( ls -d */ ); do
  echo -e "[info] Building project: $directory"
    cd $directory && sbt clean test
    if [[ $directory == "hello-world/" ]]; then
      # also run the example mvn build in hello-world
      mvn clean install
    fi
    cd -
done
