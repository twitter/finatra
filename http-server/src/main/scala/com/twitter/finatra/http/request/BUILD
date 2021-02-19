scala_library(
    sources = ["*.scala"],
    compiler_option_sets = ["fatal_warnings"],
    provides = scala_artifact(
        org = "com.twitter",
        name = "finatra-http-request",
        repo = artifactory,
    ),
    strict_deps = "True",
    dependencies = [
        "finagle/finagle-base-http/src/main",
        "finatra/http-core/src/main/scala/com/twitter/finatra/http/fileupload",
        "finatra/http-server/src/main/java/com/twitter/finatra/http/request",
        "finatra/http-server/src/main/scala/com/twitter/finatra/http/exceptions",
        "finatra/inject/inject-slf4j/src/main/scala/com/twitter/inject",
        "finatra/inject/inject-utils/src/main/scala",
    ],
    exports = [
        "finagle/finagle-base-http/src/main",
        "finatra/http-server/src/main/java/com/twitter/finatra/http/request",
        "finatra/inject/inject-slf4j/src/main/scala/com/twitter/inject",
    ],
)
