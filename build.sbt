val ScalatraVersion = "2.8.2"
val AkkaVersion = "2.6.19"

ThisBuild / scalaVersion := "2.13.8"
ThisBuild / organization := "net.jcain"

// Tests involving the database cannot run concurrently.
Test / parallelExecution := false

lazy val spelt = (project in file("."))
  .enablePlugins(PlayScala)
  .disablePlugins(PlayLayoutPlugin)
  .settings(
    name := "Spelt",
    version := "0.1.0-SNAPSHOT",
    libraryDependencies ++= Seq(
      guice,
      "ch.qos.logback" % "logback-classic" % "1.2.11" % "runtime",
      "com.auth0" % "java-jwt" % "3.19.2",
      "com.typesafe" % "config" % "1.4.2",
      "com.typesafe.akka" %% "akka-actor" % AkkaVersion,
      "com.typesafe.akka" %% "akka-testkit" % AkkaVersion % Test,
      "org.bouncycastle" % "bcprov-jdk18on" % "1.71",
      "org.neo4j.driver" % "neo4j-java-driver" % "4.4.6",
      "org.scalatestplus.play" %% "scalatestplus-play" % "5.1.0" % Test,
      "org.springframework.security" % "spring-security-crypto" % "5.7.1"
    )
  )
