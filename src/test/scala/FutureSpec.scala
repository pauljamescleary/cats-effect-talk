import java.time.Instant

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Minutes, Span}
import org.scalatest.{Matchers, WordSpec}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object FutureThings {

  def something(name: String): Future[String] = Future {
    println(name)
    name
  }

  // Note: All of these futures are SYNCHRONOUS, yet we are still QUEING EACH STEP!!!
  def somethingWild(): Future[String] = {
    for {
      first <- something("here")
      second <- something("there")
      third <- something("who")
      fourth <- something("where")
      fifth <- something("what")
    } yield fifth
  }
}

class FutureSpec extends WordSpec with Matchers with ScalaFutures {

  // Ugh, need this thing because our timeout is large, and pad it because running
  // in open stack VMs can lead to longer than expected times
  implicit val pc = PatienceConfig(Span(2, Minutes))

  def myFuture(message: String): Future[String] = Future {
    Thread.sleep(1000)
    println(s"Completed at ${Instant.now}")
    message
  }

  "The future" should {
    "be ready" in {
      whenReady(myFuture("hey")) { msg =>
        msg shouldBe "hey"
      }
    }
  }
}
