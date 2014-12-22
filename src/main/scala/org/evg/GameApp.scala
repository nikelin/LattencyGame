package org.evg

import akka.actor.{ActorSystem, Props}
import org.evg.actors.adapters.AkkaIOAdapter
import org.evg.actors.{StartServer, ServerActor}

/**
 * Created by Cyril on 22.12.2014.
 */
object GameApp {
  // we need an ActorSystem to host our application in
  implicit val system = ActorSystem("gameApp")

  def main(args : Array[String]) : Unit = {
    val gameServer = system.actorOf(
      Props[ServerActor],
      name = "gameServer"
    )
    val akkaIoAdapter = system.actorOf(
      Props[AkkaIOAdapter],
      name = "akkaAdapter"
    )

    gameServer ! StartServer(
      akkaIoAdapter,
      if (args.length > 0) args(0).toInt else 23,
      if ( args.length > 1 ) args(1) else "localhost"
    )
  }

}
