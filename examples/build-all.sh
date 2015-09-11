#!/bin/bash

set -e

################################################################################
# A script for building all the examples.
################################################################################

# run all the example sbt builds
for directory in $( ls -d */ ); do
  cd $directory && sbt clean test; cd -
done

# also run the example mvn build in hello-world
cd hello-world && mvn clean install; cd -
