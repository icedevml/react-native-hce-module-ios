# React Native Demo Application: NDEF NFC Type 4 Tag Emulator (for iOS/Android)

Demo React Native application using `@icedevml/react-native-host-card-emulation` in order to emulate NFC Forum Type 4 Tag with an example URL NDEF.

> [!NOTE]
> Make sure you have completed the [React Native's Set Up Your Environment](https://reactnative.dev/docs/set-up-your-environment) guide before proceeding.

> [!NOTE]
> For iOS, you will first need to [request the HCE entitlement](https://developer.apple.com/contact/request/hce-entitlement/) for using ISO7816 AID `D2760000850101` (i.e. NFC Forum Type 4 Tag - NDEF) before you can run this app.

## Prerequisites
1. Clone the repository.
2. Run `yarn` to install dependencies.
3. (iOS) Install Pods: `cd ios/ && bundle install && bundle exec pod install`. Then, open the project in XCode through the `ios/<project name>.xcworkspace` file.
4. (iOS) Make sure to set up your Signing Team and Bundle Identifier in "Signing & Capabilities" settings section of the project.
5. (iOS) Ensure that Host Card Emulation (HCE) entitlement with the ISO7816 AID `D2760000850101` is added to the app.

## Launching the app
### Android
1. Start Metro dev server:
   ```sh
   yarn start
   ```
2. Build and run the app:
   ```
   npx react-native run-android
   ```

### iOS
1. Start Metro dev server:
    ```sh
    yarn start
    ```
2. (iOS) Press the "Run" button in XCode.
