scala_library(
    sources = ["*.scala"],
    compiler_option_sets = ["fatal_warnings"],
    provides = scala_artifact(
        org = "com.twitter",
        name = "finatra-jackson-streaming",
        repo = artifactory,
    ),
    strict_deps = True,
    tags = ["bazel-compatible"],
    dependencies = [
        "3rdparty/jvm/com/fasterxml/jackson/core:jackson-core",
        "3rdparty/jvm/javax/inject:javax.inject",
        "finatra/inject/inject-slf4j/src/main/scala/com/twitter/inject",
        "finatra/inject/inject-utils/src/main/scala",
        "util/util-core/src/main/scala/com/twitter/concurrent",
        "util/util-core/src/main/scala/com/twitter/io",
        "util/util-jackson/src/main/scala/com/twitter/util/jackson",
    ],
)
