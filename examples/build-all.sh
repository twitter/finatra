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
  if [[ $javaVersion != *"1.8"* ]] && [[ $directory == *"java-http-server"* ]]; then
	# skip building as this example requires JDK 8
	echo -e "[warn] Skipping $directory because java 1.8 is required to build but detected java $javaVersion."
  else
  	cd $directory && sbt clean test
  	if [[ $directory == "hello-world/" ]]; then
	  # also run the example mvn build in hello-world
  	  mvn clean install
  	fi
  	cd -
  fi	
done