junit_tests(
    sources = ["*.scala"],
    compiler_option_sets = ["fatal_warnings"],
    strict_deps = True,
    dependencies = [
        "3rdparty/jvm/com/github/ben-manes/caffeine",
        "3rdparty/jvm/org/slf4j:slf4j-simple",
        "finagle/finagle-core/src/main",
        "finagle/finagle-mysql/src/main/scala",
        "finagle/finagle-mysql/src/test/scala/com/twitter/finagle/mysql/harness",
        "finagle/finagle-mysql/src/test/scala/com/twitter/finagle/mysql/harness/config",
        "finatra/inject/inject-modules/src/main/scala/com/twitter/inject/modules",
        "finatra/inject/inject-server/src/test/scala/com/twitter/inject/server",
        "finatra/mysql-client/src/main/scala/com/twitter/finatra/mysql/client",
        "finatra/mysql-client/src/main/scala/com/twitter/finatra/mysql/client/modules",
        "finatra/mysql-client/src/test/scala/com/twitter/finatra/mysql",
        "util/util-jvm/src/main/scala",
        "util/util-mock/src/main/scala/com/twitter/util/mock",
        "util/util-stats/src/main/scala/com/twitter/finagle/stats",
    ],
    # the below may be required when running locally on a Mac
    # extra_jvm_options= ['-Djava.io.tmpdir=/tmp/mysql/extracted/'],
)
