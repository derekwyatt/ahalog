name := "access-log-directive"

version := "0.1"

scalaVersion := "2.11.8"

libraryDependencies := Seq(
  "com.typesafe.akka" %% "akka-http-experimental" % "2.4.7",
  "com.typesafe.akka" %% "akka-http-testkit" % "2.4.7" % "test",
  "org.scalatest" %% "scalatest" % "2.2.6" % "test"
)
