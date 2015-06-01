/**
 * Scala Joins
 *
 * @author Philipp Haller
 */

package examples

import joins._
import events._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object testFIFO extends App {
  class FIFO extends Joins {
    object Put extends AsyncEvent[Int]
    object Get extends NullarySyncEvent[Int]
    object St1 extends AsyncEvent[Unit]
    object St2 extends AsyncEvent[Int]
    join {
      case St1(_) and1 Put(x) and1 Get() =>
        Console.println("case 1")
        Get reply x
        St2(x)
      case St2(x) and2 Get() and2 Put(y) =>
        Console.println("case 2")
        Get reply x
        St2(y)
    }
  }
  val buf = new FIFO { St1() }
  
  Future {
    buf.Put(42)
    println(buf.Get())
  }

  Thread.sleep(500)
}
