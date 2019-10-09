java_library(
    sources = rglobs("*.java"),
    compiler_option_sets = {"fatal_warnings"},
    dependencies = [
        "3rdparty/jvm/com/google/inject:guice",
        "3rdparty/jvm/javax/inject:javax.inject",
        "finatra/examples/java-thrift-server/thrift-example-idl",
        "finatra/examples/java-thrift-server/thrift-example-server/src/main/resources",
        "finatra/inject/inject-core/src/main/scala",
        "finatra/inject/inject-server/src/main/scala",
        "finatra/thrift/src/main/scala",
        "twitter-server/server/src/main/scala",
        "util/util-app/src/main/scala",
        "util/util-slf4j-api/src/main/scala",
    ],
)
