target(
    tags = ["bazel-compatible"],
    dependencies = [
        "finatra/examples/advanced/twitter-clone/src/main/scala",
    ],
)

test_suite(
    name = "tests",
    tags = ["bazel-compatible"],
    dependencies = [
        "finatra/examples/advanced/twitter-clone/src/test/scala",
    ],
)

jvm_binary(
    name = "bin",
    basename = "finatra-twitter-clone",
    main = "finatra.quickstart.TwitterCloneServerMain",
    tags = ["bazel-compatible"],
    dependencies = [
        ":twitter-clone",
        "twitter-server/logback-classic/src/main/scala",
    ],
)
