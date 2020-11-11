organization := "com.github.dtaniwaki"

name := "akka-pusher"

scalaVersion := "2.13.1"
crossScalaVersions := Seq("2.13.1")

scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked")

val akkaHttp = "10.2.1"
val akka = "2.6.10"
val specs2V = "4.10.5"

val developmentDependencies = Seq(
  "com.typesafe.akka"       %%  "akka-actor"                        % akka,
  "com.typesafe.akka"       %%  "akka-stream"                       % akka,
  "com.typesafe.akka"       %%  "akka-http"                         % akkaHttp,
  "com.github.nscala-time"  %%  "nscala-time"                       % "2.24.0",
  "org.slf4j"               %   "slf4j-api"                         % "1.7.12",
  "com.iheart"              %%  "ficus"                             % "1.5.0"
)
val testDependencies = Seq(
  "com.typesafe.akka"   %%  "akka-testkit"  % akka % "test",
  "org.specs2"          %%  "specs2-core"   % specs2V % "test",
  "org.specs2"          %%  "specs2-matcher" % specs2V % "test",
  "org.specs2"          %%  "specs2-matcher-extra" % specs2V % "test",
  "org.specs2"          %%  "specs2-mock"   % specs2V % "test"
)
libraryDependencies ++= developmentDependencies ++ testDependencies

val circeVersion = "0.12.3"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % circeVersion)

fork in Test := true
parallelExecution in Test := true
javaOptions in Test ++= Seq(
  s"-Djava.util.Arrays.useLegacyMergeSort=true"
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
