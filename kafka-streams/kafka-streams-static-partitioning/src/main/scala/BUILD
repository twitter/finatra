scala_library(
    sources = [
        "com/**/*.scala",
        # The below entries are trying to achieve the same effect in Pants and Bazel.
        # This is taken in Pants; entry starting with ".." will be ignored by Bazel
        "../scala-kafka2.5/**/*.scala",
        # This is a bazel filegroup target, however, Pants will see this as a path to match files,
        # which would return nothing.
        "finatra/kafka-streams/kafka-streams-static-partitioning/src/main/scala-kafka2.5:kafka-2.5",
    ],
    compiler_option_sets = ["fatal_warnings"],
    provides = scala_artifact(
        org = "com.twitter",
        name = "finatra-streams-static-partitioning",
        repo = artifactory,
    ),
    strict_deps = True,
    dependencies = [
        "finatra/kafka-streams/kafka-streams-queryable-thrift-client/src/main/scala",
        "finatra/kafka-streams/kafka-streams-static-partitioning/src/main/java",
        "finatra/kafka-streams/kafka-streams/src/main/scala",
    ],
    exports = [
        "finatra/kafka-streams/kafka-streams/src/main/scala",
    ],
)
