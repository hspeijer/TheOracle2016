name := """TheOracle2016"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.6"

libraryDependencies ++= Seq(
  jdbc,
  cache,
  ws,
  specs2 % Test,
  "com.pi4j" % "pi4j-core" % "1.1-SNAPSHOT",
  "com.google.protobuf" % "protobuf-java" % "2.6.1"
)

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"
resolvers += "oss-snapshots-repo" at "https://oss.sonatype.org/content/groups/public"

watchSources <+= sourceDirectory map { _ / "app" / "actors" }


// Play provides two styles of routers, one expects its actions to be injected, the
// other, legacy style, accesses its actions statically.
routesGenerator := InjectedRoutesGenerator


fork in run := true