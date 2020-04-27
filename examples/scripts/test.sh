#!/bin/bash
set -euo pipefail

BUILD_SYSTEM=${1-pants}

if [ "$BUILD_SYSTEM" = "pants" ]; then
    command="./pants test --no-test-junit-fast --test-junit-parallel-threads=1 finatra/examples::"
    echo "$command"
    eval "$command"
elif [ "$BUILD_SYSTEM" = "sbt" ]; then
    # make sure to run from /finatra directory
    targets=("benchmark" "javaInjectableApp" "scalaInjectableApp" "javaInjectableTwitterServer" "scalaInjectableTwitterServer" "javaHttpServer" "scalaHttpServer" "javaThriftServer" "scalaThriftServer" "streamingExample" "twitterClone" "exampleWebDashboard")
    for project in "${targets[@]}"; do
        command="./sbt \"project ${project}\" clean +test"
        echo "$command"
        eval "$command"
    done
else
    echo "Unsupported build system: ${BUILD_SYSTEM}."
fi