# scalaz-reactive

[![Gitter](https://badges.gitter.im/scalaz/scalaz-reactive.svg)](https://gitter.im/scalaz/scalaz-reactive?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)


# Goal

A high-performance, purely-functional library for reactive programming based on efficient incremental computation.

# Introduction

The FRP library that can be used to simplify applications that deal with values changing in time, both independently nd as result of external events. The library should be good
 to build an arcade game, a robot, or a monitoring and alerting application. Performance should be achieved by laziness and incremental computation.
 
 - Push-pull model (does that cover incremental computation?)
 - Purely functional
 -

# Core concepts
 

`Signal` - continous data changing with time. Time is continous, some discretization will occur when consumed at some moments in time. Example: the amount
of water flowing out of a bucket with a constant rate will be `L = max(W0 - r * t, 0)`
`Event` - values produced in time. Example: Add one liter of water.
Signals may switched by events (e.g adding water will chnage the signal to `L = max(W0 + X - r * t, 0)`
`Reactive values` change in time and are composed from signals and events going through a signal network.

# Design choices

Push (data driven) - all events propagated through the network - ineficient
Pull (demand driven) -  values calculated when requested. May cause latency. May lead to duplicate calculations of data that ahs not changed.
Push/pull - external events are pushed to all consumers so that eventual effect are processed.

Arrowized FRP (like Yampa) : arrows as main mean to build flows - ???

Interaction with the world: ZIO

Scala.js and DOM manupulation are out of scope for this project.

# Competition

scala.rx
 - purely functional []


Monix
 - purely functional [x]



# Background
* Haskell [Functional Reactive Programming](https://wiki.haskell.org/FRP)
* Conal Elliot's [Denotational Semantics](http://conal.net/papers/push-pull-frp/) paper
* [Overview](https://www.slant.co/topics/2349/~functional-reactive-programming-frp-libraries-for-haskell) of Haskell FRP libraries
* [frp-zoo](https://github.com/gelisam/frp-zoo): Several implementation of a simple program with different FRP libraries
* Controlling time and space: understanding the many formulations of FRP: [presentaton](https://www.youtube.com/watch?v=Agu6jipKfYw)
* A Scala FRP library [Sodium](https://github.com/SodiumFRP/sodium/tree/master/scala)