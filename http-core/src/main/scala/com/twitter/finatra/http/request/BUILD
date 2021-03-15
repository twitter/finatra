scala_library(
    compiler_option_sets = ["fatal_warnings"],
    provides = scala_artifact(
        org = "com.twitter",
        name = "finatra-http-core-request",
        repo = artifactory,
    ),
    strict_deps = "True",
    dependencies = [
        "finagle/finagle-base-http/src/main",
        "finagle/finagle-core/src/main",
        "finagle/finagle-http/src/main/scala",
        "finatra/http-core/src/main/java/com/twitter/finatra/http/request",
        "finatra/http-core/src/main/scala/com/twitter/finatra/http/exceptions",
        "finatra/http-core/src/main/scala/com/twitter/finatra/http/fileupload",
        "finatra/inject/inject-slf4j/src/main/scala/com/twitter/inject",
        "finatra/inject/inject-utils/src/main/scala",
        "util/util-core:scala",
    ],
    exports = [
        "finagle/finagle-base-http/src/main",
        "finatra/http-core/src/main/java/com/twitter/finatra/http/request",
        "finatra/inject/inject-slf4j/src/main/scala/com/twitter/inject",
    ],
)
