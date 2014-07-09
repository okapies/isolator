name := "isolator"

version := "0.1.0"

organization := "com.github.okapies"

crossScalaVersions := Seq("2.11.1")

javacOptions ++= Seq("-source", "1.6", "-target", "1.6")

javacOptions in doc := Seq("-source", "1.6")

libraryDependencies ++= Seq()

mainClass := Some("IsolatorOpenCVApp")
