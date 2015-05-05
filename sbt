#!/bin/bash

sbtver=0.13.8
sbtjar=sbt-launch.jar
sbtsha128=57d0f04f4b48b11ef7e764f4cea58dee4e806ffd
sbtrepo="http://repo.typesafe.com/typesafe/ivy-releases/org.scala-sbt/sbt-launch/$sbtver/$sbtjar"

if [ ! -f "./$sbtjar" ]; then
  echo "downloading $sbtjar from $sbtrepo" 1>&2
  if ! curl --location --silent --fail --remote-name $sbtrepo > $sbtjar; then
    exit 1
  fi
  checksum=`openssl dgst -sha1 $sbtjar | awk '{ print $2 }'`
  if [ "$checksum" != $sbtsha128 ]; then
    echo "[error] Bad $sbtjar. Delete $sbtjar and run $0 again."
    exit 1
  fi
else
  echo "[info] Skipping download of sbt-launch.jar."
fi

[ -f ~/.sbtconfig ] && . ~/.sbtconfig

java -ea                          \
  $SBT_OPTS                       \
  $JAVA_OPTS                      \
  -Djava.net.preferIPv4Stack=true \
  -XX:+AggressiveOpts             \
  -XX:+UseParNewGC                \
  -XX:+UseConcMarkSweepGC         \
  -XX:+CMSParallelRemarkEnabled   \
  -XX:+CMSClassUnloadingEnabled   \
  -XX:ReservedCodeCacheSize=128m  \
  -XX:MaxPermSize=1024m           \
  -XX:SurvivorRatio=128           \
  -XX:MaxTenuringThreshold=0      \
  -Xss8M                          \
  -Xms512M                        \
  -Xmx2G                          \
  -server                         \
  -jar $sbtjar "${@:1}"
