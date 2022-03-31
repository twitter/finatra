scala_library(
    sources = ["*.scala"],
    compiler_option_sets = ["fatal_warnings"],
    provides = scala_artifact(
        org = "com.twitter",
        name = "finatra-http-conversions",
        repo = artifactory,
    ),
    strict_deps = True,
    tags = ["bazel-compatible"],
    dependencies = [
        "finagle/finagle-base-http/src/main",
        "finatra/http-core/src/main/scala/com/twitter/finatra/http/exceptions",
        "finatra/inject/inject-utils/src/main/scala",
        "util/util-core:util-core-util",
        "util/util-slf4j-api/src/main/scala/com/twitter/util/logging",
    ],
    exports = [
        "util/util-core:util-core-util",
        "util/util-slf4j-api/src/main/scala/com/twitter/util/logging",
    ],
)
