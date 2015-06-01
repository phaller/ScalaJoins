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

object dynamicJoins extends App {

  class DynamicBuffer extends Joins {
    object Put1 extends AsyncEvent[Int]
    object Put2 extends AsyncEvent[Int]
    object Get extends NullarySyncEvent[Int]

    object ManyPuts extends JoinMany(Put1, Put2)

    join {
      case ManyPuts(a, b) and1 Get() =>
        Get reply (a+b)
    }
  }

  val dyn = new DynamicBuffer

  Future {
    Thread.sleep(500)
    Console.println("Get: "+dyn.Get())
  }

  Future {
    Thread.sleep(500)
    Console.println("Put1: "+dyn.Put1(40))
    Thread.sleep(500)
    Console.println("Put2: "+dyn.Put2(2))
  }

  Thread.sleep(1500)
}
