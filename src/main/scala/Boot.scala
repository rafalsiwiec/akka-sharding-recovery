package com.kpbochenek

import akka.actor._
import akka.cluster.Cluster
import akka.cluster.sharding.{ClusterSharding, ShardRegion}
import akka.pattern.ask
import akka.persistence.{Persistence, PersistentActor, PersistentRepr}
import akka.util.Timeout

import scala.concurrent.Await
import scala.concurrent.duration._

case class Message(id: Int, body: String)
case class RequestSum(id: Int, x: Int)


object Summator {
  val name = "summator"

  val idExtractor: ShardRegion.IdExtractor = {
    case Message(id, body) ⇒ (id.toString, body)
    case RequestSum(id, x) => (id.toString, x)
  }

  val shardResolver: ShardRegion.ShardResolver = {
    case Message(id, _) ⇒ (id % 3).toString
    case RequestSum(id, _) ⇒ (id % 3).toString
  }

}

class Summator extends PersistentActor {

  var current_sum = 0
  val nm = self.path.name

  override def preStart() = {
    println(s"Summator creates $nm")
    super.preStart()
  }

  override def receiveCommand = {
    case x: Int =>
      persist(x) { case value: Int =>
        current_sum += value
        println(s"[$nm] adds $value ---> $current_sum")
        sender() ! current_sum
      }
    case x: String =>
      sender() ! x + "Pong"
  }

  override def receiveRecover = {
    case x: Int =>
      println(s"[$nm] Recovery with $x")
      current_sum += x
    case r => println(s"Unhandled recovery message $r")
  }

  override def persistenceId: String = self.path.name
}


object MonitorActor {
  val name = "monitoring"

  case class Envelope(id: Int, message: Any)
  case class Monitor(actorId: Int)
  case object SchedulePing
  case object Ping
  case object Pong

  val extractor: ShardRegion.IdExtractor = {
    case Envelope(id, body) ⇒ (id.toString, body)
  }

  val resolver: ShardRegion.ShardResolver = {
    case Envelope(id, _) ⇒ (id % 10).toString
  }
}

class MonitorActor extends PersistentActor {
  import MonitorActor._

  val n = self.path.name
  var monitors: Set[Int] = Set()
  var monitorRegion: ActorRef = null

  implicit val timeout: Timeout = 1 second
  implicit val ex = context.dispatcher
  val cancelMonitor = context.system.scheduler.schedule(1 second, 5 seconds, self, SchedulePing)

  override def preStart() = {
    monitorRegion = ClusterSharding(context.system).shardRegion(MonitorActor.name)
    super.preStart()
  }

  override def receiveCommand: Receive = {
    case Monitor(actorId) => persist(Monitor(actorId)) { case x =>
      println(s"[$n] starting monitoring $actorId")
      monitors = monitors + actorId
    }
    case Ping => sender ! Pong
    case SchedulePing =>
      monitors.foreach(pingActor)
  }

  def pingActor(id: Int) = {
    val result = Await.result((monitorRegion ? Envelope(id, Ping)).mapTo[Pong.type], 3 seconds)
    println(s"[$n] succesfully ping $id")
  }

  override def receiveRecover: Receive = {
    case Monitor(actorId) =>
      println(s"[$n] recovery monitoring to $actorId")
      monitors += actorId
  }

  override def persistenceId: String = self.path.name
}


object Boot extends App {
  implicit val timeout: Timeout = 2000.seconds
  println("#### Application started ####")

  val system = ActorSystem("BootActorSystem")
  val cluster = Cluster(system)

  /*
  ClusterSharding(system).start(
    typeName = Summator.name,
    entryProps = Some(Props[Summator]),
    roleOverride = None,
    rememberEntries = true,
    idExtractor = Summator.idExtractor,
    shardResolver = Summator.shardResolver)

  val sumregion = ClusterSharding(system).shardRegion(Summator.name)

  println(s"Sending message to REGION --------")

  val result = sumregion ? RequestSum(1, 5)
  println(s"RESULT(5) = " + Await.result(result.mapTo[Int], 3 seconds))

  sumregion ! Message(2, "siemano ;)")

  val result2 = sumregion ? RequestSum(4, 7)
  println(s"RESULT(7) = " + Await.result(result2.mapTo[Int], 3 seconds))

  val result3 = sumregion ? RequestSum(1, 1)
  println(s"RESULT(6) = " + Await.result(result3.mapTo[Int], 3 seconds))
   */

  import MonitorActor._
  ClusterSharding(system).start(
    typeName = MonitorActor.name,
    entryProps = Some(Props[MonitorActor]),
    roleOverride = None,
    rememberEntries = true,
    idExtractor = MonitorActor.extractor,
    shardResolver = MonitorActor.resolver
  )

  /*
  val monitoring = ClusterSharding(system).shardRegion(MonitorActor.name)
  monitoring ! Envelope(1, Monitor(4))
  monitoring ! Envelope(1, Monitor(5))
  monitoring ! Envelope(1, Monitor(6))
  */

  /*
  val monitoring = ClusterSharding(system).shardRegion(MonitorActor.name)
  monitoring ! Envelope(7, Monitor(8))
  monitoring ! Envelope(8, Monitor(9))
  */

  println("#### Application ended ####")
  //Await.result(system.terminate(), 3 seconds)
}
