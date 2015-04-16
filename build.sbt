name := """register"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.11.1"

javacOptions ++= Seq("-source", "1.8", "-target", "1.8")

initialize := {
  val _ = initialize.value
  if (sys.props("java.specification.version") != "1.8")
    sys.error("Java 8 is required for this project.")
}

libraryDependencies ++= Seq(
  javaWs,
  "org.mongodb" % "mongo-java-driver" % "3.0.0",
  "com.novocode" % "junit-interface" % "0.11" % "test"
)
