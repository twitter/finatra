# Upgrade version of rocksdb
jar_library(
    name = "rocksdb-5.14.2",
    jars = [
        jar(
            org = "org.rocksdb",
            name = "rocksdbjni",
            force = True,
            rev = "5.14.2",
        ),
    ],
)

scala_library(
    sources = [
        "com/twitter/finatra/kafkastreams/**/*.scala",
        "org/apache/kafka/streams/state/**/*.scala",

        # The below entries are trying to achieve the same effect in Pants and Bazel.
        # This is taken in Pants; entry starting with ".." will be ignored by Bazel
        "../scala-kafka2.2/*.scala",
        # This is a bazel filegroup target, however, Pants will see this as a path to match files,
        # which would return nothing.
        "finatra/kafka-streams/kafka-streams/src/main/scala-kafka2.2:kafka-2.2",
    ],
    compiler_option_sets = ["fatal_warnings"],
    provides = scala_artifact(
        org = "com.twitter",
        name = "finatra-streams",
        repo = artifactory,
    ),
    strict_deps = True,
    dependencies = [
        # NOTE: remove this direct rocksDB dependency when building 2.5
        ":rocksdb-5.14.2",
        "3rdparty/jvm/it/unimi/dsi:fastutil",
        "3rdparty/jvm/org/agrona",
        "3rdparty/jvm/org/apache/kafka:kafka-clients",
        "3rdparty/jvm/org/apache/kafka:kafka-streams",
        "3rdparty/jvm/org/apache/kafka:kafka-streams-scala",
        "finatra/inject/inject-core/src/main/scala",
        "finatra/inject/inject-server/src/main/scala/com/twitter/inject/server",
        "finatra/inject/inject-slf4j",
        "finatra/kafka-streams/kafka-streams-queryable-thrift-client/src/main/scala",
        "finatra/kafka-streams/kafka-streams/src/main/java",
        "finatra/kafka/src/main/java",
        "finatra/kafka/src/main/scala",
        "util/util-jvm/src/main/scala/com/twitter/jvm",
    ],
    exports = [
        "3rdparty/jvm/org/apache/kafka:kafka-clients",
        "3rdparty/jvm/org/apache/kafka:kafka-streams",
        "3rdparty/jvm/org/apache/kafka:kafka-streams-scala",
        "finatra/inject/inject-core/src/main/scala",
        "finatra/inject/inject-server/src/main/scala/com/twitter/inject/server",
        "finatra/inject/inject-slf4j",
        "finatra/kafka-streams/kafka-streams/src/main/java",
        "finatra/kafka/src/main/scala",
    ],
)
