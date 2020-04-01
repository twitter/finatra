scala_library(
    sources = ["*.scala"],
    compiler_option_sets = ["fatal_warnings"],
    provides = scala_artifact(
        org = "com.twitter",
        name = "finatra-http-streaming",
        repo = artifactory,
    ),
    strict_deps = "True",
    dependencies = [
        "finagle/finagle-base-http/src/main/scala",
        "finatra/jackson/src/main/scala/com/twitter/finatra/jackson",
        "finatra/jackson/src/main/scala/com/twitter/finatra/jackson/streaming",
        "util/util-core/src/main/scala/com/twitter/concurrent",
        "util/util-core/src/main/scala/com/twitter/io",
        "util/util-core/src/main/scala/com/twitter/util",
    ],
    exports = [
        "finagle/finagle-base-http/src/main/scala",
        "util/util-core/src/main/scala/com/twitter/concurrent",
        "util/util-core/src/main/scala/com/twitter/io",
        "util/util-core/src/main/scala/com/twitter/util",
    ],
)
