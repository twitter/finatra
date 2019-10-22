scala_library(
    sources = globs("*.scala"),
    compiler_option_sets = {"fatal_warnings"},
    provides = scala_artifact(
        org = "com.twitter",
        name = "finatra-http-conversions",
        repo = artifactory,
    ),
    dependencies = [
        "finagle/finagle-base-http/src/main/scala",
        "finatra/http/src/main/scala/com/twitter/finatra/http/exceptions",
        "finatra/inject/inject-slf4j/src/main/scala",
        "finatra/inject/inject-utils/src/main/scala",
        "util/util-core/src/main/scala/com/twitter/util",
    ],
    exports = [
        "util/util-core/src/main/scala/com/twitter/util",
    ],
)
