play application for hosting an open register

[![Build Status](https://travis-ci.org/openregister/register.svg)](https://travis-ci.org/openregister/register)


## Pre-requisites

* Java 8<br>
http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html

* Simple Build Tool > 0.13.5<br>
http://www.scala-sbt.org/release/tutorial/Setup.html

* Postgres 9.4.x<br>
http://www.postgresql.org/download/<br>
Create the test and production databases<br>
`createdb openregister`<br>
`createdb testopenregister`

* Mongodb 2.6.x<br>
http://docs.mongodb.org/manual/installation/    

## Test

To compile and run all tests:<br>
`sbt test`


## Run

Give the register a name:<br>
Edit `conf/application.conf` and set `register.name` property. For example: `register.name=school`

Start the application:<br>
`sbt run`<br>

Access the application at:<br>
[`http://localhost:9000`](http://localhost:9000)

Load some data into the application:<br>
Go to [`http://localhost:9000/load`](http://localhost:9000/load), provide a url to the raw csv or tsv data and click Import<br>
For example, if your register name is `school`, use https://raw.githubusercontent.com/openregister/school.register/master/data/school/schools.tsv
