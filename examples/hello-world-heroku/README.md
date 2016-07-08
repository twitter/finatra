# Finatra Hello World Heroku Example Application

* A simple example finatra application that is deployable to [Heroku](https://heroku.com) and [integrates](https://github.com/rlazoti/finagle-metrics) with [Dropwizard/Codahale metrics](https://github.com/dropwizard/metrics).
* Finatra examples are built in different ways depending on the branch you are in:

If you're in master or a feature branch
----------------------------------------------------------
* Development from master or feature branches is not currently supported for this example. Please switch to a release branch and see the instructions below.

If you're in a tagged release branch (e.g. [finatra-2.2.0](https://github.com/twitter/finatra/tree/finatra-2.2.0))
----------------------------------------------------------

### Run the server on Heroku ###
Copy the hello-world-heroku directory contents (minus the .git directory) to another location locally.

```
$ cp -R hello-world-heroku ~/finatra-hello-world
```

Initialize a git repository in the new directory location:

```
$ cd ~/finatra-hello-world
$ git init
Initialized empty Git repository in ~/finatra-hello-world/.git/
```

Create a `.gitignore` file (notice the newlines):

```
$ echo ".DS_Store
classes/
target/
sbt-launch.jar" > .gitignore
```

Commit all the files to master:

```
$ git add .
$ git commit -m "Initial commit."
```

Compile and stage the application:

```
$ sbt compile stage
```

Make sure you have the [Heroku Toolbelt](https://toolbelt.heroku.com/) [installed](https://devcenter.heroku.com/articles/getting-started-with-scala#set-up).

Create a new app in Heroku:

```
$ heroku create
Creating nameless-lake-8055 in organization heroku... done, stack is cedar-14
http://nameless-lake-8055.herokuapp.com/ | https://git.heroku.com/nameless-lake-8055.git
Git remote heroku added
```

Then deploy the example application to Heroku:

```
$ git push heroku master
Counting objects: 480, done.
Delta compression using up to 4 threads.
Compressing objects: 100% (376/376), done.
Writing objects: 100% (480/480), 27.68 MiB | 16.24 MiB/s, done.
Total 480 (delta 101), reused 0 (delta 0)
remote: Compressing source files... done.
remote: Building source:
	...
```

You can then open the application in a browser with `heroku open`, e.g.:

```
$ heroku open hi?name=foo
```


### Run the example locally with the Heroku Toolbelt  ###

See the [Heroku documentation](https://devcenter.heroku.com/articles/getting-started-with-scala#run-the-app-locally) on running an app locally with Foreman.


```
$ heroku local web
19:47:56 web.1  | started with pid 77663
19:47:59 web.1  | I 0528 02:47:59.058 THREAD1: HttpMuxer[/admin/metrics.json] = com.twitter.finagle.stats.MetricsExporter(<function1>)
19:47:59 web.1  | I 0528 02:47:59.096 THREAD1: HttpMuxer[/admin/per_host_metrics.json] = com.twitter.finagle.stats.HostMetricsExporter(<function1>)
19:47:59 web.1  | I 0528 02:47:59.183 THREAD1: Serving admin http on 0.0.0.0/0.0.0.0:0
19:47:59 web.1  | I 0528 02:47:59.218 THREAD1: Finagle version 6.25.0 (rev=78909170b7cc97044481274e297805d770465110)
19:48:00 web.1  | 2015-05-27 19:48:00,550 INF            HttpRouter                Adding routes
19:48:00 web.1  | GET     /hi
```

The app will now be running at [http://localhost:5000/hi?name=foo](http://localhost:5000/hi?name=foo). `Ctrl-C` to exit.
