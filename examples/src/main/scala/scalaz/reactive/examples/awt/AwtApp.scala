//package scalaz.reactive.examples.awt
//import java.awt.event._
//import java.util.EventObject
//import java.util.concurrent.LinkedBlockingDeque
//
//import javax.swing.JFrame
//import javax.swing.SwingUtilities.invokeLater
//import scalaz.reactive.Sink.Sink
//import scalaz.reactive.{ Event, Reactive, Sink, Time }
//import scalaz.zio.{ IO, Promise, RTS }
//
//object AwtApp extends RTS {
//
//  def run(app: => AwtApp): IO[Void, Unit] = {
//
//    val io: IO[Void, IO[Void, Unit]] = for {
//      promise <- Promise.make[Void, IO[Void, Unit]]
//      _ <- IO.sync {
//            invokeLater(() => {
//              val sink = app.sinkIO
//              unsafeRun(promise.complete(sink)) // TODO: see how to pass this IO differently
//              ()
//            })
//          }.fork
//      _ = println("started ")
//      r <- promise.get
//    } yield r
//    IO.flatten(io)
//  }
//}
//abstract class AwtApp(val name: String)
//    extends JFrame
//    with KeyListener
//    with MouseListener
//    with ActionListener {
//
//  // implementing classes need to define
//  def sink: Sink[EventObject]
//
//  def eventsFilter(e: Event[EventObject]): Event[EventObject] = e
//
//  // No need to have different queues, but for testing purposes let's see how merging events works
//  private val mouseQueue = new LinkedBlockingDeque[MouseEvent]()
//  private val keyQueue   = new LinkedBlockingDeque[KeyEvent]()
//
//  private val actionQueue = new LinkedBlockingDeque[ActionEvent]()
//
//  private def keyEvent: Event[KeyEvent] =
//    Event(for {
//      t <- Time.now
//      e <- IO.sync(keyQueue.take())
//    } yield (t, Reactive(e, keyEvent)))
//
//  private def mouseEvent: Event[MouseEvent] =
//    Event(for {
//      t <- Time.now
//      e <- IO.sync(mouseQueue.take())
//    } yield (t, Reactive(e, mouseEvent)))
//
//  private def actionEvent: Event[ActionEvent] =
//    Event(for {
//      t <- Time.now
//      e <- IO.sync(actionQueue.take())
//    } yield (t, Reactive(e, actionEvent)))
//
//  private val allEvent = mouseEvent.merge(keyEvent).merge(actionEvent)
//
//  private def offerEvent[A <: EventObject](e: A, q: java.util.Queue[A]): Unit = {
//    q.add(e); ()
//  }
//
//  // key events
//  override def keyPressed(e: KeyEvent): Unit  = offerEvent(e, keyQueue)
//  override def keyReleased(e: KeyEvent): Unit = offerEvent(e, keyQueue)
//  override def keyTyped(e: KeyEvent): Unit    = offerEvent(e, keyQueue)
//
//  // mouse events
//  override def mouseClicked(e: MouseEvent): Unit =
//    offerEvent(e, mouseQueue)
//  override def mousePressed(e: MouseEvent): Unit =
//    offerEvent(e, mouseQueue)
//  override def mouseReleased(e: MouseEvent): Unit =
//    offerEvent(e, mouseQueue)
//  override def mouseEntered(e: MouseEvent): Unit =
//    offerEvent(e, mouseQueue)
//  override def mouseExited(e: MouseEvent): Unit =
//    offerEvent(e, mouseQueue)
//
//  // action events
//  def actionPerformed(e: ActionEvent): Unit = offerEvent(e, actionQueue)
//
//  def sinkIO = Sink.sinkE(sink, eventsFilter(allEvent))
//
//}
