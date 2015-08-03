package citywasp.android
package app

import android.app.Activity
import android.content.ContextWrapper
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.view.View

import citywasp.api._
import com.typesafe.config._
import TypedResource._
import scala.util._

object Settings {
  final val Prefs = "CityWaspPrefs"
}

trait Common { self: ContextWrapper =>
  def credentialsSet = getSharedPreferences(Settings.Prefs, 0).contains("email")

  def configWithCredentials = {
    val settings = getSharedPreferences(Settings.Prefs, 0)
    val email = settings.getString("email", "")
    val password = settings.getString("password", "")
    ConfigFactory.parseString(s"""
      citywasp {
        email = "$email"
        password = "$password"
      }
    """).withFallback(ConfigFactory.load)
  }
}

class LoginActivity extends Activity with TypedFindView with Common {
  override def onCreate(savedInstanceState: Bundle) = {
    super.onCreate(savedInstanceState)
    if (credentialsSet) {
      val intent = new Intent(this, classOf[StatusActivity])
      startActivity(intent)
    } else {
      setContentView(R.layout.login)
    }
  }

  def saveCredentials(view: View) = {
    val settings = getSharedPreferences(Settings.Prefs, 0)
    val editor = settings.edit()
    editor.putString("email", findView(TR.email).getText.toString)
    editor.putString("password", findView(TR.password).getText.toString)
    editor.commit

    val intent = new Intent(this, classOf[StatusActivity])
    startActivity(intent)
  }
}

class StatusActivity extends Activity with TypedFindView with Common {
  override def onCreate(savedInstanceState: Bundle) = {
    super.onCreate(savedInstanceState)
    if (!credentialsSet) {
      val intent = new Intent(this, classOf[LoginActivity])
      startActivity(intent)
    } else {
      setContentView(R.layout.status)
      verifyCredentials(findView(TR.status))
    }
  }

  def verifyCredentials(status: TextView) = {
    val config = configWithCredentials.getConfig("citywasp")
    status.append(s"Logging in as ${config.getString("email")}\n")

    import scala.concurrent.ExecutionContext.Implicits.global
    implicit val cw = RemoteCityWasp(config)

    val carFuture = for {
      session <- CityWasp.session
      _ = runOnUi { status.append("Aquired valid session.\n") }
      challenge <- session.loginChallenge
      _ = runOnUi { status.append("Got login challenge.\n") }
      loggedIn <- challenge.login
      _ =  runOnUi { status.append("Successfully logged in.\n") }
      car <- loggedIn.currentCar
    } yield car

    carFuture.onComplete { res =>
      val message = res match {
        case Success(Some(c: LockedCar)) => "Found reserved car.\n"
        case Success(Some(c: UnlockedCar)) => "Found unlocked car.\n"
        case Success(None) => "No car reservation found.\n"
        case Failure(err) =>
          "Error:\n" + s"  ${err.getMessage}\n" + (if (err.getCause != null) s"  ${err.getCause.getMessage}\n" else "")
      }
      runOnUi { status.append(message) }
    }
  }

  def runOnUi(thunk: => Unit) {
    runOnUiThread(new Runnable() {
        override def run() = {
           thunk
        }
    })
  }
}
