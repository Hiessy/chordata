package com.hiessy.appointment.model

import play.api.libs.json.{Format, Json}

case class SetAppointmentRequest(id: String, status: Status, user: String)

object SetAppointmentRequest {
  implicit val format: Format[SetAppointmentRequest] = Json.format[SetAppointmentRequest]
}





