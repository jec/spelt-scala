ThisBuild / scalaVersion := "3.3.1"
ThisBuild / organization := "net.jcain"

// Tests involving the database cannot run concurrently.
Test / parallelExecution := false

val pekkoVersion = "1.0.1"

lazy val spelt = (project in file("."))
  .enablePlugins(PlayScala)
  // Use the sbt default layout instead of Play's app/ layout.
  .disablePlugins(PlayLayoutPlugin)
  .settings(
    name := "Spelt",
    version := "0.1.0-SNAPSHOT",
    coverageExcludedPackages := ".*Reverse.*Controller;.*Routes;.*RoutesPrefix",
    libraryDependencies ++= Seq(
      guice,
      "ch.qos.logback" % "logback-classic" % "1.4.7" % "runtime",
      "com.auth0" % "java-jwt" % "4.3.0",
      "org.bouncycastle" % "bcprov-jdk18on" % "1.72",
      "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.14.2",
      "com.google.inject" % "guice" % "5.1.0",
      "com.google.inject.extensions" % "guice-assistedinject" % "5.1.0",
      "org.apache.pekko" %% "pekko-actor-testkit-typed" % pekkoVersion % Test,
      "org.apache.pekko" %% "pekko-protobuf-v3" % pekkoVersion % Test,
      "org.apache.pekko" %% "pekko-serialization-jackson" % pekkoVersion % Test,
      "org.apache.pekko" %% "pekko-stream" % pekkoVersion % Test,
      "org.neo4j.driver" % "neo4j-java-driver" % "5.6.0",
      // Added the following to fix a Guice error. Without this, it was
      // using 2.8.1, which has an Akka dependency (should be Pekko).
      "org.playframework" %% "play-ahc-ws" % "3.0.0",
      "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.0" % Test,
      "org.springframework.security" % "spring-security-crypto" % "6.0.2"
    )
  )
