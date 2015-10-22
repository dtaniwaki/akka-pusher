organization := "com.github.dtaniwaki"

name := "akka-pusher"

scalaVersion := "2.11.7"

scalacOptions += "-deprecation"

val akkaV = "2.3.14"
val akkaHttpV = "1.0"
val specs2V = "3.6.4"

libraryDependencies ++= Seq(
  "com.typesafe.akka"   %%  "akka-actor"    % akkaV,
  "com.typesafe.akka"   %%  "akka-testkit"  % akkaV % "test",
  "com.typesafe.akka"   %%  "akka-http-core-experimental" % akkaHttpV,
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

publishArtifact in Test := false
publishMavenStyle := true
pomIncludeRepository := { _ => false }
pomExtra := (
  <url>http://github.com/dtaniwaki/akka-pusher</url>
    <licenses>
      <license>
        <name>MIT</name>
        <url>http://opensource.org/licenses/MIT</url>
      </license>
    </licenses>
    <scm>
      <connection>scm:git:github.com/dtaniwaki/akka-pusher.git</connection>
      <developerConnection>scm:git:git@github.com:dtaniwaki/akka-pusher.git</developerConnection>
      <url>github.com/dtaniwaki/akka-pusher</url>
    </scm>
    <developers>
      <developer>
        <id>dtaniwaki</id>
        <name>Daisuke Taniwaki</name>
        <url>https://github.com/dtaniwaki</url>
      </developer>
    </developers>
  )

credentials += Credentials(Path.userHome / ".sbt" / ".credentials")
publishTo <<= version { (v: String) =>
  val nexus = "https://oss.sonatype.org/"
  if (v.trim.endsWith("SNAPSHOT"))
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

useGpg := true
