name := "spark-knn-graphs-outofmemory"

version := "0.1"

scalaVersion := "2.11.8"

parallelExecution in Test := false
fork in Test := true

libraryDependencies += "org.apache.spark" %% "spark-sql" % "2.2.1"
libraryDependencies += "org.apache.spark" %% "spark-mllib" % "2.2.1"
libraryDependencies += "org.apache.spark" %% "spark-hive" % "2.2.1"

libraryDependencies += "info.debatty" % "spark-knn-graphs" % "0.15"
