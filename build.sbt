import play.Project._

name := "play2sec"

organization := "com.github.kompot"

version := "0.0.1"

publishMavenStyle := true

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (version.value.trim.endsWith("SNAPSHOT"))
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

pomExtra :=
  <url>https://github.com/kompot/play2sec</url>
  <licenses>
    <license>
      <name>Apache License, Version 2.0</name>
      <url>http://www.apache.org/licenses/LICENSE-2.0.html</url>
      <distribution>repo</distribution>
    </license>
  </licenses>
  <scm>
    <url>git@github.com:kompot/play2sec.git</url>
    <connection>scm:git:git@github.com:kompot/play2sec.git</connection>
  </scm>
  <developers>
    <developer>
      <id>kompot</id>
      <name>Anton Fedchenko</name>
      <url>http://kompot.name</url>
    </developer>
  </developers>

libraryDependencies ++= Seq(
  cache,
  "org.mindrot" % "jbcrypt" % "0.3m"
)

play.Project.playScalaSettings ++ Seq(
  resolvers += "Sonatype Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/",
  resolvers += "Common maven repository" at "http://repo1.maven.org/maven2/",
  resolvers += "Local maven repository" at "file://" + Path.userHome.absolutePath + "/.m2/repository/"
) ++ org.scalastyle.sbt.ScalastylePlugin.Settings
