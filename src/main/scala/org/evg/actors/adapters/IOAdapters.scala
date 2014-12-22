package org.evg.actors.adapters

import java.net.InetSocketAddress
import java.util.concurrent.TimeUnit

import akka.pattern.ask
import akka.util.Timeout
import org.evg.actors.Bound
import scala.concurrent.ExecutionContext.Implicits.global
import akka.actor.Actor
import akka.io.{Tcp, IO}

import scala.concurrent.duration.Duration

/**
 * Created by Cyril on 23.12.2014.
 */
class AkkaIOAdapter extends Actor {
  implicit val system = context.system

  override def receive: Receive = {
    case Bind(host : String, port : Int) => {
      implicit val timeout = Timeout(Duration(5, TimeUnit.SECONDS))
      val result = IO(Tcp) ? Tcp.Bind(handler = sender(), localAddress = new InetSocketAddress(host, port))
      result.onComplete((e) => {
        sender() ! Bound(host, port)
      })
    }
  }
}

class IOAdapterMock extends Actor {
  override def receive: Receive = {
    case Bind(host : String, port : Int) => {
      sender() ! Bound(host, port)
    }
  }
}

case class Bind( host : String, port : Int )
case class Complete()
