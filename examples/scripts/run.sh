#!/bin/bash
set -euo pipefail

# Script to help run examples via Pants (https://pantsbuild.org)
# usage:
#   ./finatra/examples/scripts/run.sh injectable-app java -username=Bob
#   ./finatra/examples/scripts/run.sh injectable-app scala -username=Bob
#   ./finatra/examples/scripts/run.sh injectable-twitter-server java -admin.port=:9999
#   ./finatra/examples/scripts/run.sh injectable-twitter-server scala -admin.port=:9999
#   ./finatra/examples/scripts/run.sh http java -admin.port=:9999 -http.port=:8000
#   ./finatra/examples/scripts/run.sh http scala -admin.port=:9999 -http.port=:8000
#   ./finatra/examples/scripts/run.sh thrift java -admin.port=:9999 -thrift.port=:8000
#   ./finatra/examples/scripts/run.sh thrift scala -admin.port=:9999 -thrift.port=:8000
TYPE="$1"
LANG="$2"
ARGS=("${@:3}")

if [ ${#ARGS[@]} -ne 0 ]; then
  command="./pants -q run finatra/examples/$TYPE/$LANG/src/main/$LANG/com/twitter/finatra/example:bin -- ${ARGS[*]}"
  echo "$command"
  eval "$command"
else
  command="./pants -q run finatra/examples/$TYPE/$LANG/src/main/$LANG/com/twitter/finatra/example:bin"
  echo "$command"
  eval "$command"
fi


