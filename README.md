# BWS-Android

## About

The **BioID Web Service** (BWS) is a cloud-based online service providing a powerful multimodal biometric technology with liveness detection to application developers.
But often developers have some trouble writing a user interface for collecting the data required to perform the biometric tasks, notably face images.
Therefore we want to provide some sample code that might be used in Android to interact with the BWS.

Please also take a look at the [Developer Documentation][developer].

[BioID’s liveness detection][liveness] is a software-based security feature for facial biometrics. Also called presentation attack detection (PAD), it distinguishes live persons from fakes such as photo/video replays or masks.

[<img src="https://img.youtube.com/vi/e5lP2Fja3Ow/maxresdefault.jpg" width="50%">](https://youtu.be/e5lP2Fja3Ow)

# Before you start developing a BioID app - you must have the following credentials
- You need a [BioID Account][bioidaccountregister] with a **confirmed** email address.
- After creating the BioID Account you can request a free [trial instance][trial] for the BioID Web Service (BWS).
- After the confirmation for access to a trial instance you can login to the [BWS Portal][bwsportal].
- The BWS Portal shows you the activity for your installation and allows you to configure your test client.
- After login to the BWS Portal configure your test client. In order to access this client, please do the steps below.
- Click on 'Show client keys' on your clients (the 'key' icon on the right). The dialog 'Classic keys' opens.
- Now create a new classic key (WEB API key) for your client implementation by clicking the '+' symbol.
- You will need the _AppId_ and _AppSecret_ for your client implementation. 
> :warning: _Please note that we only store a hash of the secret i.e the secret value cannot be reconstructed! So you should copy the value of the secret immediately!_





Click on your BWS client on symbol „Update Classic client“ (the ‘pencil’ icon on the right). A dialog opens that contains all information for accessing the BWS, as well as other information needed for the user BCIDs.

For the creation of BCIDs for users in your app the following information is needed:

- Storage e.g. `bws`
- Partition e.g. `12`
- UserID – this is a unique number you assign to the user, e.g. `4711`


The BCID with the example values from above is `bws.12.4711`.
Take a look at Web API endpoint for e.g. `https://bws.bioid.com`. In this case the BWS instance name is `bws`.

Now you can add the following properties to your users `gradle.properties` file.

  * `BioIdBwsInstanceName`
  * `BioIdAppId`
  * `BioIdAppSecret`
  * `BioIdBcid`

*If you need more information about the `gradle.properties` file read [Chapter 12][gradleproperties] of the official gradle docs.*

## Integration

If you want to integrate this code into your Android app follow these steps:

  1. setup your `app/build.gradle`
    1. make sure that your `minSdkVersion` is `23` (Android 6.0) or higher
    2. make sure you enabled [Data Binding][databinding]
    3. make sure you enabled [Support Vector Drawables](https://android-developers.googleblog.com/2016/02/android-support-library-232.html)
    4. add all `buildConfigField` entries from the `android/productFlavors/bws` section within `app/build.gradle` to your project
       (the actual values are provided by your users `gradle.properties` file, have a look at the **Setup** section)
    5. add all `compile` dependencies from `app/build.gradle` to your project
  2. setup your `AndroidManifest.xml`
    1. add all `uses-feature` entries from `app/src/main/AndroidManifest.xml` to your project
    2. add all `uses-permission` entries from `app/src/main/AndroidManifest.xml` to your project
    3. add these two activity declarations to your project

        ```
        <activity android:name="com.bioid.authenticator.facialrecognition.verification.VerificationActivity"/>
        <activity android:name="com.bioid.authenticator.facialrecognition.enrollment.EnrollmentActivity"/>
        ```

  3. copy all Java source files from `app/src/main/java` and `app/src/bws/java/com/bioid/authenticator/base` to your project (do not modify any package names)
  4. copy or merge all Android resources from `app/src/main/res` to your project (of course you do not have to insert the mipmap app icons)
  5. adjust all imports of `com.bioid.authenticator.BuildConfig`, `com.bioid.authenticator.R` and `import com.bioid.authenticator.databinding.*` in the copied Java sources to your package name
  6. have a look at `app/src/bws/java/com/bioid/authenticator/main/MainActivity.java` on how to start the verification or enrollment process

## Flavors

In case you are wondering why the code is split into the **main** and **bws** source set, this is because of the closed source **connect** flavor.
The **connect** flavor uses BioID Connect as identity management.
You can try out this [facial recognition app][bioid] - available via [Play Store][playstore].


[bioid]: https://www.bioid.com/facial-recognition-app/ "BioID Facial Recognition App"
[playstore]: https://play.google.com/store/apps/details?id=com.bioid.authenticator "BioID Android App"
[bioidaccountregister]: https://account.bioid.com/Account/Register "Register a BioID account" 
[trial]: https://bwsportal.bioid.com/register "Register for a trial instance"
[bwsportal]: https://bwsportal.bioid.com "BWS Portal"
[developer]: https://developer.bioid.com "Developer Documentation"
[gradleproperties]: https://docs.gradle.org/current/userguide/build_environment.html "Gradle properties"
[databinding]: https://developer.android.com/topic/libraries/data-binding/ "Data Binding"
[vectordrawables]: https://android-developers.googleblog.com/2016/02/android-support-library-232.html "Vector Drawables"
[liveness]: https://www.bioid.com/liveness-detection/ "liveness detection"
