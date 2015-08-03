package citywasp.android
package app

import android.widget.Toast

import citywasp.api._
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.wearable._

import java.util.concurrent.TimeUnit
import scala.concurrent.{ Await, ExecutionContext, Future }
import scala.concurrent.duration._
import scala.util._

object ListenerService {
  final val Timeout = 10.seconds
  case class NodeId(id: String)
}

class ListenerService extends WearableListenerService with Common {
  import ListenerService._

  var googleApiClient: GoogleApiClient = _

  override def onCreate() = {
    super.onCreate()
    googleApiClient = getGoogleApiClient
    googleApiClient.connect
  }

  override def onMessageReceived(messageEvent: MessageEvent) = {
    import scala.concurrent.ExecutionContext.Implicits.global
    implicit val nodeId = NodeId(messageEvent.getSourceNodeId)
    showToast(messageEvent.getPath)
    messageEvent.getPath match {
      case "/status" => if (!credentialsSet) reply("/status/no-credentials") else {
        val carFuture = for {
          loggedIn <- login
          car <- loggedIn.currentCar
        } yield car
        announceCurrentCar(carFuture)
      }
      case "/action/unlock-car" =>
        val carFuture = for {
          loggedIn <- login
          Some(lockedCar: LockedCar) <- loggedIn.currentCar
          _ <- lockedCar.unlock
          car <- loggedIn.currentCar
        } yield car
        announceCurrentCar(carFuture)
      case "/action/lock-car" =>
        val carFuture = for {
          loggedIn <- login
          Some(unlockedCar: UnlockedCar) <- loggedIn.currentCar
          _ <- unlockedCar.lock
          car <- loggedIn.currentCar
        } yield car
        announceCurrentCar(carFuture)
    }
  }

  def getGoogleApiClient = new GoogleApiClient.Builder(getApplicationContext)
    .addApi(Wearable.API)
    .build()

  def reply(message: String)(implicit nodeId: NodeId) = {
    Wearable.MessageApi.sendMessage(googleApiClient, nodeId.id, message, Array())
  }

  def showToast(message: String) = {
    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
  }

  def login(implicit ec: ExecutionContext) = {
    implicit val cw = RemoteCityWasp(configWithCredentials.getConfig("citywasp"))
    for {
      session <- CityWasp.session
      challenge <- session.loginChallenge
      loggedIn <- challenge.login
    } yield loggedIn
  }

  def announceCurrentCar(car: Future[Option[Car]])(implicit nodeId: NodeId, ec: ExecutionContext) =
    Try(Await.result(car, Timeout)) match {
      case Success(Some(c: LockedCar)) => reply("/status/car-locked")
      case Success(Some(c: UnlockedCar)) => reply("/status/car-unlocked")
      case Success(None) => reply("/status/no-car")
      case _ => reply("/status/unknown")
    }

}
