# Host Card Emulation for iOS Platform (React Native Turbo Module)

> [!IMPORTANT]  
> As per Apple's policy, **Host Card Emulation feature is only available for users based in the European
> Economic Area (EEA)** with an iPhone running iOS 17.4 or later. Applications require a special HCE entitlement
> in order to use this feature, which is managed by Apple. Read more in the official support article:
> "[HCE-based contactless NFC transactions for apps in the European Economic Area (EEA)](https://developer.apple.com/support/hce-transactions-in-apps/)".

> [!NOTE]  
> Kindly please reference this project in your README if you are willing to create a derivative library.

## Contents of this repository

* `packages/rtn-hce-module-ios` - React Native Turbo Module (library)
* `packages/demo-hce-module-ios-app` - Demo React Native Application (module consumer)

## Prerequisites

1. Ensure that you are enrolled as organization (not individual) in Apple Developer Program, and that you are established in the European Economic Area (EEA).
2. Ensure that you meet other [non-technical requirements imposed by Apple](https://developer.apple.com/support/hce-transactions-in-apps/#requirements-and-availability).
3. Find out the Bundle ID of your existing application or [register a new Bundle ID](https://developer.apple.com/account/resources/identifiers/bundleId/add/bundle).
4. [Request the HCE Entitlement](https://developer.apple.com/contact/request/hce-entitlement/) for your application's bundle ID.
5. Wait until the entitlement is approved by Apple.

## Installing the Demo Application

1. Clone this repository.
2. Run `yarn` in the repository root in order to install all dependencies.
3. Navigate to `packages/demo-hce-module-ios-app/ios` subdirectory and run `bundle install && bundle exec pod install`.
4. Open `packages/demo-hce-modules-ios-app/ios/DemoHCEModuleIOSApp.xcworkspace` with XCode.
5. In "Signing & Capabilities" set up the proper signing team and bundle identifier.
6. Add Host Card Emulation (HCE) capability either through XCode (by clicking "+ Capability") or manually in the `DemoHCEModuleIOSApp.entitlements` file:
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
7. Launch the application through XCode.

## Module API

See [package documentation](https://github.com/icedevml/react-native-hce-module-ios/tree/master/packages/react-native-hce-module-ios) for API and usage documentation.
