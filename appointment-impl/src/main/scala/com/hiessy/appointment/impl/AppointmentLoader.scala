package com.hiessy.appointment.impl

import com.lightbend.lagom.scaladsl.api.ServiceLocator
import com.lightbend.lagom.scaladsl.api.ServiceLocator.NoServiceLocator
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraPersistenceComponents
import com.lightbend.lagom.scaladsl.server._
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import play.api.libs.ws.ahc.AhcWSComponents
import com.hiessy.appointment.api.AppointmentService
import com.hiessy.appointment.impl.entity.AppointmentEntity
import com.hiessy.appointment.impl.repository.AppointmentRepository
import com.lightbend.lagom.scaladsl.broker.kafka.{LagomKafkaClientComponents}
import com.softwaremill.macwire._

class AppointmentLoader extends LagomApplicationLoader {

  override def load(context: LagomApplicationContext): LagomApplication =
    new AppointmentApplication(context) {
      override def serviceLocator: ServiceLocator = NoServiceLocator
    }

  override def loadDevMode(context: LagomApplicationContext): LagomApplication =
    new AppointmentApplication(context) with LagomDevModeComponents

  override def describeService = Some(readDescriptor[AppointmentService])
}

abstract class AppointmentApplication(context: LagomApplicationContext)
  extends LagomApplication(context)
    with CassandraPersistenceComponents
    with LagomKafkaClientComponents
    with AhcWSComponents {

  // Bind the service that this server provides
  override lazy val lagomServer = serverFor[AppointmentService](wire[AppointmentServiceImpl])
  lazy val appointmentRepository = wire[AppointmentRepository]

  // Register the JSON serializer registry
  override lazy val jsonSerializerRegistry = AppointmentSerializerRegistry

  // Register the Appointment persistent entity
  persistentEntityRegistry.register(wire[AppointmentEntity])

  readSide.register(wire[AppointmentProcessor])
}
