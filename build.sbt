
name := "akka sharding example"

version := "1.0-SNAPSHOT"

scalaVersion := "2.11.6"

resolvers ++= Seq(
    "Typesafe Release Repository" at "http://repo.typesafe.com/typesafe/releases/"
    //"Akka Snapshot Repository" at "http://repo.akka.io/snapshots/"     //Needed if you want to use akka 2.4-SNAPSHOT
)

resolvers += "krasserm at bintray" at "http://dl.bintray.com/krasserm/maven"

//resolvers += ("jdgoldie at bintray" at "http://dl.bintray.com/jdgoldie/maven")

fork := true

lazy val akkaV = "2.4-M1"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaV,
  "com.typesafe.akka" %% "akka-remote" % akkaV,
  "com.typesafe.akka" %% "akka-cluster" % akkaV,
  "com.typesafe.akka" %% "akka-cluster-tools" % akkaV,
  "com.typesafe.akka" %% "akka-cluster-sharding" % akkaV,
  "com.typesafe.akka" %% "akka-persistence-experimental" % akkaV,

  "com.github.krasserm" %% "akka-persistence-cassandra" % "0.4-SNAPSHOT"  //Works with akka 2.4, you need to build it yourself from wip-akka2.4 branch
  //"com.github.krasserm" %% "akka-persistence-cassandra" % "0.3.8"       //Works with akka 2.3.x

  // needed if you want to use leveldb-shared persistence
  //"org.iq80.leveldb"            % "leveldb"          % "0.7",
  //"org.fusesource.leveldbjni"   % "leveldbjni-all"   % "1.8"
)
