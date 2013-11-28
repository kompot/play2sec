import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "play2sec"
  val appVersion      = "0.0.1"

  val appDependencies = Seq(
    cache,
    "org.mindrot" % "jbcrypt" % "0.3m"
  )

  val main = play.Project(appName, appVersion, appDependencies).settings(
    organization := "com.github.kompot"
  ).settings(org.scalastyle.sbt.ScalastylePlugin.Settings: _*)
}
