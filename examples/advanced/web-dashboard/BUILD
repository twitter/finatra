target(
    dependencies = [
        "finatra/examples/advanced/web-dashboard/src/main/scala",
    ],
)

jvm_binary(
    name = "bin",
    basename = "web-dashboard",
    main = "com.twitter.web.dashboard.ServerMain",
    dependencies = [
        ":web-dashboard",
        "twitter-server/logback-classic/src/main/scala",
    ],
)
