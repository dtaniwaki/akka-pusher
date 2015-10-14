organization := "com.github.dtaniwaki"

name := "akka-pusher"

version := "0.0.1"

scalaVersion := "2.11.7"

scalacOptions += "-deprecation"
javaOptions += "-Dconfig.resource=akka-pusher-test.conf"

val akkaV = "2.3.14"
val akkaHttpV = "1.0"
val specs2V = "3.6.4"

libraryDependencies ++= Seq(
  "com.typesafe.akka"   %%  "akka-actor"    % akkaV,
  "com.typesafe.akka"   %%  "akka-testkit"  % akkaV % "test",
  "com.typesafe.akka"   %%  "akka-http-core-experimental" % akkaHttpV,
  "com.typesafe.akka"   %%  "akka-http-experimental" % akkaHttpV,
  "io.spray"            %%  "spray-http"    % "1.3.2",
  "io.spray"            %%  "spray-json"    % "1.3.2",
  "com.typesafe"        %   "config"        % "1.3.0",
  "com.typesafe.scala-logging" %%  "scala-logging" % "3.1.0",
  "com.github.nscala-time" %% "nscala-time" % "2.2.0",
  "com.github.tototoshi" %% "slick-joda-mapper" % "2.0.0",
  "org.specs2"          %%  "specs2-core"   % specs2V % "test",
  "org.specs2"          %%  "specs2-matcher" % specs2V % "test",
  "org.specs2"          %%  "specs2-matcher-extra" % specs2V % "test",
  "org.specs2"          %%  "specs2-mock"   % specs2V % "test"
)
