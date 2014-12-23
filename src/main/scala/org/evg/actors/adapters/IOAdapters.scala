package org.evg.actors.adapters

import java.net.InetSocketAddress
import java.util.concurrent.TimeUnit

import akka.pattern.ask
import akka.util.Timeout
import org.evg.actors.Bound
import scala.concurrent.ExecutionContext.Implicits.global
import akka.actor.{ActorRef, Actor}
import akka.io.{Tcp, IO}

import scala.concurrent.duration.Duration
import scala.util.Success

/**
 * Created by Cyril on 23.12.2014.
 */
class AkkaIOAdapter extends Actor {

  override def receive: Receive = {
    case Bind(ref : ActorRef, host : String, port : Int) => {
      implicit val system = context.system
      val senderRef : ActorRef = sender()
      implicit val timeout = Timeout(Duration(5, TimeUnit.SECONDS))
      val result = IO(Tcp) ? Tcp.Bind(handler = ref, localAddress = new InetSocketAddress(host, port))
      result.onComplete((e) => {
        senderRef ! Bound(host, port)
      })
    }
  }
}

class IOAdapterMock extends Actor {
  override def receive: Receive = {
    case Bind(ref : ActorRef, host : String, port : Int) => {
      ref ! Bound(host, port)
    }
  }
}

case class Bind( ref : ActorRef, host : String, port : Int )
case class Complete()
