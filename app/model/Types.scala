package model

import play.api.libs.json.Json

import scala.util.Random

/**
 * Copyright 2014 mindsteps BV
 *
 * User: Hans Speijer
 * Date: 30/03/16
 * Time: 06:42
 */
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

case class Light(colour: ColourRGB, effect: Short = 255) {
  def toJson() = Json.obj("r" -> colour.red, "g" -> colour.green, "b" -> colour.blue) //"{r:" + colour.red + ",g:" + colour.green + ",b:" + colour.blue + "}"

  def this() {
    this(ColourRGB(0,0,0), 0)
  }
  override def toString() : String =  {"(" + colour.red + "," + colour.green + "," + colour.blue + ")"}
}

case class ColourRGB(red: Short, green: Short, blue: Short)

object ElementColours {
  val colours = List(
    Button.Fire -> ColourRGB(255,0,0),
    Button.Aether -> ColourRGB(255,255,0),
    Button.Earth -> ColourRGB(0,255,0),
    Button.Air -> ColourRGB(0,255,255),
    Button.Water -> ColourRGB(0,0,255)
  )
}

