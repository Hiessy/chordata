package com.hiessy.appointment.impl.entity

import com.hiessy.appointment.model.Appointment
import play.api.libs.json.{Format, Json}

case class AppointmentState(appointment: Option[Appointment])

object AppointmentState {
  /**
    * Format for the hello state.
    *
    * Persisted entities get snapshotted every configured number of events. This
    * means the state gets stored to the database, so that when the entity gets
    * loaded, you don't need to replay all the events, just the ones since the
    * snapshot. Hence, a JSON format needs to be declared so that it can be
    * serialized and deserialized when storing to and from the database.
    */
  implicit val format: Format[AppointmentState] = Json.format
}