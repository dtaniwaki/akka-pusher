organization := "com.github.dtaniwaki"

name := "akka-pusher"

scalaVersion := "2.11.7"
crossScalaVersions := Seq("2.11.7")

scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked")

val akkaV = "2.4.9"
val specs2V = "3.6.4"

val developmentDependencies = Seq(
  "com.typesafe.akka"       %%  "akka-actor"                        % akkaV,
  "com.typesafe.akka"       %%  "akka-http-core"                    % akkaV,
  "com.typesafe.akka"       %%  "akka-http-spray-json-experimental" % akkaV,
  "com.github.nscala-time"  %%  "nscala-time"                       % "2.2.0",
  "org.slf4j"               %   "slf4j-api"                         % "1.7.12",
  "net.ceedubs"             %%  "ficus"                             % "1.1.2"
)
val testDependencies = Seq(
  "com.typesafe.akka"   %%  "akka-testkit"  % akkaV % "test",
  "org.specs2"          %%  "specs2-core"   % specs2V % "test",
  "org.specs2"          %%  "specs2-matcher" % specs2V % "test",
  "org.specs2"          %%  "specs2-matcher-extra" % specs2V % "test",
  "org.specs2"          %%  "specs2-mock"   % specs2V % "test"
)
libraryDependencies ++= developmentDependencies ++ testDependencies

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
