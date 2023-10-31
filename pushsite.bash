#!/usr/bin/env bash
set -e

savedir=$(pwd)

echo 'cloning finatra/gh-pages...' 1>&2
cd /tmp
rm -rf finatra-github
git clone git@github.com:twitter/finatra.git finatra-github
cd finatra-github
git checkout gh-pages
git pull --force

echo 'cleaning gh-pages'
git rm -r *

echo 'cleaning workspace...' 1>&2
cd $savedir
./sbt clean

echo 'making site...' 1>&2
./sbt --warn site/makeSite
echo 'copying site to finatra/gh-pages...' 1>&2
cp -r $savedir/doc/target/site/ /tmp/finatra-github/ >/dev/null 2>&1

echo 'making unidoc...(this may take some time)' 1>&2
cd $savedir
./sbt --warn unidoc
echo 'copying unidoc to site...' 1>&2
cd $savedir
cp -r $savedir/target/scala-2.12/unidoc/ /tmp/finatra-github/scaladocs >/dev/null 2>&1

# make sure there is a .nojekyll file as we want the reStructuredText user-guide render appropriately in GithubPages
touch /tmp/finatra-github/.nojekyll
echo 'deploying site...' 1>&2
cd /tmp/finatra-github
git add .
git diff-index --quiet HEAD || (git commit -am"site push by $(whoami)"; git push origin gh-pages:gh-pages;)
echo 'completed!' 1>&2
