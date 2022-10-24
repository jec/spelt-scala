ThisBuild / scalaVersion := "2.13.8"
ThisBuild / organization := "net.jcain"

// Tests involving the database cannot run concurrently.
Test / parallelExecution := false

val akkaVersion = "2.6.20"

lazy val spelt = (project in file("."))
  .enablePlugins(PlayScala)
  .disablePlugins(PlayLayoutPlugin)
  .settings(
    name := "Spelt",
    version := "0.1.0-SNAPSHOT",
    coverageExcludedPackages := ".*Reverse.*Controller;.*Routes;.*RoutesPrefix",
    libraryDependencies ++= Seq(
      "ch.qos.logback" % "logback-classic" % "1.2.11" % "runtime",
      "com.auth0" % "java-jwt" % "4.0.0",
      "org.bouncycastle" % "bcprov-jdk18on" % "1.72",
      "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.13.4",
      "com.google.inject" % "guice" % "5.1.0",
      "com.google.inject.extensions" % "guice-assistedinject" % "5.1.0",
      "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test,
      "com.typesafe.akka" %% "akka-protobuf-v3" % akkaVersion % Test,
      "com.typesafe.akka" %% "akka-serialization-jackson" % akkaVersion % Test,
      "com.typesafe.akka" %% "akka-stream" % akkaVersion % Test,
      "org.neo4j.driver" % "neo4j-java-driver" % "5.1.0",
      "org.scalatestplus.play" %% "scalatestplus-play" % "5.1.0" % Test,
      "org.springframework.security" % "spring-security-crypto" % "5.7.3"
    )
  )
