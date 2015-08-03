package citywasp.android
package wearable

import android.app.Activity
import android.content.Context
import android.support.wearable.view.DelayedConfirmationView
import android.support.wearable.view.DelayedConfirmationView.DelayedConfirmationListener
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import android.view.View
import android.view.View.OnClickListener

import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.{ GoogleApiClient, Result, ResultCallback }
import com.google.android.gms.common.api.GoogleApiClient.{ ConnectionCallbacks, OnConnectionFailedListener }
import com.google.android.gms.wearable.MessageApi.MessageListener
import com.google.android.gms.wearable.{ MessageEvent, Node, Wearable }
import com.google.android.gms.wearable.NodeApi.GetConnectedNodesResult

import java.util.concurrent.TimeUnit

import scala.concurrent.ExecutionContext
import scala.collection.JavaConversions._
import scala.concurrent.Future
import scala.util._
import TypedResource._

object WearableActivity {
  final val BlockTimeoutMs = 100
  final val LoadingTimeoutMs = 10000

  implicit def function1ToOnClickListener(f: Function1[View, Unit]) = new OnClickListener {
    override def onClick(view: View) = f.apply(view)
  }

  implicit def function0ToRunnable(f: Function0[Unit]) = new Runnable {
    override def run() = f()
  }

  implicit def function1ToResultCallback[T <: Result](f: Function1[T, Unit]) = new ResultCallback[T] {
    override def onResult(result: T) = f(result)
  }
}

class WearableActivity extends Activity with TypedFindView with MessageListener with ConnectionCallbacks with OnConnectionFailedListener {
  import WearableActivity._

  var googleApiClient: GoogleApiClient = _

  override def onCreate(savedInstanceState: Bundle) = {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.status)

    findView(TR.delayed_confirmation).setTotalTimeMs(LoadingTimeoutMs)
    findView(TR.delayed_confirmation).start()

    googleApiClient = getGoogleApiClient
  }

  override def onResume() = {
      super.onResume()
      if (!googleApiClient.isConnected) googleApiClient.connect
  }

  override def onPause() = {
      super.onPause()
      Wearable.MessageApi.removeListener(googleApiClient, this)
      if (googleApiClient.isConnected) googleApiClient.disconnect
  }

  override def onConnected(connectionHint: Bundle) = {
    Wearable.MessageApi.addListener(googleApiClient, this)
    sendMessageToAllCompanions("/status")
  }

  override def onConnectionSuspended(cause: Int) = {
  }

  override def onConnectionFailed(result: ConnectionResult) = {
    Wearable.MessageApi.removeListener(googleApiClient, this)
    runOnUiThread { () =>
      findView(TR.delayed_confirmation).reset()
      findView(TR.status).setText("Disconnected")
    }
  }

  override def onMessageReceived(messageEvent: MessageEvent) = {
    val (message, listener) = messageEvent.getPath match {
      case "/status/no-credentials" => ("Set credentials in the App", null)
      case "/status/no-car" => ("No car reserved", null)
      case "/status/car-locked" => ("Touch to unlock", new DelayedConfirmationListener{
        override def onTimerSelected(v: View) = {
          val view = v.asInstanceOf[DelayedConfirmationView]
          view.setPressed(true)
          view.setListener(null)
          view.start()
          sendMessageToAllCompanions("/action/unlock-car")
        }
        override def onTimerFinished(v: View) = {
        }
      })
      case "/status/car-unlocked" => ("Touch to lock", new DelayedConfirmationListener{
        override def onTimerSelected(v: View) = {
          val view = v.asInstanceOf[DelayedConfirmationView]
          view.setPressed(true)
          view.setListener(null)
          view.start()
          sendMessageToAllCompanions("/action/lock-car")
        }
        override def onTimerFinished(v: View) = {
        }
      })
      case "/status/unknown" => ("Unable to get data", null)
    }
    runOnUiThread { () =>
      findView(TR.delayed_confirmation).reset()
      findView(TR.delayed_confirmation).setListener(listener)
      findView(TR.status).setText(message)
    }
  }

  def getGoogleApiClient = new GoogleApiClient.Builder(this)
    .addApi(Wearable.API)
    .addConnectionCallbacks(this)
    .addOnConnectionFailedListener(this)
    .build()

  def sendMessageToAllCompanions(message: String) =
    Wearable.NodeApi.getConnectedNodes(googleApiClient).setResultCallback { result: GetConnectedNodesResult =>
      for (node <- result.getNodes) {
        Wearable.MessageApi.sendMessage(googleApiClient, node.getId, message, Array())
      }
    }

  def showToast(message: String) = {
    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
  }
}
