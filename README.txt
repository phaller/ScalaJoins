Scala Joins -- README
=====================

Version: 0.5
Author: Philipp Haller

This Scala Joins library can be compiled and run using Scala 2.11.x.

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
