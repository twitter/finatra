jvm_binary(
    name = "bin",
    basename = "example-twitter-server",
    main = "com.twitter.finatra.sample.ExampleTwitterServerMain",
    dependencies = [
        "finatra/examples/example-twitter-server/src/main/scala",
        "twitter-server/logback-classic/src/main/scala",
        "twitter-server/server/src/main/scala",
    ],
    excludes = [
        exclude("org.slf4j", "slf4j-jdk14"),
        exclude("log4j", "log4j"),
    ],
)
