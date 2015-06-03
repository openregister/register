name := """register"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.11.6"

javacOptions ++= Seq("-source", "1.8", "-target", "1.8")

initialize := {
  val _ = initialize.value
  if (sys.props("java.specification.version") != "1.8")
    sys.error("Java 8 is required for this project.")
}

libraryDependencies ++= Seq(
  javaWs,
  "rome" % "rome" % "1.0",
  "org.markdownj" % "markdownj-core" % "0.4",
  "org.postgresql" % "postgresql" % "9.4-1201-jdbc41",
  "org.mongodb" % "mongo-java-driver" % "3.0.0",
  "com.fasterxml.jackson.dataformat" % "jackson-dataformat-csv" % "2.5.2",
  "com.fasterxml.jackson.dataformat" % "jackson-dataformat-yaml" % "2.5.2",
  "org.apache.httpcomponents" % "httpclient" % "4.3.1",
  "org.apache.httpcomponents" % "httpcore" % "4.3.1",
  "org.apache.commons" % "commons-dbcp2" % "2.1",

  // Test only dependencies
  "com.novocode" % "junit-interface" % "0.11" % "test",
  "org.jsoup" % "jsoup" % "1.7.2" % "test",
  "by.stub" % "stubby4j" % "3.1.3" % "test"
)
