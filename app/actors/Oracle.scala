package actors

import actors.OracleActor.{Tick, PlayMedia, ButtonSelect, Oracle}
import akka.actor._
import play.api.libs.json.{JsObject, Json}

import play.libs.Akka
import scala.collection.SortedMap
import scala.language.postfixOps
import scala.util.Random
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

object OracleActor {
  def props(scheduler:Scheduler) = Props(classOf[OracleActor], scheduler)

  case class MediaFile(name: String, duration: Double, tags: Seq[String])

  val files = Seq(
    MediaFile("EARTH001",29.14,Seq("EarthOracle","Challenge")),
      MediaFile("EARTH002",14.86,Seq("EarthOracle","ChallengeAgain")),
      MediaFile("EARTH003",10.57,Seq("EarthOracle","Answer","Earth","Fire","Water","Air","Aether","yes")),
      MediaFile("EARTH004",10.59,Seq("EarthOracle","Answer","Earth","Fire","Water","Air","Aether","yes")),
      MediaFile("EARTH005",8.06,Seq("EarthOracle","Answer","Earth","Fire","Water","Air","Aether","yes")),
      MediaFile("EARTH006",7.92,Seq("EarthOracle","Answer","Earth","Fire","Water","Air","Aether","yes")),
      MediaFile("EARTH007",10.82,Seq("EarthOracle","Answer","Earth","Fire","Water","Air","Aether","yes")),
      MediaFile("EARTH008",11.12,Seq("EarthOracle","Answer","Earth","Fire","Water","Air","Aether","no")),
      MediaFile("EARTH009",11.42,Seq("EarthOracle","Answer","Earth","Fire","Water","Air","Aether","no")),
      MediaFile("EARTH010",11.49,Seq("EarthOracle","Answer","Earth","Fire","Water","Air","Aether","no")),
      MediaFile("EARTH011",9.38,Seq("EarthOracle","Answer","Earth","Fire","Water","Air","Aether","no")),
      MediaFile("EARTH012",12.72,Seq("EarthOracle","Answer","Earth","Fire","Water","Air","Aether","no")),
      MediaFile("EARTH013",11.73,Seq("EarthOracle","Answer","Earth","Fire","Water","Air","Aether","maybe")),
      MediaFile("EARTH014",14.33,Seq("EarthOracle","Answer","Earth","Fire","Water","Air","Aether","maybe")),
      MediaFile("EARTH015",13.05,Seq("EarthOracle","Answer","Earth","Fire","Water","Air","Aether","maybe")),
      MediaFile("EARTH016",11.87,Seq("EarthOracle","Answer","Earth","Fire","Water","Air","Aether","none")),
      MediaFile("EARTH017",10.94,Seq("EarthOracle","Answer","Earth","Fire","Water","Air","Aether","none")),
      MediaFile("EARTH018",10.82,Seq("EarthOracle","Answer","Earth","Fire","Water","Air","Aether","none")),
      MediaFile("FIRE0001",19.97,Seq("FireOracle","Challenge")),
      MediaFile("FIRE0002",17.32,Seq("FireOracle","ChallengeAgain")),
      MediaFile("FIRE0003",8.92,Seq("FireOracle","Answer","Earth","Fire","Water","Air","Aether","yes")),
      MediaFile("FIRE0004",9.91,Seq("FireOracle","Answer","Earth","Fire","Water","Air","Aether","yes")),
      MediaFile("FIRE0005",13.35,Seq("FireOracle","Answer","Earth","Fire","Water","Air","Aether","yes")),
      MediaFile("FIRE0006",15.33,Seq("FireOracle","Answer","Earth","Fire","Water","Air","Aether","yes")),
      MediaFile("FIRE0007",11.52,Seq("FireOracle","Answer","Earth","Fire","Water","Air","Aether","yes")),
      MediaFile("FIRE0008",13.03,Seq("FireOracle","Answer","Earth","Fire","Water","Air","Aether","yes")),
      MediaFile("FIRE0009",17.23,Seq("FireOracle","Answer","Earth","Fire","Water","Air","Aether","no")),
      MediaFile("FIRE0010",10.12,Seq("FireOracle","Answer","Earth","Fire","Water","Air","Aether","no")),
      MediaFile("FIRE0011",14.7,Seq("FireOracle","Answer","Earth","Fire","Water","Air","Aether","no")),
      MediaFile("FIRE0012",15.14,Seq("FireOracle","Answer","Earth","Fire","Water","Air","Aether","no")),
      MediaFile("FIRE0013",15.7,Seq("FireOracle","Answer","Earth","Fire","Water","Air","Aether","no")),
      MediaFile("FIRE0014",16.83,Seq("FireOracle","Answer","Earth","Fire","Water","Air","Aether","maybe")),
      MediaFile("FIRE0015",14.84,Seq("FireOracle","Answer","Earth","Fire","Water","Air","Aether","none")),
      MediaFile("FIRE0016",16.97,Seq("FireOracle","Answer","Earth","Fire","Water","Air","Aether","none")),
      MediaFile("FIRE0017",13.05,Seq("FireOracle","Answer","Earth","Fire","Water","Air","Aether","none")),
      MediaFile("FIRE0018",13,Seq("FireOracle","Answer","Earth","Fire","Water","Air","Aether","none")),
      MediaFile("HILLB001",13.58,Seq("HillbOracle","Challenge")),
      MediaFile("HILLB002",8.8,Seq("HillbOracle","ChallengeAgain")),
      MediaFile("HILLB003",7.08,Seq("HillbOracle","Answer","Earth","Fire","Water","Air","Aether","yes")),
      MediaFile("HILLB004",8.27,Seq("HillbOracle","Answer","Earth","Fire","Water","Air","Aether","yes")),
      MediaFile("HILLB005",10.52,Seq("HillbOracle","Answer","Water","yes")),
      MediaFile("HILLB006",12.26,Seq("HillbOracle","Answer","Air","Aether","yes")),
      MediaFile("HILLB007",10.4,Seq("HillbOracle","Answer","Earth","yes")),
      MediaFile("HILLB008",12.03,Seq("HillbOracle","Answer","Fire","no")),
      MediaFile("HILLB009",11.33,Seq("HillbOracle","Answer","Water","no")),
      MediaFile("HILLB010",11.66,Seq("HillbOracle","Answer","Earth","Fire","Water","Air","Aether","no")),
      MediaFile("HILLB011",9.5,Seq("HillbOracle","Answer","Earth","Fire","Water","Air","Aether","no")),
      MediaFile("HILLB012",9.73,Seq("HillbOracle","Answer","Air","no")),
      MediaFile("HILLB013",8.85,Seq("HillbOracle","Answer","Aether","no")),
      MediaFile("HILLB014",10.82,Seq("HillbOracle","Answer","Earth","Fire","Water","Air","Aether","no")),
      MediaFile("HILLB015",12.52,Seq("HillbOracle","Answer","Earth","maybe")),
      MediaFile("HILLB016",12.65,Seq("HillbOracle","Answer","Water","none")),
      MediaFile("HILLB017",9.61,Seq("HillbOracle","Answer","Earth","Fire","Water","Air","Aether","maybe")),
      MediaFile("HILLB018",9.91,Seq("HillbOracle","Answer","Earth","Fire","Water","Air","Aether","none")),
      MediaFile("HILLB019",9.13,Seq("HillbOracle","Answer","Earth","Fire","Water","Air","Aether","maybe")),
      MediaFile("HILLB020",12.89,Seq("HillbOracle","Answer","Earth","Fire","Water","Air","Aether","maybe")),
      MediaFile("HILLB021",10.68,Seq("HillbOracle","Intro")),
      MediaFile("TIME0001",19.44,Seq("TimeOracle","Challenge")),
      MediaFile("TIME0002",17.07,Seq("TimeOracle","Challenge")),
      MediaFile("TIME0003",11.75,Seq("TimeOracle","ChallengeAgain")),
      MediaFile("TIME0004",10.33,Seq("TimeOracle","Answer","Fire","yes")),
      MediaFile("TIME0005",11.82,Seq("TimeOracle","Answer","Fire","yes")),
      MediaFile("TIME0006",12.4,Seq("TimeOracle","Answer","Fire","yes")),
      MediaFile("TIME0007",9.2,Seq("TimeOracle","Intro","Answer","Earth","Fire","Water","Air","Aether","none")),
      MediaFile("TIME0008",5.69,Seq("TimeOracle","Intro","Answer","Earth","Fire","Water","Air","Aether","none")),
      MediaFile("TIME0009",11.96,Seq("TimeOracle","Answer","Water","yes")),
      MediaFile("TIME0010",13.56,Seq("TimeOracle","Answer","Water","yes")),
      MediaFile("TIME0011",11.35,Seq("TimeOracle","Answer","Water","yes")),
      MediaFile("TIME0012",14.91,Seq("TimeOracle","Answer","Water","yes")),
      MediaFile("TIME0013",9.43,Seq("TimeOracle","Answer","Earth","yes")),
      MediaFile("TIME0014",12.86,Seq("TimeOracle","Answer","Earth","yes")),
      MediaFile("TIME0015",10.63,Seq("TimeOracle","Intro")),
      MediaFile("TIME0016",11.22,Seq("TimeOracle","Answer","Air","yes")),
      MediaFile("TIME0017",8.22,Seq("TimeOracle","Answer","Air","yes")),
      MediaFile("TIME0018",9.03,Seq("TimeOracle","Answer","Air","yes")),
      MediaFile("TIME0019",10.19,Seq("TimeOracle","Answer","Aether","yes")),
      MediaFile("TIME0020",11.42,Seq("TimeOracle","Answer","Aether","yes")),
      MediaFile("TIME0021",13.77,Seq("TimeOracle","Answer","Aether","yes")),
      MediaFile("TIME0022",9.98,Seq("TimeOracle","Answer","Fire","no")),
      MediaFile("TIME0023",13.4,Seq("TimeOracle","Answer","Fire","no")),
      MediaFile("TIME0024",13.35,Seq("TimeOracle","Answer","Fire","no")),
      MediaFile("TIME0025",11.75,Seq("TimeOracle","Answer","Air","no")),
      MediaFile("TIME0026",9.15,Seq("TimeOracle","Answer","Air","no")),
      MediaFile("TIME0027",10.73,Seq("TimeOracle","Answer","Water","no")),
      MediaFile("TIME0028",11.19,Seq("TimeOracle","Answer","Water","no")),
      MediaFile("TIME0029",12.4,Seq("TimeOracle","Answer","Water","no")),
      MediaFile("TIME0030",12.35,Seq("TimeOracle","Answer","Earth","no")),
      MediaFile("TIME0031",11.52,Seq("TimeOracle","Answer","Earth","no")),
      MediaFile("TIME0032",13.1,Seq("TimeOracle","Answer","Air","no")),
      MediaFile("TIME0033",12.47,Seq("TimeOracle","Answer","Earth","Fire","Water","Air","Aether","no")),
      MediaFile("TIME0034",13.54,Seq("TimeOracle","Answer","Air","no")),
      MediaFile("TIME0035",13.17,Seq("TimeOracle","Answer","Water","no")),
      MediaFile("TIME0036",10.75,Seq("TimeOracle","Answer","Aether","no")),
      MediaFile("TIME0037",15.02,Seq("TimeOracle","Answer","Aether","no")),
      MediaFile("TIME0038",13.17,Seq("TimeOracle","Answer","Aether","no")),
      MediaFile("TIME0039",8.61,Seq("TimeOracle","Answer","Earth","Fire","Water","Air","Aether","none")),
      MediaFile("TIME0040",13.84,Seq("TimeOracle","Answer","Earth","Fire","Water","Air","Aether","none")),
      MediaFile("TIME0041",14.42,Seq("TimeOracle","Answer","Earth","Fire","Water","Air","Aether","maybe")),
      MediaFile("TIME0042",15.07,Seq("TimeOracle","Answer","Earth","Fire","maybe")),
      MediaFile("TIME0043",14.7,Seq("TimeOracle","Answer","Earth","Fire","maybe")),
      MediaFile("TIME0044",15.46,Seq("TimeOracle","Answer","Earth","Fire","none")),
      MediaFile("TIME0045",12.19,Seq("TimeOracle","Answer","Earth","Fire","Water","Air","Aether","none")),
      MediaFile("TIME0046",12.89,Seq("TimeOracle","Answer","Water","maybe")),
      MediaFile("TIME0047",11.66,Seq("TimeOracle","Answer","Air","maybe")),
      MediaFile("TIME0048",11.05,Seq("TimeOracle","Answer","Aether","maybe")),
      MediaFile("TIME0049",12.26,Seq("TimeOracle","Answer","Earth","maybe")),
      MediaFile("TIME0050",10.4,Seq("TimeOracle","Answer","Aether","maybe")),
      MediaFile("TIME0051",9.87,Seq("TimeOracle","Intro")),
      MediaFile("TIME0052",37.71,Seq("TimeOracle","Intro")),
      MediaFile("TIME0053",51.99,Seq("TimeOracle")),
      MediaFile("TIME0054",17.86,Seq("TimeOracle","Challenge")),
      MediaFile("TIME0055",21.94,Seq("TimeOracle","Challenge")),
      MediaFile("TIME0056",15.84,Seq("TimeOracle","ChallengeAgain")),
      MediaFile("TIME0057",15.67,Seq("TimeOracle","ChallengeAgain")),
      MediaFile("WATER001",13.7,Seq("WaterOracle","Challenge")),
      MediaFile("WATER002",14.12,Seq("WaterOracle","ChallengeAgain")),
      MediaFile("WATER003",10.94,Seq("WaterOracle","Answer","Earth","Fire","Water","Air","Aether","yes")),
      MediaFile("WATER004",8.43,Seq("WaterOracle","Answer","Earth","Fire","Water","Air","Aether","yes")),
      MediaFile("WATER005",7.92,Seq("WaterOracle","Answer","Earth","Fire","Water","Air","Aether","yes")),
      MediaFile("WATER006",10.45,Seq("WaterOracle","Answer","Earth","Fire","Water","Air","Aether","yes")),
      MediaFile("WATER007",10.59,Seq("WaterOracle","Answer","Earth","Fire","Water","Air","Aether","yes")),
      MediaFile("WATER008",14.37,Seq("WaterOracle","Answer","Earth","Fire","Water","Air","Aether","yes")),
      MediaFile("WATER009",8.68,Seq("WaterOracle","Answer","Earth","Fire","Water","Air","Aether","no")),
      MediaFile("WATER010",14,Seq("WaterOracle","Answer","Earth","Fire","Water","Air","Aether","no")),
      MediaFile("WATER011",15.23,Seq("WaterOracle","Answer","Earth","Fire","Water","Air","Aether","no")),
      MediaFile("WATER012",11.87,Seq("WaterOracle","Answer","Earth","Fire","Water","Air","Aether","no")),
      MediaFile("WATER013",12.4,Seq("WaterOracle","Answer","Earth","Fire","Water","Air","Aether","no")),
      MediaFile("WATER014",12.59,Seq("WaterOracle","Answer","Earth","Fire","Water","Air","Aether","no")),
      MediaFile("WATER015",14.16,Seq("WaterOracle","Answer","Earth","Fire","Water","Air","Aether","no")),
      MediaFile("WATER016",10.68,Seq("WaterOracle","Answer","Earth","Fire","Water","Air","Aether","maybe")),
      MediaFile("WATER017",10.68,Seq("WaterOracle","Answer","Earth","Fire","Water","Air","Aether","maybe")),
      MediaFile("WATER018",10.26,Seq("WaterOracle","Answer","Earth","Fire","Water","Air","Aether","maybe")),
      MediaFile("WATER019",11.12,Seq("WaterOracle","Answer","Earth","Fire","Water","Air","Aether","none")),
      MediaFile("WATER020",11.82,Seq("WaterOracle","Answer","Earth","Fire","Water","Air","Aether","none")),
      MediaFile("WATER021",8.96,Seq("WaterOracle","Answer","Earth","Fire","Water","Air","Aether","none")),
      MediaFile("WATER022",7.96,Seq("WaterOracle","Answer","Earth","Fire","Water","Air","Aether","none")),
      MediaFile("WATER023",9.33,Seq("WaterOracle","Answer","Earth","Fire","Water","Air","Aether","none"))
  )

  def getMediaFile(tags: List[String]) = {
    files.filter(file => tags.forall(currentTag => file.tags.exists(tag => tag == currentTag)))
  }

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
  object State extends Enumeration {
    type State = Value
    val Idle, Challenge, Question = Value
  }

  object Oracle extends Enumeration {
    val TimeOracle, EarthOracle, WaterOracle, HillbOracle, FireOracle = Value
    def random() = {
      Oracle(Random.nextInt(Oracle.values.size))
    }
  }

  object Button extends Enumeration {
    type Button = Value
    val Earth, Water, Air, Fire,  Aether, None = Value
  }


  //internal messages
  case class Tick()

  // Messages to client
  case class PlayMedia(name:String)
  case class ButtonLight(fire: Boolean, water: Boolean, earth: Boolean, air : Boolean, aether : Boolean) {
    def this(mask : Int) {
      this(
        (mask & 0x01) > 0,
        (mask & 0x02) > 0,
        (mask & 0x04) > 0,
        (mask & 0x08) > 0,
        (mask & 0x10) > 0
      )
    }
    def toByte() : Byte = {
      var result = 0

      if(fire)   (result = result | 0x01)
      if(water)  (result = result | 0x02)
      if(earth)  (result = result | 0x04)
      if(air)    (result = result | 0x08)
      if(aether) (result = result | 0x10)

      result.toByte
    }
  }
  case class ButtonSelect(button: Button.Value)
}

class ButtonState {

}

abstract class BaseState {
  var videoState : VideoState = null
  var buttonState : ButtonState = null

  def receive(message : Any)

}

class VideoState {

}



class IdleState(oracle : OracleActor) extends BaseState {

  println("Idle State")
  val introFiles = OracleActor.getMediaFile(List("Intro"))
  var currentSchedule : Cancellable = null

  playNext

  ButtonAnimatorActor() ! ButtonAnimatorActor.Animate()

  override def receive(message: Any): Unit = {
    message match  {
      case _:ButtonSelect => {
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

  val mediaFiles = OracleActor.getMediaFile(List("Challenge", oracleType.toString))

  BoardActor() ! PlayMedia(mediaFiles(Random.nextInt(mediaFiles.size)).name)

  override def receive(message: Any): Unit = {
    message match  {
      case _:ButtonSelect => {
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

//    case Tick => {
//      BoardActor() ! PlayMedia(files(Random.nextInt(files.size)).name)
//      BoardActor() ! ButtonLight(Random.nextBoolean(),Random.nextBoolean(),Random.nextBoolean(),Random.nextBoolean(),Random.nextBoolean())
//    }
//    case button:ButtonSelect => {
//      println("Button push received" + button)
//    }
//
//    case media:PlayMedia => {
//      BoardActor() ! media
//    }
  }


}
