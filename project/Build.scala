import sbt._
import sbt.Keys._
import android.Keys._
import com.typesafe.sbt.SbtGit._
import com.typesafe.sbt.GitVersioning

object Build extends android.AutoBuild {

  object WearableSupport {
    object Keys {
      val wearableAppGen = taskKey[Seq[File]]("Wearable application generator")
      val wearableAppDescGen = taskKey[Seq[File]]("Wearable application descriptor generator")
    }

    import Keys._

    lazy val settings: Seq[sbt.Def.Setting[_]] = Seq(
      wearableAppGen := {
        val wearableApk = (packageRelease in (wearable, Android)).value
        val destination = (projectLayout in Android).value.bin / "resources" / "res" / "raw" / "wearable_release.apk"
        IO.copyFile(wearableApk, destination)
        Seq(destination)
      },
      wearableAppDescGen := {
        val file = (projectLayout in Android).value.bin / "resources" / "res" / "xml" / "wearable_app_desc.xml"
        val contents = s"""|<wearableApp package="citywasp.android">
                           |  <versionCode>${(versionCode in Android).value.get}</versionCode>
                           |  <versionName>${(versionName in Android).value.get}</versionName>
                           |  <rawPathResId>wearable_release</rawPathResId>
                           |</wearableApp>
                           |""".stripMargin
        IO.write(file, contents)
        Seq(file)
      },
      resourceGenerators in Compile <+= wearableAppGen,
      resourceGenerators in Compile <+= wearableAppDescGen
    )
  }

  lazy val common: Seq[sbt.Def.Setting[_]] = Seq(
    versionName in Android := Some(version.value),
    versionCode in Android := {
      val df = new java.text.SimpleDateFormat("yyyyMMdd'T'HHmmss")
      Some((df.parse(git.formattedDateVersion.value).getTime / 1000).toInt)
    }
  )

  lazy val application = project.settings(common ++ WearableSupport.settings)
  lazy val wearable = project.settings(common)

  lazy val root = Project(id = "citywasp-android", base = file("."))
    .settings(git.useGitDescribe := true)
    .aggregate(application, wearable)
    .enablePlugins(GitVersioning)
}
