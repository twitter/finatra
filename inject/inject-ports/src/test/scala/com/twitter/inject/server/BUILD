junit_tests(
    sources = globs("*.scala"),
    compiler_option_sets = {"fatal_warnings"},
    dependencies = [
        "finagle/finagle-core/src/main/scala",
        "finatra/inject/inject-core/src/test/scala:test-deps",
        "finatra/inject/inject-ports/src/main/scala/com/twitter/inject/server",
        "twitter-server/server/src/main/scala",
        "util/util-app/src/main/scala",
    ],
)
