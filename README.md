# scalaz-reactive

[![Gitter](https://badges.gitter.im/scalaz/scalaz-reactive.svg)](https://gitter.im/scalaz/scalaz-reactive?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)


# Goal

A high-performance, purely-functional library for reactive programming based on efficient incremental computation.

# Introduction

This library aims at faithfully implementing Functional Reactive Programming as defined in [2].
The term _Reactive proramming_
is often used to describe composing streams of discrete events and and handling them with callbacks (by subscribing). That is
different from Functional reactive programming (FRP). FRP is about composing dynamic values changing in continuous time,
and reacting to discrete events.

# Core concepts

`Behavior` - data changing with time. Time is continous, some discretization will occur when consumed at specific moments in time.
Example: the amount of water flowing out of a bucket with a constant rate will be `L = max(W0 - r * t, 0)`
`Event` - stream of (time, event) pairs. Example: Add one liter of water at moment `t`.
Signals may switched by events (e.g adding water will change the signal to `L = max(W0 + X - r * t, 0)`
`Reactive values` change in time and are composed from signals and events going through a signal network.


# Background

* _Functional Reactive Programming_ by Stephen Blackheath and Anthony Jones, Manning Publications
* Push-Pull Functional Reactive Programming [paper](http://conal.net/papers/push-pull-frp/) by Conal Elliot
* Haskell [Functional Reactive Programming](https://wiki.haskell.org/FRP)
* [Overview](https://www.slant.co/topics/2349/~functional-reactive-programming-frp-libraries-for-haskell) of Haskell FRP libraries
* [frp-zoo](https://github.com/gelisam/frp-zoo): Several implementation of a simple program with different FRP libraries
* [Controlling time and space: understanding the many formulations of FRP](https://www.youtube.com/watch?v=Agu6jipKfYw) presentation based on ELM
