scala_library(
    sources = ["*.scala"],
    compiler_option_sets = ["fatal_warnings"],
    provides = scala_artifact(
        org = "com.twitter",
        name = "finatra-http-mustache-marshalling",
        repo = artifactory,
    ),
    strict_deps = True,
    dependencies = [
        "3rdparty/jvm/javax/inject:javax.inject",
        "finagle/finagle-base-http/src/main",
        "finatra/http-core/src/main/scala/com/twitter/finatra/http/marshalling",
        "finatra/http-mustache/src/main/java/com/twitter/finatra/http/annotations",
        "finatra/http-server/src/main/scala/com/twitter/finatra/http/response",
        "finatra/inject/inject-utils/src/main/scala",
        "finatra/mustache/src/main/scala/com/twitter/finatra/mustache/marshalling",
    ],
    exports = [
        "3rdparty/jvm/javax/inject:javax.inject",
        "finagle/finagle-base-http/src/main",
        "finatra/http-core/src/main/scala/com/twitter/finatra/http/marshalling",
        "finatra/mustache/src/main/scala/com/twitter/finatra/mustache/marshalling",
    ],
)
