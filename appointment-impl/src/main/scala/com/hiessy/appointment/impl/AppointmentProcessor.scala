package com.hiessy.appointment.impl

import java.util.{Date, UUID}

import akka.Done
import com.datastax.driver.core.PreparedStatement
import com.hiessy.appointment.impl.event.{AppointmentEvent, AppointmentStarted, ChangeAppointmentEvent}
import com.hiessy.appointment.model.Status
import com.lightbend.lagom.scaladsl.persistence.cassandra.{CassandraReadSide, CassandraSession}
import com.lightbend.lagom.scaladsl.persistence.{AggregateEventTag, EventStreamElement, ReadSideProcessor}
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, Future, Promise}

class AppointmentProcessor(readSide: CassandraReadSide, session: CassandraSession)(implicit ec: ExecutionContext)
  extends ReadSideProcessor[AppointmentEvent]{

  private final val log: Logger =
    LoggerFactory.getLogger(classOf[AppointmentProcessor])

  private val updateAppointmentPromise = Promise[PreparedStatement]
  private def updateAppointmentStatus: Future[PreparedStatement] = updateAppointmentPromise.future
  private var insertAppStatement: PreparedStatement = _
  private var deleteAppStatement: PreparedStatement = _
  private var updateAppStatement: PreparedStatement = _

  override def buildHandler(): ReadSideProcessor.ReadSideHandler[AppointmentEvent] = {
    readSide.builder[AppointmentEvent]("appointmentOffSet")
      .setGlobalPrepare(createTable)
      .setPrepare(tag => prepareStatements())
      .setEventHandler[AppointmentStarted](insertAppointment)
      .setEventHandler[ChangeAppointmentEvent](updateAppointment)
      .build()
  }


  override def aggregateTags: Set[AggregateEventTag[AppointmentEvent]] = AppointmentEvent.Tag.allTags


  private def createTable(): Future[Done] = {
    for {
      _ <- session.executeCreateTable("""
          CREATE TABLE IF NOT EXISTS appointments (
            id uuid,
            status text,
            start timestamp,
            end timestamp,
            user text,
            provider text,
            PRIMARY KEY (id)
          )
      """)
      _ <- session.executeCreateTable("""
          CREATE INDEX IF NOT EXISTS appointmentIndex
            on appointments (start)
      """)

    } yield Done
  }

  /**
    * Statement definition
    * @return
    */
  private def prepareStatements(): Future[Done] = {

    val updateAppStatus = session.prepare("""
        UPDATE appointments
          SET status = ?, user = ?
        WHERE
          id = ?
      """)

    updateAppointmentPromise.completeWith(updateAppStatus)

    for {
      insert <- session.prepare("INSERT INTO appointments(id, status, start, end, provider, user) VALUES (?, ?, ?, ?, ?, ?)")
      delete <- session.prepare("DELETE FROM appointments where id = ?")
      update <- updateAppStatus
    } yield {
      insertAppStatement = insert
      deleteAppStatement = delete
      updateAppStatement = update
      Done
    }
  }

  /**
    * This method updates the appointment status
    * @param streamElement
    * @return
    */
  private def updateAppointment(streamElement: EventStreamElement[ChangeAppointmentEvent]) = {
    val app = streamElement.event.appointment
    log.info(s"Searching appointment for update")
    selectAppointment(app.id).flatMap {
      case None => throw new IllegalStateException("Appointment not found for id " + app.id)
      case Some(row) => {
        val identity = row.getUUID("id")
        log.info(s"Appointment found with id:${identity}")
        updateAppointmentStatus.map { ps =>
          val bindAppStatus = ps.bind()
          bindAppStatus.setString("status", Status.withStatus(app.status))
          bindAppStatus.setString("user", app.user)
          bindAppStatus.setUUID("id", identity)
          List(bindAppStatus)
        }
      }
    }
  }

  /**
    * This method is in charge of executing the insert statement when the AppointmentStarted event is triggered
    * @param started
    * @return
    */
  private def insertAppointment(streamElement: EventStreamElement[AppointmentStarted]) = {

    val app = streamElement.event.appointment

    Future.successful(
      List(
        insertAppStatement.bind(
          app.id,
          Status.withStatus(app.status),
          Date.from(app.startDate.get),
          Date.from(app.endDate.get),
          app.provider,
          app.user)
      )
    )

  }


  /**
    * This method is in charge of deleting an appointment
    * @param event
    * @return
    */
  private def deleteAppoinment(event: EventStreamElement[_]) = {
    Future.successful(
      List(deleteAppStatement.bind(UUID.fromString(event.entityId)))
    )
  }

  /**
    * This method find a single appointment.
    * @param id
    * @return
    */
  private def selectAppointment(id: UUID) = {
    session.selectOne(s"""
      SELECT
        *
      FROM
        appointments
      WHERE
        id = ?""", id)
  }

}
