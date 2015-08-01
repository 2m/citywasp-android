## Developing

Setup keystore and key configuration for signing both sub-projects by adding

    key.alias: <KEY_ALIAS>
    key.alias.password: <KEY_PASSWORD>
    key.store: <KEYSTORE_PATH>
    key.store.password: <KEYSTORE_PASSWORD>

to `application/local.properties` and `wearable.properties` files. Working with signed releases is prefered, because only signed application APKs can contain wearable APK which will be installed automatically.

Make sure you start `sbt` with the path to Android SDK set:

    ANDROID_HOME=/path/to/android-sdk sbt

To create application APK with wearable APK included run `application/android:packageRelease`.

To install application and wearable APKs run `application/android:install`.
