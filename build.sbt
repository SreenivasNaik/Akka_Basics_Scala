name := "Akka_Basics_Scala"

version := "0.1"

scalaVersion := "2.13.6"


val akkaVersion = "2.5.32"
val scalaTestVersion = "3.0.1"
val akkaHttpVersion     = "10.0.10"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
  "org.scalactic" %% "scalactic" % "3.2.10",
   "org.scalatest" %% "scalatest" % "3.2.10"

// "org.scalactic" %% "scalactic" % "3.2.9",
 // "com.typesafe.akka"%% "akka-http" % akkaHttpVersion,

)

resolvers += "Artima Maven Repository" at "https://repo.artima.com/releases"
