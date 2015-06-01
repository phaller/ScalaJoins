/**
 * Scala Joins
 *
 * @author Philipp Haller
 */

package examples

object boundedBufferBench {
  def main(args: Array[String]) {
    val num = Integer.parseInt(args(0))
    val times = Integer.parseInt(args(1))

    val lbuf = new LBB(100)
    val jbuf = new JBB { Free(100) }
    
    println("testing lock-based implementation...")
    val lstart = System.currentTimeMillis
    var threads = List[Thread]()    
    for (val i <- List.range(0, num)) {
      val lprod = new LProducer(lbuf, times)
      val lcons = new LConsumer(lbuf, times)
      threads = lprod :: lcons :: threads
      lprod.start
      lcons.start
    }
    threads foreach { t => t.join() }
    val lend = System.currentTimeMillis
    println("time: "+(lend-lstart))

    println("testing join-based implementation...")
    val jstart = System.currentTimeMillis
    var jthreads = List[Thread]()    
    for (val i <- List.range(0, num)) {
      val jprod = new JProducer(jbuf, times)
      val jcons = new JConsumer(jbuf, times)
      jthreads = jprod :: jcons :: jthreads
      jprod.start
      jcons.start
    }
    jthreads foreach { t => t.join() }
    val jend = System.currentTimeMillis
    println("time: "+(jend-jstart))
  }

  class LProducer(b: LBB, cnt: Int) extends Thread {
    val s = "hello"
    override def run() {
      for (val _ <- List.range(0, cnt)) {
        b.Put(s)
      }
    }
  }

  class LConsumer(b: LBB, cnt: Int) extends Thread {
    override def run() {
      for (val _ <- List.range(0, cnt)) {
        b.Get()
      }
    }
  }

  class JProducer(b: JBB, cnt: Int) extends Thread {
    val s = "hello"
    override def run() {
      for (val _ <- List.range(0, cnt)) {
        b.Put(s)
      }
    }
  }

  class JConsumer(b: JBB, cnt: Int) extends Thread {
    override def run() {
      for (val _ <- List.range(0, cnt)) {
        b.Get()
      }
    }
  }
}
