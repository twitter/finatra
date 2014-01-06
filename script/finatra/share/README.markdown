# ###PROJECT_NAME###

Finatra requires either [maven](http://maven.apache.org/) or [sbt](http://www.scala-sbt.org/release/docs/Getting-Started/Setup.html) to build and run your app.

## SBT Instructions

### Runs your app on port 7070

    sbt run

### Testing

    sbt test

### Packaging (fatjar)

    sbt assembly


## Maven Instructions

### Runs your app on port 7070

    mvn scala:run

### Testing

    mvn test

### Packaging (fatjar)

    mvn package


## Heroku

### To put on heroku

    heroku create
    git push heroku master

### To run anywhere else

    java -jar target/*-0.0.1-SNAPSHOT-jar-with-dependencies.jar
