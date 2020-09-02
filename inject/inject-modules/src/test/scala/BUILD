junit_tests(
    sources = ["com/twitter/inject/modules/tests/*.scala"],
    compiler_option_sets = ["fatal_warnings"],
    dependencies = [
        ":test-deps",
        "3rdparty/jvm/com/google/inject:guice",
        "3rdparty/jvm/com/google/inject/extensions:guice-assistedinject",
        "3rdparty/jvm/com/google/inject/extensions:guice-testlib",
        "3rdparty/jvm/junit",
        "3rdparty/jvm/net/codingwell:scala-guice",
        "3rdparty/jvm/org/scalatest",
        "3rdparty/jvm/org/scalatestplus:junit",
        "3rdparty/jvm/org/slf4j:slf4j-api",
        scoped(
            "3rdparty/jvm/org/slf4j:slf4j-simple",
            scope = "runtime",
        ),
        "finatra/inject/inject-core/src/main/scala/com/twitter/inject",
        "finatra/inject/inject-core/src/test/scala/com/twitter/inject",
        "finatra/inject/inject-modules/src/main/scala",
        "util/util-slf4j-api/src/main/scala",
        "util/util-stats/src/main/scala",
    ],
)

scala_library(
    name = "test-deps",
    sources = ["com/twitter/inject/modules/*.scala"],
    compiler_option_sets = ["fatal_warnings"],
    provides = scala_artifact(
        org = "com.twitter",
        name = "inject-modules-tests",
        repo = artifactory,
    ),
    dependencies = [
        "3rdparty/jvm/com/google/inject:guice",
        "3rdparty/jvm/com/google/inject/extensions:guice-assistedinject",
        "3rdparty/jvm/net/codingwell:scala-guice",
        "3rdparty/jvm/org/slf4j:slf4j-api",
        "finatra/inject/inject-core/src/main/scala",
        "util/util-slf4j-api/src/main/scala",
        "util/util-stats/src/main/scala",
    ],
)
