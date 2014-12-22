package org.evg.actors

import java.net.{InetSocketAddress, ServerSocket}
import java.util.concurrent.TimeUnit

import akka.actor._
import akka.io.Tcp.{Connected}
import akka.io.{IO, Tcp}
import akka.pattern.ask
import akka.util.Timeout
import org.evg.actors.adapters.{Complete, Bind}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

/**
 * Created by Cyril on 22.12.2014.
 */
class ServerActor extends Actor with ActorLogging {
  var endPoint : ServerSocket = null
  val gameActor = context.actorOf(Props[GameActor])

  override def receive : Receive = {
    case StartServer(ioActor : ActorRef, port, host) => {
      implicit val actorSystem = context.system
      implicit val timeout = Timeout(Duration(15, TimeUnit.SECONDS))
      val future = ioActor ? Bind(host, port)
      future.onComplete( (e) => {
        sender() ! Bound(host, port)
      })
    }
    case Connected(_, address) => {
      log.info("Receiving client connection - " + address );
      val actor : ActorRef = context.actorOf(Props[IncomeConnectionActor]);
      sender() ! Tcp.Register(handler = actor)
      actor ! Start(sender(), gameActor)
    }
    case e => {
      log.info("Something has been received: " + e );
    }
  }

}

case class Bound( val host : String, val port : Int )
case class Shutdown()
case class StartServer( val ioAdapter : ActorRef, val port : Int, val host : String )
