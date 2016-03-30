package controllers

import play.api._
import play.api.mvc._

import akka.actor._
import scala.concurrent.duration._

import javax.inject._

import actors._

import scala.Left
import scala.Right
import scala.concurrent.Future

import play.api.Logger
import play.api.Play.current
import play.api.libs.json.JsValue
import play.api.mvc.Action
import play.api.mvc.Controller
import play.api.mvc.WebSocket

@Singleton
class Application @Inject() (system: ActorSystem) extends Controller {

  println("Init Application")
  val oracle = system.actorOf(OracleActor.props(system.scheduler))

  system.actorOf(Props[SensorActor])
  system.actorOf(Props[DMXActor])
  system.actorOf(Props[VideoPlayerActor])

  val UID = "uid"
  var counter = 0

  //Use the system's dispatcher as ExecutionContext
  import system.dispatcher

//  val cancellable = system.scheduler.schedule(0 milliseconds, 20 seconds, oracle, Tick)

//  system.scheduler.scheduleOnce(10 seconds, oracle, PlayMedia("TIME0001"))

  def index = Action { implicit request =>
    {
      val uid = request.session.get(UID).getOrElse {
        counter += 1
        counter.toString
      }

      Ok(views.html.index(uid)).withSession {
        println("creation uid " + uid)
        request.session + (UID -> uid)
      }
    }
  }

  def websocket = WebSocket.tryAcceptWithActor[JsValue, JsValue] { implicit request =>
    Future.successful(request.session.get(UID) match {
      case None => Left(Forbidden)
      case Some(uid) => Right(UserActor.props(uid))
    })
  }

  def media(path : String) = Action {
    Ok("File:" + path)
  }

}
