name := "tou-app"
version := "1.0"

scalaVersion := "2.13.15"
val sparkVersion = "3.5.3"

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % sparkVersion % "provided",
  "org.apache.spark" %% "spark-sql" % sparkVersion % "provided",
  // add other dependencies here
  "org.scalatest" %% "scalatest" % "3.2.19" % Test,
  "org.mockito" %% "mockito-scala" % "1.17.37" % Test
)

// for creating fat jar via sbt assembly (requires a plugin: refer to project/plugins.sbt)
assembly / assemblyMergeStrategy := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x                             => MergeStrategy.deduplicate
}

libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.11" % Test
