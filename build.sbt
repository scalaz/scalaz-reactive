import Scalaz._

enablePlugins(MicrositesPlugin)
micrositeName := "Scalaz Reactive"
micrositeDescription := "A high-performance, purely-functional library for reactive programming based on efficient incremental computation."

lazy val commonSettings = Seq(
  organization := "org.scalaz",
  version := "0.1.0-SNAPSHOT",
  resolvers +=
    "Sonatype OSS Snapshots".at("https://oss.sonatype.org/content/repositories/snapshots")
)

val dependencies = Seq(
  "org.scalaz" %% "scalaz-core" % "7.2.26",
  "org.scalaz" %% "scalaz-zio"  % "0.1-SNAPSHOT"
)

val testDependencies = Seq(
  "org.specs2" %% "specs2-core"          % "4.2.0" % Test,
  "org.specs2" %% "specs2-scalacheck"    % "4.2.0" % Test,
  "org.specs2" %% "specs2-matcher-extra" % "4.2.0" % Test
)

publishTo in ThisBuild := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots".at(nexus + "content/repositories/snapshots"))
  else
    Some("releases".at(nexus + "service/local/staging/deploy/maven2"))
}

dynverSonatypeSnapshots in ThisBuild := true

lazy val sonataCredentials = for {
  username <- sys.env.get("SONATYPE_USERNAME")
  password <- sys.env.get("SONATYPE_PASSWORD")
} yield Credentials("Sonatype Nexus Repository Manager", "oss.sonatype.org", username, password)

credentials in ThisBuild ++= sonataCredentials.toSeq

addCommandAlias("fmt", "all scalafmtSbt scalafmt test:scalafmt")
addCommandAlias("check", "all scalafmtSbtCheck scalafmtCheck test:scalafmtCheck")

lazy val core =
  (project in file("core"))
    .settings(
      commonSettings ++
        stdSettings("reactive-core"),
      libraryDependencies ++= (dependencies ++ testDependencies)
    )

lazy val examples = (project in file("examples"))
  .settings(
    commonSettings ++
      stdSettings("reactive-examples")
  )
  .dependsOn(
    core
  )
