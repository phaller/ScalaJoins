/**
 * Join patterns in Scala using extractor patterns.
 *
 * NEW:
 * Some parts of multi-threaded implementation are reused,
 * e.g. object <code>&amp;</code> and trait <code>JoinsBase</code>.
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
 * @date   2007/09/06
 *
 */

package joins

import scala.actors.{Actor, OutputChannel}
import scala.actors.Actor._

object joinsActor extends App {

  class JoinPatterns[R](f: PartialFunction[Any, R], joins: JoinsBase)
  extends PartialFunction[Any, R] {
    def asJoinMessage(msg: Any, isDryRun: Boolean) =
      if (msg.isInstanceOf[ArgCont])
        JoinAttrs(joins, isDryRun)
      else
        msg

    override def isDefinedAt(msg: Any) = {
      joins.beforeMatch()
      val isDef = f.isDefinedAt(asJoinMessage(msg, true))
      joins.afterMatch()
      isDef
    }

    override def apply(msg: Any) = {
      joins.beforeRun()
      f(asJoinMessage(msg, false))
    }
  }

  trait JoinActor extends Actor with JoinsBase {
    implicit val joinsOwner = this

    override def send(msg: Any, replyTo: OutputChannel[Any]) {
      enqueueReplyDestination(msg, replyTo)
      super.send(msg, replyTo)
    }

    override def receive[R](f: PartialFunction[Any, R]): R =
      super.receive(new JoinPatterns(f, this))

    override def react(f: PartialFunction[Any, Unit]): Nothing =
      super.react(new JoinPatterns(f, this))

    def enqueueReplyDestination(msg: Any, replyTo: OutputChannel[Any]) {
      if (msg.isInstanceOf[ArgCont])
        msg.asInstanceOf[ArgCont].enqueue(replyTo)
    }
  }

  abstract class JoinMessage {
    protected val owner: JoinsBase
    protected val tag = owner.freshTag

    type argType
    var buf: List[(argType, OutputChannel[Any])] = Nil

    // refers to the last dequeued reply destination
    var replyDest: OutputChannel[Any] = null

    def enqueue(arg: argType, replyTo: OutputChannel[Any]): Unit = synchronized {
      buf = buf ::: List((arg, replyTo))
    }

    def reply(res: Any) { replyDest ! res }

    def check(attrs: Any): Option[argType] = attrs match {
      case JoinAttrs(joins, dry) =>
        joins.notify(tag, dry)
        if (dry && !buf.isEmpty) {
          //println("buffer non-empty: "+buf.head)
          Some(buf.head._1)
        }
        else if (!dry && joins.matches(tag)) {
          val arg = buf.head._1
	  // keep a reference to the reply destination such that it is still
     	  // accessible when performing a reply in the join pattern body
	  replyDest = buf.head._2
          buf = buf.tail
          Some(arg)
        } else None
      case _ => None
    }
  }

  class Join(implicit joins: JoinsBase) extends {
    val owner = joins
  } with JoinMessage {
    type argType = Unit

    def apply(): ArgCont = {
      //println("invoke event "+tag)
      val mySelf = this
      new ArgCont {
        type argContType = Unit
        val proto = mySelf
        val arg = {}
      }
    }

    // pattern matcher requires Boolean as return type
    // for nullary unapply instead of Option[Unit]
    def unapply(scrut: Any) = !check(scrut).isEmpty
  }

  abstract class NonNullaryJoinMessage extends JoinMessage {
    def unapply(scrut: Any): Option[argType] = check(scrut)
  }

  abstract class ArgCont {
    type argContType
    val proto: JoinMessage { type argType = argContType }
    val arg: argContType

    def enqueue(replyTo: OutputChannel[Any]) = {
      proto.enqueue(arg, replyTo)
    }
  }

  class Join1[Arg](implicit joins: JoinsBase) extends {
    val owner = joins
  } with NonNullaryJoinMessage {
    type argType = Arg
    def apply(arg: Arg) = {
      val mySelf = this
      val myArg = arg
      new ArgCont {
        type argContType = argType
        val proto = mySelf
        val arg = myArg
      }
    }
  }

  class Join2[A1, A2](implicit joins: JoinsBase) extends {
    val owner = joins
  } with NonNullaryJoinMessage {
    type argType = (A1, A2)
    def apply(a1: A1, a2: A2): ArgCont = {
      val mySelf = this
      val myArg = (a1, a2)
      new ArgCont {
        type argContType = argType
        val proto = mySelf
        val arg = myArg
      }
    }
  }

  /*
   * Simple buffer example.
   */
  class Buffer extends JoinActor {
    object Put extends Join1[Int]
    object Get extends Join
    object Put2 extends Join2[Int, String]
    object Get1 extends Join1[String]
    object PutGT4 extends Join1[Int]

    def act() {
      receive {
	case Put(y) and1 Get() =>
          Get reply y
	case Some(x) => println(x)
      }
    }
  }

  val buffer = new Buffer; buffer.start()

  val prod = actor {
    //Thread.sleep(500)
    //buffer ! Some(42)

    Thread.sleep(1000)
    buffer ! buffer.Put(3)
    //Thread.sleep(1000)
    //buffer ! Put(5)

    /*Thread.sleep(1000)
    buffer ! buffer.Put2(3, "world")
    Thread.sleep(1000)
    buffer ! buffer.Put2(7, "monde")
    Thread.sleep(1000)
    buffer ! buffer.Put2(5, "world")*/
  }

  val cons = actor {
    Thread.sleep(500)
    Console.println("got: "+(buffer !? buffer.Get()))
    //Console.println("got: "+(buffer !? Get()))
    //Console.println("got: "+(buffer !? buffer.Get1("hello")))
  }

}
