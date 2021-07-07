junit_tests(
    sources = [
        "com/twitter/inject/tests/exceptions/*.scala",
        "com/twitter/inject/tests/thrift/utils/*.scala",
    ],
    compiler_option_sets = ["fatal_warnings"],
    tags = ["bazel-compatible"],
    dependencies = [
        "3rdparty/jvm/org/scalatest",
        "3rdparty/jvm/org/scalatestplus:junit",
        "3rdparty/jvm/org/slf4j:slf4j-api",
        scoped(
            "3rdparty/jvm/org/slf4j:slf4j-simple",
            scope = "runtime",
        ),
        "finagle/finagle-core/src/main",
        "finagle/finagle-mux/src/main/scala",
        "finatra/inject/inject-core/src/main/scala/com/twitter/inject",
        "finatra/inject/inject-core/src/test/scala/com/twitter/inject",
        "finatra/inject/inject-thrift/src/main/scala",
        "finatra/inject/inject-utils/src/main/scala",
        "scrooge/scrooge-core/src/main/scala",
        "util/util-core:scala",
        "util/util-mock/src/main/scala/com/twitter/util/mock",
        "util/util-slf4j-api/src/main/scala",
    ],
)
