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


## Test

To compile and run all tests:<br>
`sbt test`


## Run

Start the application:<br>
`sbt run`<br>


## Setup and Load data

Add all the register entries to `/etc/hosts`:<br>
Register, Field and Datatype must be there:<br>
`127.0.0.1    register.openregister.dev`<br>
`127.0.0.1    field.openregister.dev`<br>
`127.0.0.1    datatype.openregister.dev`

Also add all the register hosts you would like to access. For example for school:<br>
`127.0.0.1    school.openregister.dev`

Load data into Datatype, Field and Register regsiters: 

* Follow [`https://github.com/openregister/register.register`](https://github.com/openregister/register.register) to load data for Regsiter register
* Follow [`https://github.com/openregister/datatype.register`](https://github.com/openregister/datatype.register) to load data for Datatype register
* Follow [`https://github.com/openregister/field.register`](https://github.com/openregister/field.register) to load data for Field register


Access the application at:<br>
`http://<register-name>.openregister.dev:9000`


Load data into the application for a generic register:<br>
Type `http://<register-name>.openregister.dev:9000/load` in a browser and provide a url to the raw csv or tsv data and click Import<br>
For example, if your register name is `school`, go to [`http://school.openregister.dev:9000/load`](http://school.openregister.dev:9000/load) and enter `https://raw.githubusercontent.com/openregister/school.register/master/data/school/schools.tsv`
