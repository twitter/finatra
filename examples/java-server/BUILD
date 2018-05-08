target(
    dependencies = [
        "finatra/examples/java-server/src/main/java",
    ],
)

target(
    name = "tests",
    dependencies = [
        "finatra/examples/java-server/src/test/java",
    ],
)

jvm_binary(
    name = "bin",
    basename = "java-server",
    main = "com.twitter.hello.HelloWorldServerMain",
    dependencies = [
        ":java-server",
        "twitter-server/logback-classic/src/main/scala",
    ],
    deploy_excludes = [
        exclude("org.slf4j", "slf4j-jdk14"),
        exclude("log4j", "log4j"),
    ],
)
