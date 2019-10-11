organization in ThisBuild := "com.hiessy"
version in ThisBuild := "1.0-SNAPSHOT"

// the Scala version that will be used for cross-compiled libraries
scalaVersion in ThisBuild := "2.12.4"
lagomServiceGatewayPort in ThisBuild := 9018
lagomServiceGatewayPort in ThisBuild := 9020

val macwire = "com.softwaremill.macwire" %% "macros" % "2.3.0" % "provided"
val scalaTest = "org.scalatest" %% "scalatest" % "3.0.4" % Test

lazy val `appointment` = (project in file("."))
  .aggregate(`appointment-api`, `appointment-impl`)

lazy val `appointment-api` = (project in file("appointment-api"))
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslKafkaClient,
      lagomScaladslKafkaBroker,
      lagomScaladslApi
    )
  )

lazy val `appointment-impl` = (project in file("appointment-impl"))
  .enablePlugins(LagomScala)
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslPersistenceCassandra,
      lagomScaladslTestKit,
      "com.datastax.cassandra" % "cassandra-driver-extras" % "3.0.0",
      macwire,
      scalaTest
    )
  )
  .settings(lagomForkedTestSettings: _*)
  .dependsOn(`appointment-api`)
