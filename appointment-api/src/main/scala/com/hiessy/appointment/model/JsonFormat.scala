package com.hiessy.appointment.model

import play.api.libs.json._

object JsonFormat {

  def enumReads[E <: Enumeration](enum: E): Reads[E#Value] = Reads {
    case JsString(s) =>
      try {
        JsSuccess(enum.withName(s).asInstanceOf[E#Value])
      } catch {
        case _: NoSuchElementException =>
          JsError(s"Enumeration expected of type: '${enum.getClass}', but it does not contain '$s'")
      }
    case _ => JsError("String value expected")
  }
  def enumWrites[E <: Enumeration]: Writes[E#Value] = Writes(v => JsString(v.toString))
  def enumFormat[E <: Enumeration](enum: E): Format[E#Value] = {
    Format(enumReads(enum), enumWrites)
  }

}

sealed trait Status
object Status {
  case object InProgress extends Status
  case object Available extends Status
  case object Unavailable extends Status
  case object Booked extends Status
  case object Finished extends Status
  case object Canceled extends Status
  implicit val formatState = Format[Status](
    Reads { js =>
      val state = (JsPath).read[String].reads(js)
      state.fold(
        errors => JsError("State undefined or incorrect"), {
          case "InProgress" => JsSuccess(InProgress)
          case "Available" => JsSuccess(Available)
          case "Unavailable" => JsSuccess(Unavailable)
          case "Booked" => JsSuccess(Booked)
          case "Finished" => JsSuccess(Finished)
          case "Canceled" => JsSuccess(Canceled)
        }
      )
    },
    Writes {
      case InProgress => JsString("InProgress")
      case Available => JsString("Available")
      case Unavailable => JsString("Unavailable")
      case Booked => JsString("Booked")
      case Finished => JsString("Finished")
      case Canceled => JsString("Canceled")
    }
  )

  def withStatus(status: String): Status = {
    status match {
      case "InProgress" => InProgress
      case "Available" => Available
      case "Unavailable" => Unavailable
      case "Booked" => Booked
      case "Finished" => Finished
      case "Canceled" => Canceled
    }
  }

  def withStatus(status: Status): String = {
    status match {
      case InProgress => "InProgress"
      case Available => "Available"
      case Unavailable => "Unavailable"
      case Booked => "Booked"
      case Finished => "Finished"
      case Canceled => "Canceled"
    }
  }
}
