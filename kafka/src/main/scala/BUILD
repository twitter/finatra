FINATRA_KAFKA_CROSS = [
    {"kafka": ""},
    {"kafka": "2.5"},
]

cross_scala_library(
    name = "scala",
    sources = ["**/*.scala"],
    compiler_option_sets = ["fatal_warnings"],
    cross_build = FINATRA_KAFKA_CROSS,
    provides = scala_artifact(
        org = "com.twitter",
        name = "finatra-kafka{-kafka}",
        repo = artifactory,
    ),
    strict_deps = True,
    dependencies = [
        "3rdparty/jvm/org/apache/kafka:kafka-clients{-kafka}",
        "finagle/finagle-core/src/main",
        "finatra/inject/inject-core/src/main/scala",
        "finatra/inject/inject-slf4j",
        "finatra/inject/inject-slf4j/src/main/scala/com/twitter/inject",
        "finatra/inject/inject-utils",
        "finatra/kafka/src/main/java",
        "finatra/utils/src/main/scala",
        "scrooge/scrooge-serializer/src/main/scala",
        "util/util-codec/src/main/scala/com/twitter/util",
    ],
    exports = [
        "3rdparty/jvm/org/apache/kafka:kafka-clients{-kafka}",
        "finagle/finagle-core/src/main",
        "finatra/inject/inject-core/src/main/scala",
        "finatra/inject/inject-slf4j",
        "finatra/inject/inject-utils",
        "finatra/utils/src/main/scala",
        "scrooge/scrooge-serializer/src/main/scala",
    ],
)
