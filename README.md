# scalaz-reactive

[![Gitter](https://badges.gitter.im/scalaz/scalaz-reactive.svg)](https://gitter.im/scalaz/scalaz-reactive?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

## Goal

A high-performance, purely-functional library for reactive programming based on efficient incremental computation.

## Introduction

This library aims at faithfully implementing Functional Reactive Programming as defined in [2].
The term _Reactive programming_
is often used to describe composing streams of discrete events. Functional reactive programming (FRP) is about
composing dynamic values changing in continuous time and reacting to discrete events.

## Core concepts

`Behaviour[A](value: Reactive[TimeFun[A]])` - value changing over time.

`Event[+A](value: Future[Reactive[A]])` - stream of (Time, a) pairs.

`Reactive[+A](head: A, tail: Event[A])` - reactive value.

`Sink[A, B](f: A => IO[Void, Unit])` - consumer of reactive values.

## Example

This project is just starting, so the working example is quite simple:

```scala
case class Tick(name: String)

  def ticks(interval: Duration, name: String): Event[Tick] =
    Event(IO.point { (Time.now, Reactive(Tick(name), ticks(interval, name).delay(interval))) })

  def myAppLogic: IO[Void, Unit] =
    Sink[Tick, Unit](t => IO.now(println(s"tick ${t.name}")))
      .sink(
        ticks(0.2 second, "a")
          .merge(ticks(0.4 second, "b"))
      )
```

This program produces a `scalaz.zio.IO` that can be run by e.g. `scalaz.zio.App` - see `TwoTickers.scala` in `examples`.

## Background

* _Functional Reactive Programming_ by Stephen Blackheath and Anthony Jones, Manning Publications
* Push-Pull Functional Reactive Programming [paper](http://conal.net/papers/push-pull-frp/) by Conal Elliot
* Haskell [Functional Reactive Programming](https://wiki.haskell.org/FRP)
* [Overview](https://www.slant.co/topics/2349/~functional-reactive-programming-frp-libraries-for-haskell) of Haskell FRP libraries
* [frp-zoo](https://github.com/gelisam/frp-zoo): Several implementation of a simple program with different FRP libraries
* [Controlling time and space: understanding the many formulations of FRP](https://www.youtube.com/watch?v=Agu6jipKfYw) presentation based on ELM
