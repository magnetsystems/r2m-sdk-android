# rest2mobile SDK for Android, version 1.1.0

This repository contains the binary and source code of rest2mobile SDK for Android. This SDK is a library of classes that support rest2mobile code generation, REST requests, and JSON handling. The library support the following functions: 
- Connecting asynchronously to the REST server
- Building the REST request URL and HTTP headers
- Marshalling and unmarshalling JSON data
- Enforcing type safety by converting JSON data

If you're using rest2mobile with Android Studio or IntelliJ IDEA using Gradle, you don't have to download the SDK. Gradle allows you to specify the SDK as a Maven repository in your build file. Using the SDK with Android Studio or IntelliJ IDEA is described in the topic [Set Up Android Studio](https://developer.magnet.com/android/).

If you're using rest2mobile with Eclipse or Ant, download the zip file containing the SDK by clicking **Download ZIP** on the right side of the code view for this repository. Next, follow the instructions: 
* [Set Up Eclipse](https://github.com/magnetsystems/rest2mobile/wiki/rest2mobile-setup-eclipse) 
* [Set Up Ant](https://github.com/magnetsystems/rest2mobile/wiki/rest2mobile-setup-ant) 

Detailed documentation for all of rest2mobile is available on the [Magnet rest2mobile wiki]
(https://github.com/magnetsystems/rest2mobile/wiki) site.

Javadoc of the SDK is available [here](https://magnetsystems.github.io/r2m-sdk-android/reference/com/magnet/android/mms/MagnetMobileClient.html).

## License

Licensed under the **[Apache License, Version 2.0] [license]** (the "License");
you may not use this software except in compliance with the License.

## About the sdk source code
The sdk uses a regular Gradle Android project. You will find the source file in:
magnet-library/api/src/main
 
The unit tests in:
magnet-library/api/androidTest
 
The distribution (compressed zip file) in:
magnet-library/api/build/distributions
 
To compile the sdk, (after cloning it to your computer) simply execute the following:
<pre>
$ cd magnet-library
$ gradle build
</pre>

## Copyright

Copyright © 2014 Magnet Systems, Inc. All rights reserved.
[license]: http://www.apache.org/licenses/LICENSE-2.0
