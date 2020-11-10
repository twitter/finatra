junit_tests(
    sources = ["*.java"],
    dependencies = [
        "3rdparty/jvm/junit",
        "3rdparty/jvm/org/scalatestplus:junit",
        "3rdparty/jvm/org/slf4j:slf4j-api",
        scoped(
            "3rdparty/jvm/org/slf4j:slf4j-simple",
            scope = "runtime",
        ),
        "finatra/inject/inject-app/src/test/scala/com/twitter/inject/app",
        "finatra/inject/inject-core/src/main/scala/com/twitter/inject",
        "finatra/inject/inject-slf4j/src/main/scala/com/twitter/inject",
        "finatra/validation/src/main/scala/com/twitter/finatra/validation",
        "finatra/validation/src/test/scala/com/twitter/finatra/validation/tests",
    ],
)
