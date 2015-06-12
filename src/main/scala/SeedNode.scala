package com.kpbochenek

import akka.cluster.sharding.{ClusterSharding, ShardRegion}

import akka.actor._
import akka.cluster.Cluster
import com.google.common.collect.ImmutableSet
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigValueFactory

object SeedNode extends App {
  val host = "127.0.0.1"
  val port = 2551

  val actorSystemName = "BootActorSystem"

  val seedNode = "akka.tcp://" + actorSystemName + "@" + host + ":" + port
  val config = ConfigFactory.parseResources("seed.conf")
  val system = ActorSystem.create(actorSystemName, config)

  val cluster = Cluster(system)
  
  import MonitorActor._
  ClusterSharding(system).start(
    typeName = MonitorActor.name,
    entryProps = Some(Props[MonitorActor]),
    roleOverride = None,
    rememberEntries = true,
    idExtractor = MonitorActor.extractor,
    shardResolver = MonitorActor.resolver
  )
}
