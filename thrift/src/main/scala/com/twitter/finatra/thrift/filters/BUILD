scala_library(
    sources = globs("*.scala"),
    compiler_option_sets = {"fatal_warnings"},
    provides = scala_artifact(
        org = "com.twitter",
        name = "finatra-thrift-filters",
        repo = artifactory,
    ),
    strict_deps = True,
    dependencies = [
        "3rdparty/jvm/javax/inject:javax.inject",
        "finagle/finagle-exp/src/main/scala",
        "finagle/finagle-thrift/src/main/scala",
        "finatra/inject/inject-slf4j/src/main/scala",
        "finatra/thrift/src/main/scala/com/twitter/finatra/thrift/exceptions",
        "finatra/thrift/src/main/scala/com/twitter/finatra/thrift/response",
    ],
)
