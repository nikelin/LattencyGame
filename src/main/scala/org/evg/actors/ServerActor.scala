package org.evg.actors

import java.net.{InetSocketAddress, ServerSocket}
import java.util.concurrent.TimeUnit

import akka.actor._
import akka.io.Tcp.{Connected}
import akka.io.{Tcp, IO}
import akka.pattern.ask
import akka.util.Timeout
import org.evg.actors.adapters.{Complete, Bind}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success}

/**
 * Created by Cyril on 22.12.2014.
 */
class ServerActor extends Actor with ActorLogging {
  var endPoint : ServerSocket = null
  val gameActor = context.actorOf(Props[GameActor])

  override def receive : Receive = {
    case StartServer(ioActor : ActorRef, port, host) => {
      implicit val actorSystem = context.system
      val senderRef : ActorRef = sender()
      implicit val timeout = Timeout(Duration(5, TimeUnit.SECONDS))
      val future = ioActor ? Bind(self, host, port)
      future.onComplete( (e) => {
        e match {
          case Success(Bound(host, port)) => {
            self ! Bound(host, port)
            senderRef ! Bound(host, port)
          }
          case Failure(e : Throwable) => {
            log.error(e, "FAIL: Server not started")
          }
        }
      })
    }
    case Bound(host : String, port : Int ) => {
      log.info(f"Server started on $host:$port")
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
