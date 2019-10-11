package com.hiessy.appointment.model

import java.time.{Duration, Instant}
import java.time.temporal.ChronoUnit
import java.util.UUID

import play.api.libs.json.{Format, Json}

case class Appointment(
    id: UUID,
    status: Status,
    startDate: Option[Instant],
    endDate: Option[Instant],
    provider: String,
    user: String
  ) {

  def start() = {
    copy(
      id = UUID.randomUUID(),
      status = Status.InProgress
    )
  }

  def finish() = {
    copy(
      status = Status.Finished
    )
  }

  def setDuration(duration: Duration) = {
    copy(
      endDate = Some(startDate.get.plus(duration))
    )
  }

}

object Appointment {
  implicit val format: Format[Appointment] = Json.format[Appointment]
}
