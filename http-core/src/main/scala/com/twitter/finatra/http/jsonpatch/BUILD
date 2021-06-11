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
        "finagle/finagle-base-http/src/main",
        "finatra/http-core/src/main/java/com/twitter/finatra/http/jsonpatch",
        "finatra/http-core/src/main/scala/com/twitter/finatra/http/exceptions",
        "finatra/http-core/src/main/scala/com/twitter/finatra/http/marshalling",
        "finatra/http-core/src/main/scala/com/twitter/finatra/http/response",
        "util/util-jackson/src/main/scala/com/twitter/util/jackson",
    ],
    exports = [
        "3rdparty/jvm/com/fasterxml/jackson/core:jackson-core",
        "3rdparty/jvm/com/fasterxml/jackson/core:jackson-databind",
        "finatra/http-core/src/main/java/com/twitter/finatra/http/jsonpatch",
        "finatra/http-core/src/main/scala/com/twitter/finatra/http/response",
        "util/util-jackson/src/main/scala/com/twitter/util/jackson",
    ],
)
