package com.hiessy.appointment.api

import akka.{Done, NotUsed}
import com.hiessy.appointment.model.{AddAppointmentEvent, Appointment, SetAppointmentRequest}
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.api.transport.Method
import com.lightbend.lagom.scaladsl.api.{Service, ServiceCall}
import play.api.libs.json.{Format, Json}

/**
  * The Appointment service interface.
  * <p>
  * This describes everything that Lagom needs to know about how to serve and
  * consume the AppointmentService.
  */
trait AppointmentService extends Service {

  def addAppointment(provider: String): ServiceCall[AddAppointmentEvent, Done]
  def getAppointments(provider: String): ServiceCall[NotUsed, Seq[Appointment]]
  def setAppointment(provider: String): ServiceCall[SetAppointmentRequest, Done]

  //def appEvents: Topic[AppointmentEvent]

  override final def descriptor = {
    import Service._
    // @formatter:off
    named("appointment")
      .withCalls(
        restCall(Method.POST,"/api/appointment/:provider", addAppointment _),
        restCall(Method.GET,"/api/appointment/:provider", getAppointments _),
        restCall(Method.PUT,"/api/appointment/:provider", setAppointment _)
      ).withAutoAcl(true)
    // @formatter:on
  }
}