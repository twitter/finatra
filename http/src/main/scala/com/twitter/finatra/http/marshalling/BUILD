scala_library(
    sources = globs("*.scala"),
    compiler_option_sets = {"fatal_warnings"},
    provides = scala_artifact(
        org = "com.twitter",
        name = "finatra-http-marshalling",
        repo = artifactory,
    ),
    strict_deps = "True",
    dependencies = [
        "3rdparty/jvm/com/fasterxml/jackson/core:jackson-databind",
        "3rdparty/jvm/com/google/inject:guice",
        "3rdparty/jvm/javax/inject:javax.inject",
        "3rdparty/jvm/net/codingwell:scala-guice",
        "finagle/finagle-base-http/src/main/scala",
        "finatra/http-annotations/src/main/java/com/twitter/finatra/http/annotations",
        "finatra/inject/inject-core/src/main/scala",
        "finatra/inject/inject-utils/src/main/scala",
        "finatra/jackson/src/main/scala/com/twitter/finatra/jackson",
    ],
    exports = [
        "3rdparty/jvm/com/google/inject:guice",
        "3rdparty/jvm/javax/inject:javax.inject",
        "finagle/finagle-base-http/src/main/scala",
        "finatra/inject/inject-core/src/main/scala",
    ],
)
