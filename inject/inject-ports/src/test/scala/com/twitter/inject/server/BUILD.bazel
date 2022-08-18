junit_tests(
    sources = ["*.scala"],
    compiler_option_sets = ["fatal_warnings"],
    platform = "java8",
    tags = ["bazel-compatible"],
    dependencies = [
        "3rdparty/jvm/org/scalatestplus:junit",
        "finagle/finagle-core/src/main",
        "finatra/inject/inject-core/src/test/scala/com/twitter/inject",
        "finatra/inject/inject-ports/src/main/scala/com/twitter/inject/server",
        "twitter-server/server/src/main/scala",
        "util/util-app/src/main/scala",
    ],
)
