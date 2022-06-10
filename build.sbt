val ScalatraVersion = "2.8.2"
val AkkaVersion = "2.6.19"

ThisBuild / scalaVersion := "2.13.8"
ThisBuild / organization := "net.jcain"

lazy val spelt = (project in file("."))
  .enablePlugins(JettyPlugin)
  .settings(
    name := "Spelt",
    version := "0.1.0-SNAPSHOT",
    libraryDependencies ++= Seq(
      "ch.qos.logback" % "logback-classic" % "1.2.11" % "runtime",
      "com.auth0" % "java-jwt" % "3.19.2",
      "com.typesafe" % "config" % "1.4.2",
      "com.typesafe.akka" %% "akka-actor" % AkkaVersion,
      "com.typesafe.akka" %% "akka-testkit" % AkkaVersion % Test,
      "javax.servlet" % "javax.servlet-api" % "3.1.0" % "provided",
      "org.bouncycastle" % "bcprov-jdk18on" % "1.71",
      "org.eclipse.jetty" % "jetty-webapp" % "9.4.35.v20201120" % "container;compile",
      "org.json4s" %% "json4s-jackson" % "4.0.5",
      "org.neo4j.driver" % "neo4j-java-driver" % "4.4.5",
      "org.scalatra" %% "scalatra" % ScalatraVersion,
      "org.scalatra" %% "scalatra-json" % "2.8.2",
      "org.scalatra" %% "scalatra-scalatest" % ScalatraVersion % Test,
      "org.springframework.security" % "spring-security-crypto" % "5.7.1"
    )
  )
