play application for hosting an open register

[![Build Status](https://travis-ci.org/openregister/register.svg)](https://travis-ci.org/openregister/register)


## Pre-requisites

* Java 8<br>
http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html

* Simple Build Tool<br>
http://www.scala-sbt.org/release/tutorial/Setup.html

* Postgres<br>
http://www.postgresql.org/download/<br>
Create the test and production databases<br>
`createdb openregister`<br>
`createdb testopenregister`

* Mongodb<br>
http://docs.mongodb.org/manual/installation/    

## Test

To compile and run all tests:<br>
`sbt test`


## Run

Start the application:<br>
`sbt run`<br>

Access the application at<br>
`http://localhost:9000`

Load some data into the application. For example the list of schools<br>
`http://localhost:9000/load?url=https://raw.githubusercontent.com/openregister/school.register/master/data/School/schools.tsv`
