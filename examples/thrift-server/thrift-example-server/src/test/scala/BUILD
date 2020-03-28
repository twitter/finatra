junit_tests(
    sources = ["**/*.scala"],
    compiler_option_sets = {"fatal_warnings"},
    dependencies = [
        "3rdparty/jvm/com/google/inject:guice",
        "3rdparty/jvm/com/google/inject/extensions:guice-assistedinject",
        "3rdparty/jvm/com/google/inject/extensions:guice-testlib",
        "3rdparty/jvm/net/codingwell:scala-guice",
        "3rdparty/jvm/org/scalatest",
        "3rdparty/jvm/org/slf4j:slf4j-simple",
        "finatra/examples/thrift-server/thrift-example-idl/src/main/thrift:thrift-scala",
        "finatra/examples/thrift-server/thrift-example-server/src/main/scala",
        "finatra/inject/inject-server/src/main/scala",
        "finatra/inject/inject-server/src/test/scala:test-deps",
        "finatra/thrift/src/test/scala:test-deps",
    ],
)
