target(
    dependencies = [
        "finatra/examples/hello-world/src/main/scala",
    ],
)

target(
    name = "tests",
    dependencies = [
        "finatra/examples/hello-world/src/test/scala",
    ],
)

jvm_binary(
    name = "bin",
    basename = "finatra-hello-world",
    main = "com.twitter.hello.HelloWorldServerMain",
    dependencies = [
        ":hello-world",
        "twitter-server/logback-classic/src/main/scala",
    ],
    excludes = [
        exclude("org.slf4j", "slf4j-jdk14"),
        exclude("log4j", "log4j"),
    ],
)
