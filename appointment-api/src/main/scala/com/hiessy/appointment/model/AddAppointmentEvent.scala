package com.hiessy.appointment.model

import play.api.libs.json._

case class AddAppointmentEvent(start: String, end: String, user: String)

object AddAppointmentEvent {
  implicit val format: Format[AddAppointmentEvent] = Json.format[AddAppointmentEvent]
}




