scala_library(
    compiler_option_sets = ["fatal_warnings"],
    provides = scala_artifact(
        org = "com.twitter",
        name = "finatra-http-core-request",
        repo = artifactory,
    ),
    strict_deps = True,
    tags = ["bazel-compatible"],
    dependencies = [
        "finagle/finagle-base-http/src/main",
        "finagle/finagle-core/src/main",
        "finagle/finagle-http/src/main/scala",
        "finatra/http-core/src/main/java/com/twitter/finatra/http/request",
        "finatra/http-core/src/main/scala/com/twitter/finatra/http/exceptions",
        "finatra/http-core/src/main/scala/com/twitter/finatra/http/fileupload",
        "finatra/inject/inject-utils/src/main/scala",
        "util/util-core:scala",
        "util/util-slf4j-api/src/main/scala/com/twitter/util/logging",
    ],
    exports = [
        "finagle/finagle-base-http/src/main",
        "finatra/http-core/src/main/java/com/twitter/finatra/http/request",
        "util/util-slf4j-api/src/main/scala/com/twitter/util/logging",
    ],
)
