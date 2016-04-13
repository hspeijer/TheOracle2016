package model

import model.Button.Button

/**
 * Copyright 2014 mindsteps BV
 *
 * User: Hans Speijer
 * Date: 30/03/16
 * Time: 06:42
 */

case class PlayMedia(media:MediaFile)
case class MediaComplete(media: MediaFile)
case class SensorSelect(sensors: Set[Button])
case class SensorTrigger(button: Button)
case class DoSmoke(duration: Int, intensity: Short)
case class LightState(lights: Seq[Light]) {
  def this() = {
    this(new Array[Light](5))
  }
  def fire = {
    lights(0).colour != ColourRGB(0, 0, 0)
  }
  def aether = {
    lights(1).colour != ColourRGB(0, 0, 0)
  }
  def earth = {
    lights(2).colour != ColourRGB(0, 0, 0)
  }
  def air = {
    lights(3).colour != ColourRGB(0, 0, 0)
  }
  def water = {
    lights(4).colour != ColourRGB(0, 0, 0)
  }
}
case class Timeout()
