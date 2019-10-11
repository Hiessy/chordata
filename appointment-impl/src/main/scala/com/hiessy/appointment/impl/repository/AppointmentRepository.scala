package com.hiessy.appointment.impl.repository

import akka.stream.Materializer
import com.datastax.driver.core.Row
import com.hiessy.appointment.model.{Appointment, Status}
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession

import scala.concurrent.ExecutionContext

private[impl] class AppointmentRepository (session: CassandraSession)(implicit ec: ExecutionContext, mat: Materializer) {

  def getAppointments(provider: String) = {
    session.selectAll(s"""
      SELECT
        *
      FROM
        appointments
      WHERE provider = ? ALLOW FILTERING
    """, provider).map { rows =>
      rows.map(appointmentConverter)
    }
  }

  private def appointmentConverter(item: Row): Appointment = {
    Appointment(
      item.getUUID("id"),
      Status.withStatus(item.getString("status")),
      Some(item.getTimestamp("start").toInstant),
      Some(item.getTimestamp("end").toInstant),
      item.getString("provider"),
      item.getString("user")
    )
  }

}
