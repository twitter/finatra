scala_library(
    sources = globs("*.scala"),
    compiler_option_sets = {"fatal_warnings"},
    provides = scala_artifact(
        org = "com.twitter",
        name = "finatra-http-exceptions-json-internal",
        repo = artifactory,
    ),
    strict_deps = "True",
    dependencies = [
        "3rdparty/jvm/com/fasterxml/jackson/core:jackson-core",
        "3rdparty/jvm/javax/inject:javax.inject",
        "finagle/finagle-base-http/src/main/scala",
        "finatra/http/src/main/scala/com/twitter/finatra/http/exceptions",
        "finatra/http/src/main/scala/com/twitter/finatra/http/response",
        "finatra/inject/inject-slf4j/src/main/scala",
        "finatra/jackson/src/main/scala",
    ],
    exports = [
        "3rdparty/jvm/com/fasterxml/jackson/core:jackson-core",
        "3rdparty/jvm/javax/inject:javax.inject",
        "finagle/finagle-base-http/src/main/scala",
        "finatra/http/src/main/scala/com/twitter/finatra/http/exceptions",
        "finatra/http/src/main/scala/com/twitter/finatra/http/response",
        "finatra/inject/inject-slf4j/src/main/scala",
        "finatra/jackson/src/main/scala",
    ],
)
