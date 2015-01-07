# ###PROJECT_NAME###

Finatra requires either [maven](http://maven.apache.org/) or [sbt](http://www.scala-sbt.org/release/docs/Getting-Started/Setup.html) to build and run your app.

The `.env` file is used to configure Heroku environment variables locally. See [heroku's docs](https://devcenter.heroku.com/articles/config-vars) for more information. *Do not check it into source control!*.


## SBT Instructions

### Runs your app on port 7070

    sbt run

### Testing

    sbt test

### Packaging

    sbt stage

## Maven Instructions

### Runs your app on port 7070

    mvn scala:run

### Testing

    mvn test

### Packaging (fatjar)

    mvn package

## Heroku

Install [heroku toolbelt](https://toolbelt.heroku.com/).

You may also need to install foreman:

    gem install foreman

### To put on heroku

    heroku login
    git init
    heroku create --buildpack https://github.com/heroku/heroku-buildpack-scala.git
    heroku config:set ENV=production
    git add .
    git commit -am "Initial commit"
    git push heroku master

### To run locally like it does on heroku

    foreman start web

### To run anywhere else

    ./target/universal/stage/bin/<appname>
    
