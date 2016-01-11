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

javaVersion=`java -version 2>&1 | grep "java version" | awk '{print $3}' | tr -d \"`

MAX_PERM_SIZE_JVM_ARG=""
ELIMINATE_AUTOBOX_JVM_ARG=""
if [[ $javaVersion == *"1.8"* ]]; then
  # Workaround for JDK issue: https://bugs.openjdk.java.net/browse/JDK-8058847
  # only add the option on version JDK8
  ELIMINATE_AUTOBOX_JVM_ARG="-XX:-EliminateAutoBox"
else
  MAX_PERM_SIZE_JVM_ARG="-XX:MaxPermSize=1024M"
fi

[ -f ~/.sbtconfig ] && . ~/.sbtconfig

CMD="java -ea                     \
  $SBT_OPTS                       \
  $JAVA_OPTS                      \
  -XX:+AggressiveOpts             \
  -XX:+UseParNewGC                \
  -XX:+UseConcMarkSweepGC         \
  -XX:+CMSParallelRemarkEnabled   \
  -XX:+CMSClassUnloadingEnabled   \
  -XX:ReservedCodeCacheSize=128m  \
  ${MAX_PERM_SIZE_JVM_ARG}        \
  -XX:SurvivorRatio=128           \
  -XX:MaxTenuringThreshold=0      \
  ${ELIMINATE_AUTOBOX_JVM_ARG}    \
  -Xms512M                        \
  -Xmx768M                        \
  -server                         \
  -jar $sbtjar ${@:1}"

echo ${CMD}
eval $CMD
