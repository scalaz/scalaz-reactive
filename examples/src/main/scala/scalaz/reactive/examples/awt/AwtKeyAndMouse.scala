//package scalaz.reactive.examples.awt
//
//import java.awt.event._
//import java.awt.{ BorderLayout, Dimension }
//import java.util.EventObject
//
//import javax.swing.{ JButton, JScrollPane, JTextArea, _ }
//import scalaz.reactive.Event
//import scalaz.reactive.Sink.Sink
//import scalaz.zio.{ IO, _ }
//
//object KeyboardAndTimeApp extends App with RTS {
//
//  def run(args: List[String]): IO[Nothing, ExitStatus] =
//    myAppLogic.attempt.map(_.fold(_ => 1, _ => 0)).map(ExitStatus.ExitNow(_))
//
//  def myAppLogic =
//    AwtApp.run(new KeyboardAndTimeApp("AWT Events Demo"))
//}
//
//class KeyboardAndTimeApp(name: String) extends AwtApp(name) {
//
//  setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
//  val displayArea = new JTextArea
//  buildPane()
//
//  private def buildPane(): Unit = {
//
//    displayArea.setText("Click the mouse or press a lowercase vowel key")
//    val button = new JButton("Clear")
//    button.addActionListener(this)
//    displayArea.addKeyListener(this)
//    displayArea.setEditable(false)
//    val scrollPane = new JScrollPane(displayArea)
//    scrollPane.setPreferredSize(new Dimension(375, 125))
//    getContentPane.add(scrollPane, BorderLayout.CENTER)
//    getContentPane.add(button, BorderLayout.PAGE_END)
//    pack()
//    displayArea.addMouseListener(this)
//    setVisible(true)
//  }
//
//  val sink: Sink[EventObject] = (e: EventObject) =>
//    e match {
//      case e: MouseEvent if e.getID == MouseEvent.MOUSE_CLICKED =>
//        IO.sync(displayArea.append(s"\nClicked at [${e.getX},${e.getY}]"))
//      case e: KeyEvent =>
//        IO.sync(displayArea.append(s"\nKey [${e.getKeyChar}] ${e.getID}]"))
//      case _: ActionEvent =>
//        IO.sync {
//          displayArea.setText("")
//        }
//      case _ => IO.sync(println(s"Ignoring $e"))
//    }
//
//  override def eventsFilter(e: Event[EventObject]): Event[EventObject] =
//    e.filter(
//      e =>
//        !e.isInstanceOf[KeyEvent] || Set('a', 'e', 'i', 'o', 'u')
//          .contains(e.asInstanceOf[KeyEvent].getKeyChar)
//    )
//
//}
