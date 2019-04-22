target(
    dependencies = [
        "finatra/examples/http-server/src/main/scala",
    ],
)

target(
    name = "tests",
    dependencies = [
        "finatra/examples/http-server/src/test/scala",
    ],
)

jvm_binary(
    name = "bin",
    basename = "finatra-http-server",
    main = "com.twitter.hello.HelloWorldServerMain",
    dependencies = [
        ":http-server",
        "twitter-server/logback-classic/src/main/scala",
    ],
    excludes = [
        exclude("org.slf4j", "slf4j-jdk14"),
        exclude("log4j", "log4j"),
    ],
)
