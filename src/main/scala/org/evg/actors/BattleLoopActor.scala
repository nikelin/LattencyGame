package org.evg.actors

import java.util.concurrent.TimeUnit

import akka.actor.{ActorRef, ActorLogging, Actor}

import scala.concurrent.duration.FiniteDuration
import scala.util.Random

import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by Cyril on 23.12.2014.
 */
class BattleLoopActor extends Actor with ActorLogging {

  val random = new Random()
  var state : Int = BattleLoopActor.STATE_RANDOM

  var firstOpponent : ActorRef = null;
  var secondOpponent : ActorRef = null;

  override def receive: Receive = {
    case StartLoop(o1 : ActorRef, o2 : ActorRef ) => {
      firstOpponent = o1; secondOpponent = o2;

      firstOpponent ! SendMessage(GameActor.OPPONENT_CHOSEN)
      firstOpponent ! SendMessage(GameActor.OPPONENT_CHOSEN_EN)
      secondOpponent ! SendMessage(GameActor.OPPONENT_CHOSEN)
      secondOpponent ! SendMessage(GameActor.OPPONENT_CHOSEN_EN)

      doSchedule()
    }

    case ResultReceived( s : String, receivedFrom : ActorRef) => {
      if ( s == " " ) {
        val opponent : ActorRef = ( if (firstOpponent == receivedFrom) secondOpponent else firstOpponent);
        state match {
          case BattleLoopActor.STATE_AWAIT => {
            receivedFrom ! SendMessage(BattleLoopActor.YOU_WIN_MSG)
            receivedFrom ! SendMessage(BattleLoopActor.YOU_WIN_MSG_EN)
            opponent ! SendMessage(BattleLoopActor.YOU_LOSE_MSG)
            opponent ! SendMessage(BattleLoopActor.YOU_LOSE_MSG_EN)

            receivedFrom ! DisconnectClient()
            opponent ! DisconnectClient()

            state = BattleLoopActor.STATE_FINISHED
          }
          case BattleLoopActor.STATE_RANDOM => {
            receivedFrom ! SendMessage (BattleLoopActor.YOU_WAS_TOO_FAST_MSG)
            receivedFrom ! SendMessage (BattleLoopActor.YOU_WAS_TOO_FAST_MSG_EN)

            opponent ! SendMessage (BattleLoopActor.YOUR_OPPONENT_WAS_TOO_FAST_MSG)
            opponent ! SendMessage (BattleLoopActor.YOUR_OPPONENT_WAS_TOO_FAST_MSG_EN)

            receivedFrom ! DisconnectClient ()
            opponent ! DisconnectClient ()

            state = BattleLoopActor.STATE_FINISHED
          }
        }
      }
    }
    case LoopTick() => {
      if ( state == BattleLoopActor.STATE_RANDOM ) {
        val message: Int = random.nextInt(4);
        firstOpponent ! SendMessage(String.valueOf(message));
        secondOpponent ! SendMessage(String.valueOf(message));

        if (message != 3) {
          doSchedule();
        } else {
          state = BattleLoopActor.STATE_AWAIT
        }
      }
    }

    case StopLoop() => {
      firstOpponent ! DisconnectClient(false)
      secondOpponent ! DisconnectClient(false)

      context.stop(self)
    }
  }

  private def doSchedule(): Unit = {
    context.system.scheduler.scheduleOnce(
      FiniteDuration(4-random.nextInt(3), TimeUnit.SECONDS),
      self,
      LoopTick()
    )
  }

}

object BattleLoopActor {
  /**
   * Possible loop actor stats
   */
  val STATE_RANDOM = 1; // phase of numbers generation, until the desired '3' will be reached
  val STATE_AWAIT = 2; // phase on which we waiting for players response
  val STATE_FINISHED = 3; // final phase when response received  and winner chosed (somehow)

  val YOU_WIN_MSG = "Вы нажали пробел первым и победили"
  val YOU_WIN_MSG_EN = "You were first, and you are winner!"

  val YOU_LOSE_MSG = "Вы не успели и проиграли"
  val YOU_LOSE_MSG_EN = "You weren't so quick, as your opponent was and that's why you are lose this battle!"

  val YOU_WAS_TOO_FAST_MSG = "Вы поспешили и проиграли"
  val YOU_WAS_TOO_FAST_MSG_EN = "You hurried and lost the battle!"

  val YOUR_OPPONENT_WAS_TOO_FAST_MSG = "Your opponent hurried and lost the battle! So, you won."
  val YOUR_OPPONENT_WAS_TOO_FAST_MSG_EN = "Ваш противник поспешил и вы выйграли"

}

case class ResultReceived( s : String, o1 : ActorRef )
case class LoopTick()
case class StartLoop(o1 : ActorRef, o2 : ActorRef)
case class StopLoop()
