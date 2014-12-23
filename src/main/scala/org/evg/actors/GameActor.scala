package org.evg.actors

import akka.actor.{Props, ActorRef, ActorLogging, Actor}
import akka.persistence.{RecoveryCompleted, SnapshotOffer, PersistentActor}
import scala.collection.mutable

/**
 * Created by Cyril on 22.12.2014.
 */
class GameActor extends Actor with ActorLogging {
  val state : GameActorState = new GameActorState()

  override def receive: Receive = {
//    case Register() => {
//      persist(StateChangedEvent(state))(stateUpdated)
//      persist(StateChangedEvent(state)) { event =>
//        stateUpdated(event)
//        context.system.eventStream.publish(event)
//      }
//    }

    case Unregister() => {
      state.clearDataFor(sender())
    }

    case Register() => {
      if (!state.isEmptyPool()) {
        val opponent = state.nextFromPool();

        val gm : GamePair = state.createAndRegisterPair( sender(), opponent, context.actorOf(Props[BattleLoopActor]) )
        gm.startBattle()
      } else {
        state.putToPool(sender())
      }
    }
    case ReceivedMessage(message : String) => {
      for ( pair : GamePair <- state.pairs() ) {
        if ( pair.o1 == sender() || pair.o2 == sender() ) {
          pair.bl ! ResultReceived(message, sender() )
        }
      }
    }
  }


}

case class GameActorState() {
  private var actorsPool : mutable.Queue[ActorRef] = new mutable.Queue[ActorRef]()
  private var pairsList : List[GamePair] = List[GamePair]()

  def updated( state : GameActorState ) = {
    actorsPool = state.actorsPool
    pairsList = state.pairsList
  }

  def pairs() : Iterator[GamePair] = pairsList.iterator

  def putToPool( actor : ActorRef ) : Unit = actorsPool.enqueue(actor)

  def isEmptyPool() : Boolean = actorsPool.isEmpty

  def nextFromPool() : ActorRef = actorsPool.dequeue()

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

case class GamePair( val o1 : ActorRef, val o2 : ActorRef, val bl : ActorRef ) {

  def startBattle() = bl ! StartLoop(o1, o2)

  def stopBattle() = {
    bl ! StopLoop()
  }

}

object GameActor {
  val OPPONENT_CHOSEN : String = "Противник найден. Нажмите пробел, когда увидите цифру 3"
  val OPPONENT_CHOSEN_EN : String = "Opponent chosen. Press SPACE, when you will see number 3"
}

case class StateChangedEvent( val state : GameActorState )
case class Register()
case class Unregister()
