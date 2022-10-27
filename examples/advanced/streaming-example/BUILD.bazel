test_suite(
    name = "tests",
    tags = ["bazel-compatible"],
    dependencies = [
        "finatra/examples/advanced/streaming-example/src/test/scala",
    ],
)

jvm_binary(
    name = "bin",
    basename = "streaming-example",
    main = "com.twitter.streaming.StreamingServerMain",
    platform = "java8",
    tags = ["bazel-compatible"],
    dependencies = [
        "finatra/examples/advanced/streaming-example/src/main/scala",
        "twitter-server/logback-classic/src/main/scala",
    ],
)
