VERSION=1.6.0
EXAMPLE_REPO=~/personal/code/finatra_example


function replace_with_example_app {
  local example_app=$(cat $_FINATRA_ROOT/../../src/test/scala/com/twitter/finatra/ExampleSpec.scala | awk '/###BEGIN_APP###/{s=x}{s=s$0"\n"}/###END_APP###/{print s}' | egrep -v 'BEGIN_APP|END_APP')
  local tmpfile=$(mktemp /tmp/fin.XXX)
  m4 --undefine incr -D__EXAMPLEAPP__="$example_app" $1 > $tmpfile
  mv $tmpfile $2
}

function replace_with_example_spec {
  local example_spec=$(cat $_FINATRA_ROOT/../../src/test/scala/com/twitter/finatra/ExampleSpec.scala | awk '/###BEGIN_SPEC###/{s=x}{s=s$0"\n"}/###END_SPEC###/{print s}' | egrep -v 'BEGIN_SPEC|END_SPEC')
  local tmpfile=$(mktemp /tmp/fin.XXX)
  m4 --undefine incr -D__EXAMPLESPEC__="$example_spec" $1 > $tmpfile
  mv $tmpfile $2
}
