target(
    dependencies = [
        "finatra/examples/advanced/streaming-example/src/main/scala",
    ],
)

target(
    name = "tests",
    dependencies = [
        "finatra/examples/advanced/streaming-example/src/test/scala",
    ],
)

jvm_binary(
    name = "bin",
    basename = "streaming-example",
    main = "com.twitter.streaming.StreamingServerMain",
    dependencies = [
        ":streaming-example",
        "twitter-server/logback-classic/src/main/scala",
    ],
)
