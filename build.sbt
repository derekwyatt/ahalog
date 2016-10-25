name := "access-log-directive"

version := "0.1"

scalaVersion := "2.11.8"

libraryDependencies := Seq(
  "com.typesafe.akka" %% "akka-http" % "3.0.0-RC1",
  "com.typesafe.akka" %% "akka-http-testkit" % "3.0.0-RC1" % "test",
  "org.scalatest" %% "scalatest" % "3.0.0" % "test"
)
