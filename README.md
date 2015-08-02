# Scala Joins

Author: Philipp Haller

## Summary

Join patterns are an attractive declarative way to synchronize both threads
and asynchronous distributed computations. This library integrates joins into
Scala based on extensible pattern matching provided by *extractors*. Scala
Joins supports join patterns with multiple synchronous events, and guards.
Joins are also integrated with actors. It enables the use of join patterns in
the context of more advanced synchronization modes, such as future-type
message sending and token-passing continuations.

## Releases

- Release [v0.5](https://github.com/phaller/ScalaJoins/releases/tag/v0.5) (for Scala 2.11)
- Release [v0.4](https://github.com/phaller/ScalaJoins/releases/tag/v0.4) (original release)

## Paper

The main ideas of the implementation are explained in the following paper:

Philipp Haller and Tom Van Cutsem. Implementing joins using extensible
pattern matching. In *COORDINATION*, pages 135--152. Springer, 2008.
[[EPFL](http://infoscience.epfl.ch/record/125992)]
[[Springer](http://link.springer.com/chapter/10.1007/978-3-540-68265-3_9)]

## Build and Test

The Scala Joins library can be compiled and run using Scala 2.11.x.

    $ tar xzf ScalaJoins-0.5.tar.gz
    $ cd ScalaJoins-0.5
    $ mkdir classes
    $ scalac -d classes src/joins/*.scala

The main `joins.scala` file contains a runnable test:

    $ scala -cp classes joins.joinsTest
    getty: 4
    get1: helloworld

The synchronous channel example is compiled and run as follows:

    $ scalac -d classes examples/syncChannel.scala
    $ scala -cp classes examples.syncChannel
    wrote: ()
    read: 42

The FIFO example is compiled and run as follows:

    $ scalac -d classes examples/FIFO.scala
    $ scala -cp classes examples.testFIFO
    case 1
    42
