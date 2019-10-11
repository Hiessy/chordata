//package com.hiessy.appointment.impl
//
//import com.lightbend.lagom.scaladsl.server.LocalServiceLocator
//import com.lightbend.lagom.scaladsl.testkit.ServiceTest
//import org.scalatest.{AsyncWordSpec, BeforeAndAfterAll, Matchers}
//import com.hiessy.appointment.api._
//
//class AppointmentServiceSpec extends AsyncWordSpec with Matchers with BeforeAndAfterAll {
//
//  private val server = ServiceTest.startServer(
//    ServiceTest.defaultSetup
//      .withCassandra()
//  ) { ctx =>
//    new AppointmentApplication(ctx) with LocalServiceLocator
//  }
//
//  val client = server.serviceClient.implement[AppointmentService]
//
//  override protected def afterAll() = server.stop()
//
//  "Appointment service" should {
//
//    "say hello" in {
//      client.hello("Alice").invoke().map { answer =>
//        answer should ===("Hello, Alice!")
//      }
//    }
//
//    "allow responding with a custom message" in {
//      for {
//        _ <- client.useGreeting("Bob").invoke(GreetingMessage("Hi"))
//        answer <- client.hello("Bob").invoke()
//      } yield {
//        answer should ===("Hi, Bob!")
//      }
//    }
//  }
//}
