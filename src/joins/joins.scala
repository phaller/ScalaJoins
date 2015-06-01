/**
 * Join patterns in Scala using extractor patterns.
 *
 * NEW:
 * Check for non-linearity.
 *
 * Simplified synchronous events. We now always enqueue
 * a new `SyncVar' that might get resolved when the body
 * of the join is run. This lead to another simplification
 * in the `reply' method.
 *
 * OTHERS:
 * - Support for multiple synchronous messages in patterns.
 * Inside the body of the pattern, one replies to a synchronous
 * message `Msg' by calling its `reply' method,
 * as in `Msg reply result'.
 *
 * - Return types of synchronous messages are checked.
 * However, we do not enforce that all synchronous senders are
 * replied to when a join pattern triggers.
 *
 * @author Philipp Haller
 * @date   2008/01/04
 *
 */

package joins

import scala.concurrent.{SyncVar, Lock}
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.mutable.{Queue, BitSet, Set}

case class JoinAttrs(joins: JoinsBase, dry: Boolean)

object and1 {
  def unapply(attrs: Any) = attrs match {
    case JoinAttrs(joins, dry) =>
      joins.notifyJoinOp(dry)
      Some((attrs, attrs))
    case _ => None
  }
}

object and2 {
  def unapply(attrs: Any) = attrs match {
    case JoinAttrs(joins, dry) =>
      joins.notifyJoinOp(dry)
      Some((attrs, attrs))
    case _ => None
  }
}

object and3 {
  def unapply(attrs: Any) = attrs match {
    case JoinAttrs(joins, dry) =>
      joins.notifyJoinOp(dry)
      Some((attrs, attrs))
    case _ => None
  }
}

object and4 {
  def unapply(attrs: Any) = attrs match {
    case JoinAttrs(joins, dry) =>
      joins.notifyJoinOp(dry)
      Some((attrs, attrs))
    case _ => None
  }
}

trait JoinsBase {
  private var count = 0
  def freshTag = synchronized { count += 1; count }

  val tags: Set[Int] = new BitSet(8)

  var seenEvt = false

  var currentCase = 1
  var matchingCase = 0

  var currentEvent = 0
  var lastEvent = -1

  val lock = new Lock

  def notify(tag: Int, dry: Boolean) {
    seenEvt = true
    if (dry) {
      assert(!tags.contains(tag), "join pattern non-linear")
      //println("adding "+tag+" to set "+tags)
      tags += tag
    } else {
      currentEvent += 1
      //println("current event ("+tag+"): "+currentEvent)
      //if (currentEvent == lastEvent) println("LAST EVENT")
    }
  }

  def notifyJoinOp(dry: Boolean) {
    //println("JOIN, current set: "+tags+", dry: "+dry)
    if (seenEvt) { // starting a new case
      seenEvt = false
      if (dry) tags.clear()
      else currentEvent = 0
      currentCase += 1
      //println("starting next case "+currentCase)
    }
  }

  def matches(tag: Int): Boolean = {
    val answer = (tags contains tag) && (currentCase == matchingCase)
    if (answer) tags -= tag // TODO: remove?
    answer
  }

  def isLastEvent = currentEvent == lastEvent

  def beforeMatch() {
    //println("starting new match")
    tags.clear()
    currentCase = 1
    seenEvt = false
  }

  def afterMatch() {
    matchingCase = currentCase
    lastEvent = tags.size
  }

  def beforeRun() {
    currentCase = 1
    currentEvent = 0
    seenEvt = false
  }
}

abstract class Event[R] {
  protected val owner: Joins
  private[joins] val tag = owner.freshTag
  //println(this+" has tag: "+tag)
  def invoke(): R
}

trait SyncEventBase[R] extends Event[R] {
  protected val waitQ = new Queue[SyncVar[R]]

  def invoke(): R = {
    val res = new SyncVar[R]
    waitQ += res
    owner.matchAndRun()
    res.get
  }

  def reply(res: R): Unit = {
    owner.lock.acquire
    waitQ.dequeue set res
    owner.lock.release
  }
}

abstract class NullaryEvent[R] extends Event[R] with Function0[R] {
  private var cnt = 0

  def apply(): R = {
    //println("acquiring owner's lock")
    owner.lock.acquire
    //println("invoke event "+tag)
    cnt += 1
    invoke()
  }

  def unapply(attrs: Any): Boolean = attrs match {
    case JoinAttrs(_, dry) =>
      owner.notify(tag, dry)
      if (dry && cnt > 0) {
        //println("MATCH: "+tag)
        //println("invocation count: "+cnt)
        true
      } else if (!dry && owner.matches(tag)) {
        //println("MATCH: "+tag)
        cnt -= 1
        if (owner.isLastEvent) {
          //println("releasing owner's lock")
          owner.lock.release
        }
        true
      } else {
        //println("NO MATCH: "+tag)
        false
      }
    case _ => false
  }
}

abstract class NonNullaryEvent[R, Arg] extends Event[R] with Function1[Arg, R] {
  private var buf: List[Arg] = Nil

  def apply(arg: Arg): R = {
    //println("acquiring owner's lock")
    owner.lock.acquire
    //println("invoke event "+tag+" with arg "+arg)
    buf = buf ::: List(arg)
    invoke()
  }

  def unapply(attrs: Any): Option[Arg] = attrs match {
    case JoinAttrs(_, dry) =>
      owner.notify(tag, dry)
      if (dry && !buf.isEmpty) {
        //println("MATCH: "+tag+" ("+buf.head+")")
        Some(buf.head)
      }
      else if (!dry && owner.matches(tag)) {
        //println("MATCH: "+tag+" ("+buf.head+")")
        val arg = buf.head
        buf = buf.tail
        if (owner.isLastEvent) {
          //println("releasing owner's lock")
          owner.lock.release
        }
        Some(arg)
      } else {
        //println("NO MATCH: "+tag)
        None
      }
    case _ => None
  }
}

trait Joins extends JoinsBase {
  implicit val joinsOwner = this

  private var joinSet: PartialFunction[Any, Any] = _
  def join(joinSet: PartialFunction[Any, Any]) {
    this.joinSet = joinSet
  }

  def matchAndRun(): Unit = {
    beforeMatch()
    if (joinSet.isDefinedAt(JoinAttrs(this, true))) {
      //println("successful match")
      afterMatch()
      beforeRun()
      joinSet(JoinAttrs(this, false))
    } else {
      //println("match failed")
      //println("releasing owner's lock")
      lock.release
    }
  }
}

object events {
  class AsyncEvent[Arg](implicit joins: Joins) extends {
    val owner = joins
  } with NonNullaryEvent[Unit, Arg] {
    def invoke(): Unit = owner.matchAndRun()
  }

  class NullaryAsyncEvent(implicit joins: Joins) extends {
    val owner = joins
  } with NullaryEvent[Unit] {
    def invoke(): Unit = owner.matchAndRun()
  }

  class SyncEvent[R, Arg](implicit joins: Joins) extends {
    val owner = joins
  } with NonNullaryEvent[R, Arg] with SyncEventBase[R]

  class NullarySyncEvent[R](implicit joins: Joins) extends {
    val owner = joins
  } with NullaryEvent[R] with SyncEventBase[R]

  class Async2[A1, A2](implicit joins: Joins) extends AsyncEvent[(A1, A2)] {
    def apply(a1: A1, a2: A2) = super.apply((a1, a2))
  }

  class JoinMany[Arg](evts: AsyncEvent[Arg]*) {
    def unapplySeq(scrut: Any): Option[Seq[Arg]] = {
      val matched = evts.map(_.unapply(scrut))
      if (matched.exists(_.isEmpty)) None
      else Some(matched map { x => (x: @unchecked) match {
        case Some(arg) => arg
      } })
    }
  }
}

object joinsTest extends App {
  import events._

  class Buffer extends Joins {
    object Put extends AsyncEvent[Int]
    object Getty extends SyncEvent[Int, Int]

    object Put2 extends AsyncEvent[String]
    object Get1 extends SyncEvent[String, String]

    join {
      case Put(y) and1 Getty(z) =>
        Getty reply y
      case Put2(y) and2 Get1(z) =>
        Get1 reply z+y
    }
  }

  val buffer = new Buffer

  Future {
    Thread.sleep(1000)
    buffer.Put(4)

    Thread.sleep(1000)
    buffer.Put2("world")
  }

  Future {
    Console.println("get1: "+buffer.Get1("hello"))
  }

  Future {
    Thread.sleep(2000)
    Console.println("getty: "+buffer.Getty(2))
  }

  Thread.sleep(2500)
}

/*
object joinsTest extends Application {
  import events._

  /*
   * Simple buffer example.
   */
  class Buffer extends Joins {
    object Put extends AsyncEvent[Int]
    object Get extends NullarySyncEvent[Int]
    object Getty extends SyncEvent[Int, Int]

    object Put2 extends Async2[Int, String]
    object Get1 extends SyncEvent[String, String]

    join {
      case Get() and1 Put(y) and1 Getty(z) =>
        Getty reply y
        Get reply y+z
      case Get1(z) and2 Put2(y, "world") =>
        Get1 reply z+y
    }
  }

  val buffer = new Buffer

  val prod = spawn {
    Thread.sleep(1000)
    buffer.Put(4)

    Thread.sleep(1000)
    buffer.Put2(5, "world")
  }

  val cons = spawn {
    Console.println("got: "+buffer.Get())

    Console.println("got: "+buffer.Get1("hello"))
  }

  val cons2 = spawn {
    Thread.sleep(2000)
    Console.println("getty: "+buffer.Getty(2))
  }
}
*/
