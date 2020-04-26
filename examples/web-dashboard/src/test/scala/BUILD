junit_tests(
    sources = ["**/*.scala"],
    compiler_option_sets = ["fatal_warnings"],
    dependencies = [
        "3rdparty/jvm/com/google/inject:guice",
        "3rdparty/jvm/com/google/inject/extensions:guice-assistedinject",
        "3rdparty/jvm/com/google/inject/extensions:guice-testlib",
        "3rdparty/jvm/net/codingwell:scala-guice",
        "3rdparty/jvm/org/mockito:mockito-all",
        "3rdparty/jvm/org/scalatest",
        "3rdparty/jvm/org/slf4j:slf4j-simple",
        "3rdparty/jvm/org/specs2:mock",
        "finatra/examples/web-dashboard/src/main/scala",
        "finatra/http/src/test/scala/com/twitter/finatra/http",
        "finatra/inject/inject-server/src/main/scala",
        "finatra/inject/inject-server/src/test/scala:test-deps",
    ],
    excludes = [
        exclude(
            org = "org.slf4j",
            name = "slf4j-jdk14",
        ),
    ],
)
