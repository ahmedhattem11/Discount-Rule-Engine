ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.13"

lazy val root = (project in file("."))
  .settings(
    name := "Scala Project",
    // Include the ojdbc6.jar in the project's classpath
    unmanagedJars in Compile += file("lib/ojdbc6.jar")
  )
libraryDependencies += "org.apache.logging.log4j" % "log4j-core" % "2.14.1"


