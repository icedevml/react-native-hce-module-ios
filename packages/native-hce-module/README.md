# Package: React Native HCE Module

Host Card Emulation Module for React Native on iOS and Android platforms.

> [!IMPORTANT]  
> As per Apple's policy, **Host Card Emulation feature is only available for users based in the European
> Economic Area (EEA)** with an iPhone running iOS 17.4 or later. Applications require a special HCE entitlement
> in order to use this feature, which is managed by Apple. Read more in the official support article:
> "[HCE-based contactless NFC transactions for apps in the European Economic Area (EEA)](https://developer.apple.com/support/hce-transactions-in-apps/)".

## Prerequisites
This package was designed for React Native [New Architecture](https://reactnative.dev/architecture/landing-page) only.
Make sure that your project has it enabled. Note that the New Architecture is enabled by default for React Native 0.76+ projects.

### iOS
1. Ensure that you are enrolled as organization (not individual) in Apple Developer Program, and that you are established in the European Economic Area (EEA).
2. Ensure that you meet other [non-technical requirements imposed by Apple](https://developer.apple.com/support/hce-transactions-in-apps/#requirements-and-availability).
3. Find out the Bundle ID of your existing application or [register a new Bundle ID](https://developer.apple.com/account/resources/identifiers/bundleId/add/bundle).
4. [Request the HCE Entitlement](https://developer.apple.com/contact/request/hce-entitlement/) for your application's bundle ID.
5. Wait until the entitlement is approved by Apple.

### Android
*(no prerequisite steps are required for Android)*

## Installation
Install `@icedevml/react-native-host-card-emulation` package within your React Native project. Then, follow the subsections below for each platform that you need to support.

### iOS
1. Make sure to set up a proper signing team and bundle identifier in "Signing & Capabilities" configuration section of your project.
2. Add Host Card Emulation (HCE) entitlement with desired AIDs. This could be done either through XCode (by clicking "+ Capability") or within the entitlements XML file directly. Example:
    ```xml
    <?xml version="1.0" encoding="UTF-8"?>
    <!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
    <plist version="1.0">
    <dict>
        <key>com.apple.developer.nfc.hce</key>
        <true/>
        <key>com.apple.developer.nfc.hce.iso7816.select-identifier-prefixes</key>
        <array>
            <string>F001020304</string>
            <string>F00102030405</string>
            <string>F0010203040506</string>
        </array>
    </dict>
    </plist>
    ```
   Replace `F001020304`, `F00102030405`, `F0010203040506` with the ISO7816 AIDs that you want to support.
3. In the `ios/` app's subdirectory, run `bundle install && bundle exec pod install`.

### Android
1. In `android/app/src/main/AndroidManifest.xml` add:
   ```diff
    <manifest xmlns:android="http://schemas.android.com/apk/res/android">
      ...

   +  <uses-permission android:name="android.permission.NFC" />
   +  <uses-feature android:name="android.hardware.nfc.hce" android:required="false" />
   +  <permission android:name="com.itsecrnd.rtnhceandroid.HCE_BROADCAST" android:protectionLevel="signature" />
   +  <uses-permission android:name="com.itsecrnd.rtnhceandroid.HCE_BROADCAST"/>

      ...

      <application
        ...
        >

        ...
   
   +    <service
   +      android:name="com.itsecrnd.rtnhceandroid.service.HCEService"
   +      android:exported="true"
   +      android:enabled="true"
   +      android:permission="android.permission.BIND_NFC_SERVICE">
   +      <intent-filter>
   +        <action android:name="android.nfc.cardemulation.action.HOST_APDU_SERVICE" />
   +        <category android:name="android.intent.category.DEFAULT"/>
   +      </intent-filter>
   +
   +      <meta-data
   +        android:name="android.nfc.cardemulation.host_apdu_service"
   +        android:resource="@xml/aid_list" />
   +    </service>
      </application>
    </manifest>
   ```
2. Create file `android/app/src/main/res/xml/aid_list.xml`:
   ```xml
   <host-apdu-service xmlns:android="http://schemas.android.com/apk/res/android"
     android:description="@string/app_name"
     android:requireDeviceUnlock="false">
     <aid-group android:category="other"
       android:description="@string/app_name">
       <aid-filter android:name="F001020304" />
       <aid-filter android:name="F00102030405" />
       <aid-filter android:name="F0010203040506" />
     </aid-group>
   </host-apdu-service>
   ```
   Replace `F001020304`, `F00102030405`, `F0010203040506` with the ISO7816 AIDs that you want to support.

## API Specification & Demo App

This module provides a uniform low-level HCE API for both mobile platforms.

1. Subscribe to the event stream (there is a single global event stream for the entire module):
   ```typescript
   NativeHCEModule?.onEvent(async (event: HCEModuleEvent) => {
       switch (event.type) {
           /* ... your handler here ... */
       }
   });
   ```
2. When user indicates that he wants to perform the HCE action, call the following function from the button's onClick routine:
   ```typescript
   await NativeHCEModule?.beginSession();
   ```
   This will emit `sessionStarted` event on both platforms. You can start the HCE straight from that event:
   ```typescript
   // inside NativeHCEModule?.onEvent handler's switch
   case 'sessionStarted':
       NativeHCEModule?.setSessionAlertMessage('Tap towards the reader');  // only for iOS, no-op in Android
       await NativeHCEModule?.startHCE();
       break;
   ```
3. Calling `startHCE()` causes:
   * iOS: Launch of the NFC scan system UI. The smartphone will start listening for C-APDUs from the readers right after that UI appears on screen. The `readerDetected` event will be emitted as soon as the NFC tag field presence is observed. The `readerDeselected` event will be emitted if the reader is physically disconnected or a non-matching AID is selected by the reader.
   * Android: No user interface appears (you have to implement it on your own scanning UI, within your app). The `readerDetected` event will be emitted as soon as the first matching SELECT AID command is observed. The `readerDeselected` event will be emitted if the reader is physically disconnected or a non-matching AID is selected by the reader.
4. Optionally, you can handle `readerDetected` and `readerDeselected events to enhance user's experience.
   ```typescript
   // inside NativeHCEModule?.onEvent handler's switch
   case 'readerDetected':
       NativeHCEModule?.setSessionAlertMessage('Reader detected');  // only for iOS, no-op in Android
       break;

    case 'readerDeselected':
        NativeHCEModule?.setSessionAlertMessage('Lost reader');  // only for iOS, no-op in Android
        break;
   ```
5. Handle CAPDU
   ```typescript
   // inside NativeHCEModule?.onEvent handler's switch
   case 'received':
       NativeHCEModule?.setSessionAlertMessage('Keep holding the tag');

       const capdu = Buffer.from(event.arg!, 'hex');
       console.log('Received C-APDU, capdu.toString('hex'));
   
       // let's say we always want to respond with [0x0A] + Status Word 0x9000
       await NativeHCEModule?.respondAPDU(Buffer.from([0x0A, 0x90, 0x00], "hex"));
       break;
   ```
6. (iOS) Optionally, if you need to utilize `NFCPresentmentIntentAssertion` then at any time you can call:
   ```typescript
   NativeHCEModule?.acquireExclusiveNFC();
   ```
   Which will throw an exception if the presentment intent assertion was already acquired and is still valid 
   (usually 15 seconds) or if the cooldown period for acquiring new assertion was not yet expired (usually also 15 seconds).

> [!NOTE]
> See [Demo App's Code (Example Library Usage)](https://github.com/icedevml/react-native-host-card-emulation/blob/master/packages/demo-hce-module-app/App.tsx) example for more insignt about the library's API.

> [!NOTE]
> Raw native module's API specification is available in [packages/native-hce-module/js/NativeHCEModule.ts](https://github.com/icedevml/react-native-host-card-emulation/blob/master/packages/native-hce-module/js/NativeHCEModule.ts).
> Check it out in order to understand what methods you can call against the module and what are the expected parameters/return values.
