VERSION=1.1.1
EXAMPLE_REPO=~/personal/finatra_example


## Usage: replace_from_token /tmp/t APP => replace all ###EXAMPLE$2###

function replace_with_example_app {
  example_app=$(cat $_FINATRA_ROOT/../../src/test/scala/com/twitter/finatra/ExampleSpec.scala | awk '/###BEGIN_APP###/{s=x}{s=s$0"\n"}/###END_APP###/{print s}' | egrep -v 'BEGIN_APP|END_APP')
  example_app_output=$(echo "$example_app" | tr '\n' '\000' | sed -e 's^\x00^{{N}}^g' -e 's^/^{{S}}^g' -e 's^?^{{Q}}^g')
  sed -ie "s^###EXAMPLEAPP###^$example_app_output^g" $1
  tmpfile=$(mktemp /tmp/fin.XXX)
  cat $1 | sed -e 's^{{N}}^\n^g' -e 's^{{S}}^/^g' -e 's^{{Q}}^?^g' > $tmpfile
  mv $tmpfile $1
}

function replace_with_example_spec {
  example_app=$(cat $_FINATRA_ROOT/../../src/test/scala/com/twitter/finatra/ExampleSpec.scala | awk '/###BEGIN_SPEC###/{s=x}{s=s$0"\n"}/###END_SPEC###/{print s}' | egrep -v 'BEGIN_SPEC|END_SPEC')
  example_app_output=$(echo "$example_app" | tr '\n' '\000' | sed -e 's^\x00^{{N}}^g' -e 's^/^{{S}}^g' -e 's^?^{{Q}}^g')
  sed -ie "s^###EXAMPLESPEC###^$example_app_output^g" $1
  tmpfile=$(mktemp /tmp/fin.XXX)
  cat $1 | sed -e 's^{{N}}^\n^g' -e 's^{{S}}^/^g' -e 's^{{Q}}^?^g' > $tmpfile
  mv $tmpfile $1
}
