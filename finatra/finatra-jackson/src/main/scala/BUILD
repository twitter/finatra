scala_library(name='scala',
  provides = scala_artifact(
    org = 'com.twitter',
    name = 'finatra-jackson',
    repo = artifactory,
  ),
  dependencies=[
    '3rdparty/jvm/org/scalastuff:scalabeans',
    '3rdparty/jvm/com/twitter/bijection:core',
    '3rdparty:commons-lang',
    '3rdparty:grizzled-slf4j',
    '3rdparty:guice',
    '3rdparty:scala-guice',
    '3rdparty:scalap',
    '3rdparty:jackson-annotations',
    '3rdparty:jackson-core',
    '3rdparty:jackson-databind',
    '3rdparty:jackson-datatype-joda',
    '3rdparty:jackson-module-scala',
    '3rdparty/jvm/joda-time',
    '3rdparty/jvm/org/scalaj:scalaj-time',
    'finatra/finatra/finatra-utils'
  ],
  sources=rglobs('*.scala'),
  java_sources=[
    'finatra/finatra/finatra-jackson/src/main/java'
  ],
  resources=[
    'finatra/finatra/finatra-jackson/src/main/resources'
  ]
)
