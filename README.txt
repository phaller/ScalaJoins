Scala Joins -- README
=====================

Version: 0.4
Author: Philipp Haller
Date: 2008/01/04

This preview of a Scala Joins library can be compiled and run with
Scala version 2.6.1-final. Here is a sample session:

$ tar xzf ScalaJoins-0.4.tar.gz
$ cd ScalaJoins-0.4
$ mkdir classes
$ scalac -d classes src/joins/*.scala

The main `joins.scala` file already contains a test that can be run as follows:
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

