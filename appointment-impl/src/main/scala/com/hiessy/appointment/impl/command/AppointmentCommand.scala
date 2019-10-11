package com.hiessy.appointment.impl.command

import akka.Done
import com.hiessy.appointment.model.Appointment
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity.ReplyType

/**
  * This interface defines all the commands that the AppointmentEntity supports.
  */
sealed trait AppointmentCommand[R] extends ReplyType[R]

case class AddAppointmentCommand(appointment: Appointment) extends AppointmentCommand[Done]
case class SetAppointmentCommand(appointment: Appointment) extends AppointmentCommand[Done]
case object GetAppointmentsCommand extends AppointmentCommand[Appointment]
