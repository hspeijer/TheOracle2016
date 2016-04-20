package actors

import akka.actor._
import model._
import play.api.libs.json.{JsObject, Json}

import play.libs.Akka
import scala.collection.SortedMap
import scala.language.postfixOps
import scala.util.Random
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

object OracleActor {
  def props(scheduler:Scheduler) = Props(classOf[OracleActor], scheduler)

  // Idle state
  //
  // During IdleState the oracle is waiting for visitors. It randomly plays some clips that are non specific.
  // The oracle stays in this state until a visitor is detected. Once a visitor is detected through a proximity sensor
  // or interaction with one or more of the buttons it goes to ChallengeState. During this state the buttons animate to
  // draw attention.

  // Challenge state
  //
  // Once a visitor is present an oracle is chosen and it presents itself. The game is explained. During this
  // state the buttons are not active.

  // Question state
  //
  // The buttons indicate through going all on and of that the visitor can now ask a question.
  // After a while the buttons animate to indicate that they can be pressed.
  // After a long while it goes back to challenge state but more impatient
  // If a long time passes without a question it tells the visitor to get lost and goes back to IdleState
  // If a question is answered the impatientce is gone and the visitor can ask more
  // If the visitor waits again it goes impatient again
  // During the answering of a question the stone that was chosen stays lit. It goes out and goes back to the pattern that indicates
  // the visitor can ask another question

  // during playback of a video the inputs are disabled.

}

abstract class BaseState {

  var isPlayingMedia = false

  def receive(message : Any)
}

class IdleState(oracle : OracleActor) extends BaseState {

  BoardActor() ! Message("0", "Idle Sate")

  val introFiles = MediaFile.getMediaFile(List("Intro"))
  var currentSchedule : Cancellable = null
  var switchingState = false

  playNext

  ButtonAnimatorActor() ! ButtonAnimatorActor.Animate("Colours", Button.None)

  override def receive(message: Any): Unit = {
    message match  {
      case _:SensorSelect => {
        //If playing media, wait for clip to finish
        if(!isPlayingMedia && !switchingState) {
          switchingState = true
          switchState()
        }

      }
      case _:PlayMedia => {
        if(!isPlayingMedia) {
          isPlayingMedia = true
        }
      }
      case _:MediaComplete => {
        println("Media Complete")
        isPlayingMedia = false
        if(!switchingState) {
          playNext
        }
      }
      case _ => {
      }
    }
  }

  def switchState() = {
    currentSchedule.cancel()
    BoardActor() ! DoSmoke(2000, 127)
    oracle.scheduler.scheduleOnce(2 seconds, new Runnable {
      override def run(): Unit = {
        ButtonAnimatorActor() ! ButtonAnimatorActor.Stop()
        oracle.currentState = new ChallengeState(oracle)
      }
    })
  }

  def playNext(): Unit = {
    val currentMedia = introFiles(Random.nextInt(introFiles.size))
    //schedule playtime video + 10 seconds + random 0-60 seconds
    currentSchedule = oracle.scheduler.scheduleOnce(10 + Random.nextInt(180) seconds, BoardActor(), PlayMedia(currentMedia))
  }
}

class ChallengeState(oracle : OracleActor) extends BaseState {
  var oracleType : Oracle.Value = Oracle.random()
  var currentSchedule : Cancellable = null
  val mediaFiles = MediaFile.getMediaFile(List("Challenge", oracleType.toString))
  var challengedAgain = false;

  BoardActor() ! Message("0", "Challenge state")

  ButtonAnimatorActor() ! ButtonAnimatorActor.Animate("SensorSelect", Button.None)
  BoardActor() ! PlayMedia(mediaFiles(Random.nextInt(mediaFiles.size)))

  override def receive(message: Any): Unit = {
    message match  {
      case sensors:SensorSelect => {
          ButtonAnimatorActor() ! sensors
      }
      case trigger: SensorTrigger => {
        if(!isPlayingMedia) {
          BoardActor() ! Message("0", "Sensor triggered! " + trigger)
          ButtonAnimatorActor() ! ButtonAnimatorActor.Animate("Pulse", trigger.button)
          currentSchedule.cancel()
          val answers = MediaFile.getMediaFile(List("Answer", oracleType.toString))
          BoardActor() ! PlayMedia(answers(Random.nextInt(answers.size)))
          challengedAgain = false
        }
      }
      case _:PlayMedia => {
        isPlayingMedia = true
      }

      case media:MediaComplete => {
        println("Finished playing " + media.media.name)
        isPlayingMedia = false
        ButtonAnimatorActor() ! ButtonAnimatorActor.Animate("SensorSelect", Button.None)

        if(currentSchedule!= null) {
          currentSchedule.cancel()
        }
        setTimeOut()
      }
      case _ => {
      }
    }
  }

  def setTimeOut() {
    currentSchedule = oracle.scheduler.scheduleOnce(30 seconds, new Runnable {
      override def run(): Unit = {
        if (!challengedAgain) {
          challengedAgain = true
          val answers = MediaFile.getMediaFile(List("ChallengeAgain", oracleType.toString))
          BoardActor() ! PlayMedia(answers(Random.nextInt(answers.size)))
          setTimeOut()
        } else {
          oracle.currentState = new IdleState(oracle)
        }
      }
    })
  }
}

class OracleActor(val scheduler:Scheduler) extends Actor {
  println("Creating Oracle Actor")

  var currentState : BaseState = new IdleState(this)

  override def preStart() = {
    BoardActor() ! Subscribe
  }

  def receive = {
    case message: Any => {
      currentState.receive(message)
    }
  }
}
