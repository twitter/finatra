scala_library(
    sources = globs("*.scala"),
    compiler_option_sets = {"fatal_warnings"},
    strict_deps = True,
    dependencies = [
        "3rdparty/jvm/javax/inject:javax.inject",
        "finagle/finagle-core/src/main/scala",
        "finatra/inject/inject-app:test-deps",
        "finatra/inject/inject-core/src/test/scala:test-deps",
        "finatra/thrift/src/main/scala",
        "finatra/thrift/src/test/thrift:doeverything-thrift-java",
        "finatra/thrift/src/test/thrift:doeverything-thrift-scala",
        "util/util-stats/src/main/scala",
    ],
)
