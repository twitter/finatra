junit_tests(
    sources = globs(
        "com/twitter/inject/tests/exceptions/*.scala",
        "com/twitter/inject/tests/thrift/utils/*.scala",
    ),
    compiler_option_sets = {"fatal_warnings"},
    dependencies = [
        "3rdparty/jvm/org/scalatest",
        "3rdparty/jvm/org/slf4j:slf4j-api",
        "3rdparty/jvm/org/slf4j:slf4j-simple",
        "finagle/finagle-core/src/main/scala",
        "finagle/finagle-mux/src/main/scala",
        "finatra/inject/inject-core/src/main/scala",
        "finatra/inject/inject-core/src/test/scala:test-deps",
        "finatra/inject/inject-thrift/src/main/scala",
        "finatra/inject/inject-utils/src/main/scala",
        "scrooge/scrooge-core/src/main/scala",
        "util/util-core/src/main/scala",
        "util/util-slf4j-api/src/main/scala",
    ],
)
