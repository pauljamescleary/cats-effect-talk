import java.time.Instant

import cats.effect._
import org.scalatest.{Matchers, WordSpec}

object IOThings {

  def something(name: String): IO[String] = IO {
    println(name)
    name
  }

  // Note: All of these futures are SYNCHRONOUS, yet we are still QUEING EACH STEP!!!
  def somethingWild(): IO[String] =
    for {
      first <- something("here")
      second <- something("there")
      third <- something("who")
      fourth <- something("where")
      fifth <- something("what")
    } yield fifth
}

class IOSpec extends WordSpec with Matchers {

  def myIO(message: String): IO[String] = IO {
    Thread.sleep(1000)
    println(s"Completed at ${Instant.now}")
    message
  }

  "The io" should {
    "be ready" in {
      myIO("hey").unsafeRunSync shouldBe "hey"
    }
  }
}
