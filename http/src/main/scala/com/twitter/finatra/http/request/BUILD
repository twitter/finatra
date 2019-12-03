scala_library(
    sources = globs("*.scala"),
    compiler_option_sets = {"fatal_warnings"},
    provides = scala_artifact(
        org = "com.twitter",
        name = "finatra-http-request",
        repo = artifactory,
    ),
    strict_deps = "True",
    dependencies = [
        "finagle/finagle-base-http/src/main/scala",
        "finatra/http/src/main/java/com/twitter/finatra/http/request",
        "finatra/http/src/main/scala/com/twitter/finatra/http/exceptions",
        "finatra/http/src/main/scala/com/twitter/finatra/http/fileupload",
        "finatra/inject/inject-slf4j/src/main/scala",
        "finatra/inject/inject-utils/src/main/scala",
    ],
    exports = [
        "finagle/finagle-base-http/src/main/scala",
        "finatra/http/src/main/java/com/twitter/finatra/http/request",
        "finatra/inject/inject-slf4j/src/main/scala",
    ],
)
