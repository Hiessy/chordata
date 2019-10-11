package com.hiessy.appointment.impl.event

import com.hiessy.appointment.model.Appointment
import com.lightbend.lagom.scaladsl.persistence.{AggregateEvent, AggregateEventTag}
import play.api.libs.json.{Format, Json}



/**
  * This interface defines all the events that the AppointmentEntity supports.
  */
sealed trait AppointmentEvent extends AggregateEvent[AppointmentEvent] {
  def aggregateTag = AppointmentEvent.Tag
}

object AppointmentEvent {
  val NumShards = 4
  val Tag = AggregateEventTag.sharded[AppointmentEvent](NumShards)
}

case class ChangeAppointmentEvent(appointment: Appointment) extends AppointmentEvent
object ChangeAppointmentEvent {
  implicit val format: Format[ChangeAppointmentEvent] = Json.format
}

case class AppointmentStarted(appointment: Appointment) extends AppointmentEvent
object AppointmentStarted {
  implicit val format: Format[AppointmentStarted] = Json.format
}

case class AppointmentFinished(appointment: Appointment) extends AppointmentEvent
object AppointmentFinished {
  implicit val format: Format[AppointmentFinished] = Json.format
}
