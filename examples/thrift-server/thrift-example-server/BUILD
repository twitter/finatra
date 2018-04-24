target(
    dependencies = [
        "finatra/examples/thrift-server/thrift-example-server/src/main/scala",
    ],
)

target(
    name = "tests",
    dependencies = [
        "finatra/examples/thrift-server/thrift-example-server/src/test/scala",
    ],
)

jvm_binary(
    name = "bin",
    basename = "thrift-example-server",
    main = "com.twitter.calculator.CalculatorServerMain",
    dependencies = [
        ":thrift-example-server",
        "twitter-server/logback-classic/src/main/scala",
    ],
    deploy_excludes = [
        exclude("org.slf4j", "slf4j-jdk14"),
        exclude("log4j", "log4j"),
    ],
)
