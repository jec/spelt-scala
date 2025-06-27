ThisBuild / scalaVersion := "3.3.6"
ThisBuild / semanticdbEnabled := true
ThisBuild / semanticdbVersion := scalafixSemanticdb.revision
ThisBuild / organization := "net.jcain"

// Tests involving the database cannot run concurrently.
Test / parallelExecution := false

val pekkoVersion = "1.1.4"
val guiceVersion = "6.0.0"

lazy val spelt = (project in file("."))
  .enablePlugins(PlayScala)
  // Use the sbt default layout instead of Play's app/ layout.
  .disablePlugins(PlayLayoutPlugin)
  .settings(
    name := "Spelt",
    version := "0.1.0-SNAPSHOT",
    scalacOptions += "-Wunused:imports",
    coverageExcludedPackages := ".*Reverse.*Controller;.*Routes;.*RoutesPrefix",
    libraryDependencies ++= Seq(
      guice,
      "ch.qos.logback" % "logback-classic" % "1.5.18" % "runtime",
      "com.auth0" % "java-jwt" % "4.5.0",
      "org.bouncycastle" % "bcprov-jdk18on" % "1.81",
      "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.19.1",
      "com.google.inject" % "guice" % guiceVersion,
      "com.google.inject.extensions" % "guice-assistedinject" % guiceVersion,
      "io.github.neotypes" %% "neotypes-core" % "1.2.2",
      "io.github.neotypes" %% "neotypes-generic" % "1.2.2",
      "org.apache.pekko" %% "pekko-actor-testkit-typed" % pekkoVersion % Test,
      "org.apache.pekko" %% "pekko-protobuf-v3" % pekkoVersion % Test,
      "org.apache.pekko" %% "pekko-serialization-jackson" % pekkoVersion % Test,
      "org.apache.pekko" %% "pekko-stream" % pekkoVersion % Test,
      "org.neo4j.driver" % "neo4j-java-driver" % "5.28.5",
      "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.1" % Test,
      "org.springframework.security" % "spring-security-crypto" % "6.5.1",
      "org.wvlet.airframe" %% "airframe-ulid" % "2025.1.14",
    ),
  )

lazy val outliner = (project in file("outliner"))
  .settings(
    name := "Outliner",
    libraryDependencies ++= Seq(
      "net.ruippeixotog" %% "scala-scraper" % "3.2.0",
    ),
  )
