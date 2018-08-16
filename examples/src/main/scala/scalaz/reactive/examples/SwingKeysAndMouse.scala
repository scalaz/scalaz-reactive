package scalaz.reactive.examples

import java.awt.event._

import javax.swing._
import javax.swing.JButton
import javax.swing.JScrollPane
import java.awt.BorderLayout
import java.awt.Dimension
import java.io.IOException

import javax.swing.JTextArea
import javax.swing.JTextField
import javax.swing.SwingUtilities._
import scalaz.reactive.Future.Future
import scalaz.reactive.{Reactive, Time}
import scalaz.zio.{App, IO, Queue}
import scalaz.zio.console._

import scala.concurrent.duration._
import scala.language.postfixOps

object KeyboardAndTime extends App {

  def run(args: List[String]): IO[Nothing, ExitStatus] =
    myAppLogic.attempt.map(_.fold(_ => 1, _ => 0)).map(ExitStatus.ExitNow(_))

  def myAppLogic: IO[IOException, Unit] =
    for {
      _ <- IO.point {
        invokeLater(() => {
          new KeyEventDemo("KeyEventDemo")
          ()
        })
      }
      - <- putStrLn("Started")
      - <- IO.sleep(10 days) // Swing onExit exits the program
      - <- putStrLn("Finished")
    } yield ()

}

class KeyEventDemo(val name: String)
    extends JFrame
    with KeyListener
    with MouseListener
    with ActionListener {

  setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
  //Set up the content pane.
  //Display the window.

  val displayArea = new JTextArea
  val typingArea = new JTextField(20)
  addComponentsToPane
  pack
  displayArea.addMouseListener(this)
  setVisible(true)
  def addComponentsToPane(): Unit = {
    val button = new JButton("Clear")
    button.addActionListener(this)

    typingArea.addKeyListener(this)

    displayArea.setEditable(false)
    val scrollPane = new JScrollPane(displayArea)
    scrollPane.setPreferredSize(new Dimension(375, 125))
    getContentPane.add(typingArea, BorderLayout.PAGE_START)
    getContentPane.add(scrollPane, BorderLayout.CENTER)
    getContentPane.add(button, BorderLayout.PAGE_END)
  }

  def keyTyped(e: KeyEvent): Unit = ()

//  def keyPressed(e: KeyEvent) = displayArea.append(e.getKeyChar.toString)
  val keyEvents: IO[Nothing, Queue[KeyEvent]] = Queue.unbounded[KeyEvent]
  def keyPressed(e: KeyEvent) = () // keyEvents.map { q => {q.offer(e)}}

  def keyReleased(e: KeyEvent): Unit = ()

  val futureEvent: Future[KeyEvent] =
    keyEvents.flatMap(_.take).map { e => (Time(), e)
    }
  val eventKey = scalaz.reactive.Event(futureReactive)
  def futureReactive: Future[Reactive[KeyEvent]] = ???

  /** Handle the button click. */
  def actionPerformed(e: ActionEvent): Unit = { //Clear the text components.
    displayArea.setText("")
    typingArea.setText("")
    //Return the focus to the typing area.
    typingArea.requestFocusInWindow
    ()
  }
  override def mouseClicked(e: MouseEvent): Unit = ()
  override def mousePressed(e: MouseEvent): Unit =
    displayArea.append(s"[${e.getX},${e.getY}]")
  override def mouseReleased(e: MouseEvent): Unit = ()
  override def mouseEntered(e: MouseEvent): Unit = ()
  override def mouseExited(e: MouseEvent): Unit = ()
}
