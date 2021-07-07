junit_tests(
    sources = ["com/twitter/inject/tests/**/*.scala"],
    compiler_option_sets = ["fatal_warnings"],
    tags = ["bazel-compatible"],
    dependencies = [
        "3rdparty/jvm/joda-time",
        "3rdparty/jvm/org/scalatest",
        "3rdparty/jvm/org/scalatestplus:junit",
        "3rdparty/jvm/org/slf4j:slf4j-api",
        scoped(
            "3rdparty/jvm/org/slf4j:slf4j-simple",
            scope = "runtime",
        ),
        "finagle/finagle-core/src/main",
        "finatra/inject/inject-core/src/main/scala/com/twitter/inject",
        "finatra/inject/inject-core/src/test/scala/com/twitter/inject",
        "finatra/inject/inject-utils/src/main/scala",
        "finatra/inject/inject-utils/src/test/java/com/twitter/inject/annotations",
        "finatra/inject/inject-utils/src/test/resources",
        "util/util-core:scala",
        "util/util-slf4j-api/src/main/scala",
    ],
)
