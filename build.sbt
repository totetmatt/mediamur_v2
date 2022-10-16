import NativePackagerHelper._
ThisBuild / version := "2.0.1"
ThisBuild / scalaVersion := "2.13.9"

enablePlugins(JavaServerAppPackaging)
enablePlugins(UniversalPlugin)

maintainer := "matthieu.totet"

lazy val root = (project in file("."))
  .settings(
    name := "mediamur",
    idePackagePrefix := Some("fr.totetmatt.mediamur")
  )

val AkkaVersion = "2.6.20"
val AkkaHttpVersion = "10.2.10"
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
  "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
  "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % AkkaHttpVersion,
  "com.twitter" % "twitter-api-java-sdk" % "2.0.3"
)

libraryDependencies += "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.13.4"
Universal / mappings  += file("./conf/application.conf.default") -> "conf/application.conf"
Universal / mappings  += file("./sdk.properties") -> "sdk.properties"
Universal / mappings ++= directory("ui")
Universal / javaOptions ++= Seq(
  "-Dconfig.file=conf/application.conf"
)