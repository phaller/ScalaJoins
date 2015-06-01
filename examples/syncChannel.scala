/**
 * Scala Joins
 *
 * @author Philipp Haller
 */

package examples

import joins._
import events._
import scala.concurrent.ops._

object syncChannel extends Application {

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

  spawn {
    Thread.sleep(500)
    Console.println("read: "+schan.Read())
  }

  spawn {
    Thread.sleep(1000)
    Console.println("wrote: "+schan.Write(42))
  }
}
