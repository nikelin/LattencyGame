package org.evg.actors

import akka.actor.{Props, ActorRef, ActorLogging, Actor}
import scala.collection.mutable

/**
 * Created by Cyril on 22.12.2014.
 */
class GameActor extends Actor with ActorLogging {

  override def receive: Receive = {
    case Unregister() => {
      GameActor.clearDataFor(sender())
    }

    case Register() => {
      if (!GameActor.actorsPool.isEmpty) {
        val opponent = GameActor.actorsPool.dequeue();

        val gm : GamePair = GameActor.createAndRegisterPair( sender(), opponent, context.actorOf(Props[BattleLoopActor]) )
        gm.startBattle()
      } else {
        GameActor.actorsPool.enqueue(sender())
      }
    }
    case ReceivedMessage(message : String) => {
      for ( pair : GamePair <- GameActor.pairsList ) {
        if ( pair.o1 == sender() || pair.o2 == sender() ) {
          pair.bl ! ResultReceived(message, sender() )
        }
      }
    }
  }

}

case class GamePair( o1 : ActorRef, o2 : ActorRef, bl : ActorRef ) {

  def startBattle() = bl ! StartLoop(o1, o2)

  def stopBattle() = {
    bl ! StopLoop()
  }

}

object GameActor {
  val OPPONENT_CHOSEN : String = "Противник найден. Нажмите пробел, когда увидите цифру 3"
  val OPPONENT_CHOSEN_EN : String = "Opponent chosen. Press SPACE, when you will see number 3"

  var actorsPool : mutable.Queue[ActorRef] = new mutable.Queue[ActorRef]()
  var pairsList : List[GamePair] = List[GamePair]()

  def createAndRegisterPair( o1 : ActorRef, o2 : ActorRef, bl : ActorRef ): GamePair = {
    val x : GamePair = new GamePair(o1, o2, bl);
    pairsList = x :: pairsList;
    x
  }
  
  def clearDataFor( o1 : ActorRef ) : Unit = {
    actorsPool = actorsPool.filterNot( (o) => o == o1 )

    pairsList = pairsList.filterNot( (o : GamePair) =>
      if ( o.o1 == o1 || o.o2 == o1 ) {
        o.stopBattle()
        true
      }
      else {
        false
      }
    )
  }

}

case class Register()
case class Unregister()
