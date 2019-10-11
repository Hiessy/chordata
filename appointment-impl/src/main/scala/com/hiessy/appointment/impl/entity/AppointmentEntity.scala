package com.hiessy.appointment.impl.entity

import akka.Done
import com.hiessy.appointment.impl.command.{AddAppointmentCommand, AppointmentCommand, GetAppointmentsCommand, SetAppointmentCommand}
import com.hiessy.appointment.impl._
import com.hiessy.appointment.impl.event.{AppointmentEvent, AppointmentStarted, ChangeAppointmentEvent}
import com.hiessy.appointment.model.Appointment
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity
import org.slf4j.{Logger, LoggerFactory}

class AppointmentEntity extends PersistentEntity {

  override type Command = AppointmentCommand[_]
  override type Event = AppointmentEvent
  override type State = AppointmentState

  private final val log: Logger =
    LoggerFactory.getLogger(classOf[AppointmentServiceImpl])

  /**
    * The initial state. This is used if there is no snapshotted state to be found.
    */
  override def initialState: AppointmentState = AppointmentState(None)

  /**
    * An entity can define different behaviours for different states, so the behaviour
    * is a function of the current state to a set of actions.
    */
  override def behavior: Behavior = {
    GetAppointments orElse {
      Actions().onCommand[AddAppointmentCommand, Done] {
        case (AddAppointmentCommand(appointment), ctx, _) => {
          ctx.thenPersist(AppointmentStarted(appointment)) { _ =>
            ctx.reply(Done)
          }
        }
      }.onCommand[SetAppointmentCommand, Done] {
        case (SetAppointmentCommand(appointment), ctx, _) => {
          log.info(s" appointment:" + appointment)
          ctx.thenPersist(ChangeAppointmentEvent(appointment)) { _ =>
            ctx.reply(Done)
          }
        }
      }.onEvent {
        case (AppointmentStarted(appointment), _) => {
          log.info(s" appointment:" + appointment)
          AppointmentState(Some(appointment))
        }
        case (ChangeAppointmentEvent(appointment), _) => {
          log.info(s" appointment:" + appointment)
          AppointmentState(Some(appointment))
        }
      }
    }
  }

  private def GetAppointments = Actions()
    .onReadOnlyCommand[GetAppointmentsCommand.type, Appointment] {
    case (GetAppointmentsCommand, ctx, state) => ctx.reply(state.appointment.getOrElse(
        throw new Exception("the appointment can not be read")
      ))
  }

}

