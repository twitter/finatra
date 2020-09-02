junit_tests(
    sources = ["**/*.java"],
    dependencies = [
        "3rdparty/jvm/com/github/nscala_time",
        "3rdparty/jvm/joda-time",
        "3rdparty/jvm/org/scalatest",
        "3rdparty/jvm/org/scalatestplus:junit",
        scoped(
            "3rdparty/jvm/org/slf4j:slf4j-nop",
            scope = "runtime",
        ),
        "finatra/utils/src/main/scala/com/twitter/finatra/utils",
        "finatra/utils/src/test/resources",
        "finatra/utils/src/test/scala/com/twitter/finatra/test",
        "util/util-core/src/main/scala/com/twitter/io",
    ],
)
