# BWS-Android

## About

The **BioID Web Service** (BWS) is a cloud-based online service providing a powerful multimodal biometric technology with liveness detection to application developers.
But often developers have some trouble writing a user interface for collecting the data required to perform the biometric tasks, notably face images.
Therefore we want to provide some sample code that might be used in Android to interact with the BWS.

## Setup

To successfully run this sample app, you need to have access to an existing BWS installation.
If you don't have this access you can [register for a trial instance](https://bwsportal.bioid.com/register).

After you have access to the BioID Web Service (BWS) you can continue to create and configure your client app.

### Create and configure your client app on bwsportal.bioid.com
After you are logged in to the portal, select your client and go to the 'Configuration' section. 
The 'Client configuration' contains all information for accessing the BWS, as well as other information needed for the user BCIDs.

For the creation of BCIDs for users in your app the following information is needed:

- Storage e.g. `bws`
- Partition e.g. `12`
- UserID – this is a unique number you assign to the user, e.g. `4711`


The BCID with the example values from above is `bws.12.4711`.
Take a look at Web API endpoint for e.g. `https://bws.bioid.com`. In this case the BWS instance name is `bws`.

Click on the 'Web API keys' the add button. In the dialog window the app identifier and app secret is shown.


Now you can add the following properties to your users `gradle.properties` file.

  * `BioIdBwsInstanceName`
  * `BioIdAppId`
  * `BioIdAppSecret`
  * `BioIdBcid`

*If you need more information about the `gradle.properties` file read [Chapter 12](https://docs.gradle.org/current/userguide/build_environment.html) of the official gradle docs.*

## Integration

If you want to integrate this code into your Android app follow these steps:

  1. setup your `app/build.gradle`
    1. make sure that your `minSdkVersion` is `23` (Android 6.0) or higher
    2. make sure you enabled [Data Binding](https://developer.android.com/topic/libraries/data-binding/index.html)
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
You can try out this [facial recognition app](https://www.bioid.com/facial-recognition-app) - available via [Play Store](https://play.google.com/store/apps/details?id=com.bioid.authenticator).
