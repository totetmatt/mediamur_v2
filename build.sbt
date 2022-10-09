ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.9"
enablePlugins(JavaServerAppPackaging)
lazy val root = (project in file("."))
  .settings(
    name := "mediamur",
    idePackagePrefix := Some("fr.totetmatt.mediamur")
  )

val AkkaVersion = "2.6.8"
val AkkaHttpVersion = "10.2.10"
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
  "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
  "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % AkkaHttpVersion,
  "com.twitter" % "twitter-api-java-sdk" % "2.0.3"

)

libraryDependencies += "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.13.4"