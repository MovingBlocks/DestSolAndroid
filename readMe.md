## Destination Sol Android Facade

This project provides the Android front-end for https://github.com/MovingBlocks/DestinationSol and is meant to be used from within that project, not independently. See its repo for basic setup instructions!

## Using the Android Facade

You can build the Android version of the game by either using the command line tools or using IntelliJ IDEA / Android Studio. Any modules that you have will be included.

### Using the command line

- Download the [Android SDK](https://developer.android.com/studio#downloads) and use the `sdkmanager` tool to install the API 28 build tools.
- In the root Destination Sol project directory, run the command `echo sdk.dir=<SDK_ROOT> > local.properties`, where `<SDK_ROOT>` is the root directory in which
  you installed the Android SDK, to create a new local properties file.
- To fetch the Android facade, in the same root project directory run the command `gradlew fetchAndroid`
  The command should complete successfully.
- Afterwards, run the command `gradlew android:assembleDebug` to build the APK. This command should also complete successfully.
- Once the command has completed, use the Android emulator or connect a suitable device and ensure that it can be detected by the `adb` tool.
  This can be verified by running the command `adb devices`, which should show the device name in its output.
- Then, run the command `gradlew android:installDebug` to install the APK to the first connected device.
- A new "Destination Sol" app should now be visible on the test device. Run it and play the game.
  It should not crash with any errors or exceptions.
  
### Using IntelliJ IDEA (or Android Studio)

- Use the Android plugin (if not installed, then install it) to install the API 28 SDK build tools and platform tools.
- In the root Destination Sol project directory, run the command `gradlew fetchAndroid`. The command should complete successfully.
- Create a new build configuration based on the "Android App" template. You should set the start-up module to the "android" module.
- In the `File->Project Structure->SDKs` menu, add a new SDK and set the build target to `Android API 28` and the Java SDK to `Java 1.8`.
- In the `File->Project Structure->Modules` menu, select the `android` module and open the `Dependencies` tab.
  Set the module SDK to the new SDK you just added.
- Set the current build configuration to the newly created one and run the project using the debug button.
- A new "Destination Sol" app should now be visible on the test device. Run it and play the game.
  It should not crash with any errors or exceptions.
