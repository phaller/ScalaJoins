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

object syncChannel extends App {

  class SyncChannel extends Joins {
    object Read extends NullarySyncEvent[Int]
    object Write extends SyncEvent[Unit, Int]
    
    join {
      case Read() and1 Write(x) =>
        Read reply x
        Write reply {}
    }
  }

  val schan = new SyncChannel

  Future {
    Thread.sleep(500)
    Console.println("read: "+schan.Read())
  }

  Future {
    Thread.sleep(1000)
    Console.println("wrote: "+schan.Write(42))
  }

  Thread.sleep(1500)
}
