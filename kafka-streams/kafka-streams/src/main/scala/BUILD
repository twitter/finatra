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
    sources = rglobs(
        "com/twitter/finatra/kafkastreams/*.scala",
        "com/twitter/finatra/streams/*.scala",
        "org/*.scala",
    ),
    compiler_option_sets = {"fatal_warnings"},
    provides = scala_artifact(
        org = "com.twitter",
        name = "finatra-streams",
        repo = artifactory,
    ),
    strict_deps = False,
    dependencies = [
        ":rocksdb-5.14.2",
        "3rdparty/jvm/org/agrona",
        "3rdparty/jvm/org/apache/kafka:kafka-clients",
        "3rdparty/jvm/org/apache/kafka:kafka-streams",
        "3rdparty/jvm/org/apache/kafka:kafka-streams-scala",
        "finatra/inject/inject-core",
        "finatra/inject/inject-server",
        "finatra/inject/inject-slf4j",
        "finatra/inject/inject-utils",
        "finatra/kafka-streams/kafka-streams-queryable-thrift-client/src/main/scala",
        "finatra/kafka-streams/kafka-streams/src/main/java",
        "finatra/kafka/src/main/scala",
    ],
    exports = [
        "3rdparty/jvm/org/apache/kafka:kafka-clients",
        "3rdparty/jvm/org/apache/kafka:kafka-streams",
        "3rdparty/jvm/org/apache/kafka:kafka-streams-scala",
        "finatra/inject/inject-core",
        "finatra/inject/inject-server",
        "finatra/inject/inject-slf4j",
        "finatra/inject/inject-utils",
        "finatra/kafka-streams/kafka-streams/src/main/java",
        "finatra/kafka/src/main/scala",
    ],
)
