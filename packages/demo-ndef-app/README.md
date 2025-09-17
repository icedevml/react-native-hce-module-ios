# React Native Demo Application: NDEF Emulator App (for iOS/Android)

Demo React Native application using `@icedevml/react-native-host-card-emulation` in order to emulate NFC Forum Type 4 Tag with an example URL NDEF.

> [!NOTE]
> Make sure you have completed the [Set Up Your Environment](https://reactnative.dev/docs/set-up-your-environment) guide before proceeding.

## Prerequisites
1. Clone the repository.
2. Run `yarn` to install dependencies.
3. (iOS) Make sure to set up your Signing Team and Bundle Identifier in "Signing & Capabilities" settings section of the project.
4. (iOS) Ensure that Host Card Emulation (HCE) entitlement is added to the project with proper ISO7816 AIDs enabled.

## Launching the app
1. Start Metro dev server:
    ```sh
    yarn start
    ```
2. (iOS) Install Pods:
    ```sh
    cd ios/
    bundle install
    bundle exec pod install
    ```
3. (iOS) Build and run application:
    ```sh
    yarn ios
    ```
   (Android) Build and run application:
    ```sh
    yarn android
    ```
