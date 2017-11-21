
name := """play_crud"""

organization := "com.boris"

version := "1.0"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

//resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"
//
//resolvers += "Akka Snapshot Repository" at "http://repo.akka.io/snapshots/"

scalaVersion := "2.11.11"

libraryDependencies ++= Seq(
  cache,
  ws,
  "com.typesafe.slick" %% "slick" % "3.1.1",
  "com.typesafe.slick" %% "slick-hikaricp" % "3.1.0",
  "com.typesafe.play" %% "play-slick" % "2.0.0",
  "org.postgresql" % "postgresql" % "9.4.1209.jre7",
  "org.flywaydb" %% "flyway-play" % "3.0.1",
  "com.typesafe.play" %% "play-json" % "2.5.10",
  // JWT
  "com.pauldijou" %% "jwt-play" % "0.13.0"
)
