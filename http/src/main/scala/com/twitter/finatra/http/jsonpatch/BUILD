scala_library(
    sources = ["*.scala"],
    compiler_option_sets = ["fatal_warnings"],
    provides = scala_artifact(
        org = "com.twitter",
        name = "finatra-http-jsonpatch",
        repo = artifactory,
    ),
    strict_deps = "True",
    dependencies = [
        "3rdparty/jvm/com/fasterxml/jackson/core:jackson-core",
        "3rdparty/jvm/com/fasterxml/jackson/core:jackson-databind",
        "3rdparty/jvm/javax/inject:javax.inject",
        "finagle/finagle-base-http/src/main/scala",
        "finatra/http/src/main/java/com/twitter/finatra/http/jsonpatch",
        "finatra/http/src/main/scala/com/twitter/finatra/http/exceptions",
        "finatra/http/src/main/scala/com/twitter/finatra/http/marshalling",
        "finatra/http/src/main/scala/com/twitter/finatra/http/response",
        "finatra/jackson/src/main/scala",
    ],
    exports = [
        "3rdparty/jvm/com/fasterxml/jackson/core:jackson-core",
        "3rdparty/jvm/com/fasterxml/jackson/core:jackson-databind",
        "finatra/http/src/main/java/com/twitter/finatra/http/jsonpatch",
        "finatra/http/src/main/scala/com/twitter/finatra/http/response",
        "finatra/jackson/src/main/scala",
    ],
)
