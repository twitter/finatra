scala_library(
    sources = ["*.scala"],
    compiler_option_sets = ["fatal_warnings"],
    platform = "java8",
    provides = scala_artifact(
        org = "com.twitter",
        name = "finatra-http-mustache-marshalling",
        repo = artifactory,
    ),
    strict_deps = True,
    tags = ["bazel-compatible"],
    dependencies = [
        "3rdparty/jvm/javax/inject:javax.inject",
        "finagle/finagle-base-http/src/main",
        "finatra/http-core/src/main/scala/com/twitter/finatra/http/marshalling",
        "finatra/http-core/src/main/scala/com/twitter/finatra/http/response",
        "finatra/http-mustache/src/main/java/com/twitter/finatra/http/annotations",
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
