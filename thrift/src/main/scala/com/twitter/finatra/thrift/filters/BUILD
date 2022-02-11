scala_library(
    sources = ["*.scala"],
    compiler_option_sets = ["fatal_warnings"],
    provides = scala_artifact(
        org = "com.twitter",
        name = "finatra-thrift-filters",
        repo = artifactory,
    ),
    strict_deps = True,
    tags = ["bazel-compatible"],
    dependencies = [
        "3rdparty/jvm/javax/inject:javax.inject",
        "finagle/finagle-thrift/src/main/scala",
        "finatra/inject/inject-mdc/src/main/scala",
        "finatra/thrift/src/main/scala/com/twitter/finatra/thrift/exceptions",
        "finatra/thrift/src/main/scala/com/twitter/finatra/thrift/response",
        "util/util-slf4j-api/src/main/scala/com/twitter/util/logging",
    ],
    exports = [
        "util/util-slf4j-api/src/main/scala/com/twitter/util/logging",
    ],
)
