# Package: React Native HCE Module

Host Card Emulation Module for React Native on iOS and Android platforms.

> [!IMPORTANT]  
> As per Apple's policy, **Host Card Emulation feature is only available for users based in the European
> Economic Area (EEA)** with an iPhone running iOS 17.4 or later. Applications require a special HCE entitlement
> in order to use this feature, which is managed by Apple. Read more in the official support article:
> "[HCE-based contactless NFC transactions for apps in the European Economic Area (EEA)](https://developer.apple.com/support/hce-transactions-in-apps/)".

## Prerequisites
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
   +  <uses-feature android:name="android.hardware.nfc.hce" android:required="true" />

      ...

      <application
        ...
        >

        ...
   
   +    <service
   +      android:name="com.itsecrnd.rtnhceandroid.HCEService"
   +      android:exported="true"
   +      android:enabled="true"
   +      android:permission="android.permission.BIND_NFC_SERVICE" >
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

* [module API (declarations)](https://github.com/icedevml/react-native-host-card-emulation/blob/master/packages/native-hce-module/js/NativeHCEModule.ts)
* [demo app's code (example usage)](https://github.com/icedevml/react-native-host-card-emulation/blob/master/packages/demo-hce-module-app/App.tsx)
