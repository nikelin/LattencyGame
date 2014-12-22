package org.evg.actors

import akka.actor.{ActorRef, Actor, ActorLogging}
import akka.io.Tcp
import akka.io.Tcp.{Write, Received}
import akka.util.ByteString

/**
 * Created by Cyril on 22.12.2014.
 */
class IncomeConnectionActor extends Actor with ActorLogging {
  var connection : ActorRef = null
  var gameActor : ActorRef = null

  override def receive: Receive = {
    case Start( connection : ActorRef, gameActor : ActorRef ) => {
      this.connection = connection
      this.gameActor = gameActor

      self ! SendMessage(IncomeConnectionActor.WELCOME_MESSAGE)
      self ! SendMessage(IncomeConnectionActor.WELCOME_MESSAGE_ENG)

      gameActor ! Register()
    }
    case SendMessage(m : String) => {
      connection ! Write( data = ByteString(m + "\n\r", "UTF-8"))
    }
    case Received( data : ByteString ) => {
      /** We received Ctrl+C or Ctrl+Z **/ if ( data(0) == 3 || data(0) == 26 ) {
        gameActor ! Unregister()
        connection ! Tcp.ConfirmedClose
      } else {
        val decoded: String = data.decodeString("UTF-8").replaceAll("\\r\\n|\\r|\\n", " ");
        gameActor ! ReceivedMessage(decoded)
      }
    }
    case DisconnectClient(u : Boolean) => {
      if ( u ) {
        gameActor ! Unregister()
      }
      connection ! Tcp.ConfirmedClose
      context.stop(self)
    }
    case e => {
      log.info("Event : " + e );
    }
  }
}

object IncomeConnectionActor {

  val WELCOME_MESSAGE = "Привет! Попробую найти тебе противника\n\r"
  val WELCOME_MESSAGE_ENG = "Welcome! Wait a second, I'll try to find you a good opponent\n\r"

}

case class ReceivedMessage( val message : String )
case class SendMessage( val message : String )
case class DisconnectClient( val unregistered : Boolean = true )
case class Start( val connectionRef : ActorRef, val gameActor: ActorRef )