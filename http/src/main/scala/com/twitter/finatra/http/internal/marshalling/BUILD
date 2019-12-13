scala_library(
    sources = globs("*.scala"),
    compiler_option_sets = {"fatal_warnings"},
    provides = scala_artifact(
        org = "com.twitter",
        name = "finatra-http-marshalling-internal",
        repo = artifactory,
    ),
    strict_deps = "True",
    dependencies = [
        "3rdparty/jvm/com/fasterxml/jackson/core:jackson-databind",
        "3rdparty/jvm/com/google/inject:guice",
        "3rdparty/jvm/javax/inject:javax.inject",
        "finagle/finagle-base-http/src/main/scala",
        "finatra/http/src/main/scala/com/twitter/finatra/http/marshalling",
        "finatra/inject/inject-app/src/main/java/com/twitter/inject/annotations",
        "finatra/jackson/src/main/scala",
        "finatra/utils/src/main/scala/com/twitter/finatra/utils",
    ],
    exports = [
        "3rdparty/jvm/com/fasterxml/jackson/core:jackson-databind",
        "3rdparty/jvm/com/google/inject:guice",
        "3rdparty/jvm/javax/inject:javax.inject",
        "finagle/finagle-base-http/src/main/scala",
        "finatra/http/src/main/scala/com/twitter/finatra/http/marshalling",
        "finatra/jackson/src/main/scala",
    ],
)
