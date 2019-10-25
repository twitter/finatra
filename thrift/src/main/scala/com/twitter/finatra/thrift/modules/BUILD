scala_library(
    sources = globs("*.scala"),
    compiler_option_sets = {"fatal_warnings"},
    provides = scala_artifact(
        org = "com.twitter",
        name = "finatra-thrift",
        repo = artifactory,
    ),
    strict_deps = True,
    dependencies = [
        "finagle/finagle-core/src/main/scala",
        "finagle/finagle-exp/src/main/scala",
        "finagle/finagle-thrift/src/main/scala",
        "finagle/finagle-thriftmux/src/main/scala",
        "finatra/inject/inject-core/src/main/scala",
        "finatra/thrift/src/main/scala/com/twitter/finatra/thrift/exceptions",
        "finatra/thrift/src/main/scala/com/twitter/finatra/thrift/filters",
        "finatra/thrift/src/main/scala/com/twitter/finatra/thrift/internal/exceptions",
        "finatra/thrift/src/main/scala/com/twitter/finatra/thrift/response",
        "finatra/utils/src/main/java/com/twitter/finatra/annotations",
    ],
)
