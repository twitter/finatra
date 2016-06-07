#!/bin/bash

set -e

dir=/tmp/finatra.$$
trap "rm -fr $dir" 0 1 2

echo 'making unidoc...' 1>&2
./sbt unidoc >/dev/null 2>&1

echo 'cloning...' 1>&2
git clone -b gh-pages-source --single-branch git@github.com:twitter/finatra.git $dir >/dev/null 2>&1

savedir=$(pwd)
cd $dir
git rm -fr ./source/docs
cp -R $savedir/target/scala-2.11/unidoc/ ./source/docs/
git add -f .
git commit -am"Update scaladocs by $(whoami)"
git push origin gh-pages-source
bundle install
rake setup_github_pages[git@github.com:twitter/finatra.git]
rake generate
rake deploy