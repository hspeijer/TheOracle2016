package actors

import akka.actor._
import model.{Oracle, PlayMedia, SensorSelect, MediaFile}
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

  println("Idle State")
  val introFiles = MediaFile.getMediaFile(List("Intro"))
  var currentSchedule : Cancellable = null

  playNext

  ButtonAnimatorActor() ! ButtonAnimatorActor.Animate()

  override def receive(message: Any): Unit = {
    message match  {
      case _:SensorSelect => {
        println("Message received " + message)
        currentSchedule.cancel()
        ButtonAnimatorActor() ! ButtonAnimatorActor.Stop()
        oracle.currentState = new ChallengeState(oracle)
      }
      case _:PlayMedia => {
        println("Idle Play Media")
        ButtonAnimatorActor() ! ButtonAnimatorActor.Animate()
        playNext
      }
      case _ => {
      }
    }
  }

  def playNext(): Unit = {
    var currentMedia = introFiles(Random.nextInt(introFiles.size))

    //schedule playtime video + 10 seconds + random 0-30 seconds
    currentSchedule = oracle.scheduler.scheduleOnce(currentMedia.duration + 10 + Random.nextInt(30) seconds, BoardActor(), PlayMedia(currentMedia.name))
  }
}

class ChallengeState(oracle : OracleActor) extends BaseState {
  println("Challenge State")
  var oracleType : Oracle.Value = Oracle.random()

  val mediaFiles = MediaFile.getMediaFile(List("Challenge", oracleType.toString))

  BoardActor() ! PlayMedia(mediaFiles(Random.nextInt(mediaFiles.size)).name)

  override def receive(message: Any): Unit = {
    message match  {
      case _:SensorSelect => {
        oracle.currentState = new IdleState(oracle)
      }
      case _:PlayMedia => {
      }
      case _ => {
      }
    }
  }
}

class OracleActor(val scheduler:Scheduler) extends Actor {
  println("Creating Oracle Actor")

  var currentState : BaseState = new IdleState(this)

  override def preStart() = {
    BoardActor() ! Subscribe
  }

  def receive = {
    case message : Any => {
      currentState.receive (message)
    }
  }
}
