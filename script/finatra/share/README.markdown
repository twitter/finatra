# Finatra Example

### Runs your app on port 7070

    mvn scala:run

### Testing

    mvn test

### To put on heroku

    heroku create
    git push heroku master

### To run anywhere else

    mvn package
    java -jar target/*-0.0.1-SNAPSHOT-jar-with-dependencies.jar
