# rest2mobile SDK for Android

This repository contains the rest2mobile SDK for Android. This SDK is a library of classes that support rest2mobile code generation, REST requests, and JSON handling. The library support the following functions: 
- Connecting asynchronously to the REST server
- Building the REST request URL and HTTP headers
- Marshalling and unmarshalling JSON data
- Enforcing type safety by converting JSON data

If you're using rest2mobile with Android Studio or IntelliJ IDEA using Gradle, you don't have to download the SDK. Gradle allows you to specify the SDK as a Maven repository in your build file. Using the SDK with Android Studio or IntelliJ IDEA is described in the topic [Set Up Android Studio](https://developer.magnet.com/android/).

If you're using rest2mobile with Eclipse or Ant, download the zip file containing the SDK by clicking **Download ZIP** on the right side of the code view for this repository. Next, follow the instructions in the topic 
[Set Up Eclipse or Ant](https://github.com/magnetsystems/rest2mobile/wiki/rest2mobile-setup-eclipse-ant) in the Magnet rest2mobile wiki.

Detailed documentation for all of rest2mobile is available on the [Magnet rest2mobile wiki]
(https://github.com/magnetsystems/rest2mobile/wiki) site.

Javadoc of the SDK is available [here](https://magnetsystems.github.io/r2m-sdk-android/reference/com/magnet/android/mms/MagnetMobileClient.html).

Version 1.0 of the rest2moble Android SDK is licensed under the terms of the [Magnet Software License Agreement](http://www.magnet.com/resources/tos.html). See the [LICENSE](https://github.com/magnetsystems/magnet-sdk-android/blob/master/LICENSE) file for full details.

Note: The coming release of rest2mobile Android SDK will be available under an Apache v2 license.

### Known Issues
During "gradle build", the Lint Error "InvalidPackage: Package not included in Android" may appear.
In order to work around it, you may include the following code snippet in app/build.gradle of your Android Studio project.

<pre>
android {
    lintOptions {
        abortOnError false
    }
}
</pre>

Another [workaround available online](http://stackoverflow.com/questions/16184109/using-twitter4j-in-android-getting-lint-error-in-library-invalid-package-refe).
