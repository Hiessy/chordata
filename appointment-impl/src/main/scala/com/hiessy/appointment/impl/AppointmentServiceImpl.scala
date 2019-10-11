package com.hiessy.appointment.impl

import java.time._
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.UUID

import akka.actor.ActorSystem
import akka.kafka.scaladsl.Consumer
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import akka.{Done, NotUsed}
import com.hiessy.appointment.api.AppointmentService
import com.hiessy.appointment.impl.command.{AddAppointmentCommand, SetAppointmentCommand}
import com.hiessy.appointment.impl.entity.AppointmentEntity
import com.hiessy.appointment.impl.repository.AppointmentRepository
import com.hiessy.appointment.model.Status.Available
import com.hiessy.appointment.model.{AddAppointmentEvent, Appointment, SetAppointmentRequest, Status}
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.persistence.PersistentEntityRegistry
import com.typesafe.config.ConfigFactory
import org.apache.kafka.common.serialization.StringDeserializer
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent._
import scala.util.{Failure, Success}

/**
  * Implementation of the AppointmentService.
  */
class AppointmentServiceImpl(
                persistentEntityRegistry: PersistentEntityRegistry,
                appointmentRepository: AppointmentRepository) extends AppointmentService {


  implicit val system = ActorSystem.create("AppointmentActor")
  implicit val materializer = ActorMaterializer()
  lazy val mskBrokers: String = ConfigFactory.load.getString("aws.msk.nodes")
  // consumer setting
  val consumerSettings: ConsumerSettings[String, String] = setupConsumerSetting

  // consumer
  val consumer = setupConsumer

  // handle shutdown
  implicit val ec: ExecutionContextExecutor = system.dispatcher
  handleConsumerShutdown

  private final val log: Logger =
    LoggerFactory.getLogger(classOf[AppointmentServiceImpl])

  /**
    * Add a new appointment
    * @param provider
    * @return
    */
  override def addAppointment(provider: String): ServiceCall[AddAppointmentEvent, Done] = ServiceCall { request =>
    val ref = persistentEntityRegistry.refFor[AppointmentEntity](provider)
    log.info(s" Adding a new appointment for ${provider}")
    val format = DateTimeFormatter.ofPattern( "yyyy-MM-dd HH:mm")
    val start = LocalDateTime.parse( request.start , format ).toInstant(ZoneOffset.UTC)
    val end = LocalDateTime.parse( request.end , format ).toInstant(ZoneOffset.UTC)
    //TODO check if appointment exists
    val newAppointment: Appointment = Appointment(UUID.randomUUID(), Available, Some(start), Some(end), provider, request.user)
    val newApp = AddAppointmentCommand(newAppointment)
    ref.ask(newApp)
  }

  /**
    * Get all the appointments regardless of their status
    * @param provider
    * @return
    */
  override def getAppointments(provider: String): ServiceCall[NotUsed, Seq[Appointment]] = ServiceCall { _ =>
    log.info(s"Getting appointments for ${provider}")
    appointmentRepository.getAppointments(provider)
  }

  /**
    * Set an appointment
    * @param provider
    * @return
    */
  override def setAppointment(provider: String): ServiceCall[SetAppointmentRequest, Done] = { request =>
    log.info(s"Updating appointment for ${provider}")
    val ref = persistentEntityRegistry.refFor[AppointmentEntity](provider)
    val updateAppointment: Appointment = Appointment(UUID.fromString(request.id), request.status, None, None, provider, request.user)
    ref.ask(SetAppointmentCommand(updateAppointment))
  }

  /**
    * Generate the the appointments for the current day.
    * @return
    */
   def generateAppointments(provider: String): ServiceCall[NotUsed, Done] = ServiceCall { _ =>
    log.info("Generating appointment for the current day")
    val ref = persistentEntityRegistry.refFor[AppointmentEntity](provider)
    val today = LocalDateTime.now()
    val initial = LocalDateTime.of(today.getYear(), today.getMonth(), today.getDayOfMonth(), 9, 0)
    val start = initial.toInstant(ZoneOffset.UTC)
    val end = start.plus(20, ChronoUnit.MINUTES)
    val duration = Duration.between(start, end)
    def setStartAndEnd(h:Int, m:Int) = LocalDateTime.of(today.getYear(), today.getMonth(), today.getDayOfMonth(), h, m).toInstant(ZoneOffset.UTC)

    val appointments = List(
      Appointment(UUID.randomUUID(), Status.Available, Some(setStartAndEnd(9,0)), Some(setStartAndEnd(9,0).plus(duration)), provider, "UNDEFINED"),
      Appointment(UUID.randomUUID(), Status.Available, Some(setStartAndEnd(9,20)), Some(setStartAndEnd(9,20).plus(duration)), provider, "UNDEFINED"),
      Appointment(UUID.randomUUID(), Status.Available, Some(setStartAndEnd(9,40)), Some(setStartAndEnd(9,40).plus(duration)), provider, "UNDEFINED"),
      Appointment(UUID.randomUUID(), Status.Available, Some(setStartAndEnd(10,0)), Some(setStartAndEnd(10,0).plus(duration)), provider, "UNDEFINED"),
      Appointment(UUID.randomUUID(), Status.Available, Some(setStartAndEnd(10,20)), Some(setStartAndEnd(10,20).plus(duration)), provider, "UNDEFINED"),
      Appointment(UUID.randomUUID(), Status.Available, Some(setStartAndEnd(10,40)), Some(setStartAndEnd(10,40).plus(duration)), provider, "UNDEFINED")
    )
    appointments.foreach(
      appointment => ref.ask(AddAppointmentCommand(appointment))
    )
    Future.successful(Done)
  }

  private def setupConsumerSetting = {
    ConsumerSettings(system, new StringDeserializer, new StringDeserializer)
      .withBootstrapServers(mskBrokers)
      .withGroupId("appointment-group")
  }

  private def setupConsumer = {
    Consumer
      .committableSource(consumerSettings, Subscriptions.topics("appointment"))
      .map(msg => {
        // Do something with the message
        println(s"got new appointment- ${msg.record.value()}")

      })
      //this will publish up stream, since it's not getting used this can be ignored
      .runWith(Sink.ignore)
  }

  private def handleConsumerShutdown = {
    consumer onComplete {
      case Success(_) =>
        println("Successfully terminated consumer")
        system.terminate()
      case Failure(err) =>
        println(err.getMessage)
        system.terminate()
    }
  }
}

