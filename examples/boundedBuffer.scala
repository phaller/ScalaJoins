/**
 * Scala Joins
 *
 * @author Philipp Haller
 */

package examples

import joins._
import events._

import scala.collection.mutable.Queue

trait BB {
  val Put: Function1[String, Unit]
  val Get: Function0[String]
}

class JBB extends Joins {
  object Put extends SyncEvent[Unit, String]
  object Get extends NullarySyncEvent[String]
  // internal events
  object P extends AsyncEvent[String]
  object Free extends AsyncEvent[Int]
  object Full extends NullaryAsyncEvent
  join {
    case Put(s) and1 Free(c) =>
      if (c == 1) Full() else Free(c-1)
      P(s)
      Put.reply()
    case Get() and2 P(s) and2 Full() =>
      Free(1)
      Get reply s
    case Get() and3 P(s) and3 Free(c) =>
      Free(c+1)
      Get reply s
  }
}

object testJBB extends App {
  import scala.concurrent.Future
  import scala.concurrent.ExecutionContext.Implicits.global

  val buf = new JBB { this.Free(2) }
  val s = "hello"
  Future {
    buf.Put(s)
    buf.Put(s)
  }
  Future {
    buf.Get()
    buf.Get()
  }
  Thread.sleep(500)
}

class LBB(capacity: Int) extends BB {
  private val q = new Queue[String]
  def put(s: String) = synchronized {
    while (q.length == capacity) { wait() }
    q += s
    notifyAll()
  }
  def get() = this.synchronized {
    while (q.length == 0) { wait() }
    val s = q.dequeue
    notifyAll()
    s
  }
  val Put = (s: String) => put(s)
  val Get = () => get()
}
