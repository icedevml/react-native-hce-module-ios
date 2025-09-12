import React from 'react';
import {SafeAreaView, StyleSheet, Text, Button, Alert} from 'react-native';
import {Buffer} from 'buffer/';

import NativeHCEModuleIOS from '@icedevml/react-native-hce-module-ios/js/NativeHCEModuleIOS';

function App(): React.JSX.Element {
  React.useEffect(() => {
    NativeHCEModuleIOS?.onEvent(async event => {
      try {
        console.log('received event', event);

        switch (event.type) {
          case 'readerDetected':
            NativeHCEModuleIOS?.setSessionAlertMessage('Reader detected');
            break;

          case 'readerDeselected':
            NativeHCEModuleIOS?.setSessionAlertMessage('Lost reader');
            break;

          case 'sessionStarted':
            NativeHCEModuleIOS?.setSessionAlertMessage('Tap towards the reader');
            await NativeHCEModuleIOS?.startHCE();
            break;

          case 'sessionInvalidated':
            if (event.arg === "maxSessionDurationReached") {
              Alert.alert("Information", "Session has timed out and thus was invalidated " +
                "by the operating system.");
            }
            break;

          case 'received':
            const capdu = Buffer.from(event.arg!, "hex");
            // for the demo, we just respond with the reversed C-APDU and success status 9000
            const rapdu = Buffer.concat([
              Buffer.from(event.arg!, "hex").reverse(),
              Buffer.from([0x90, 0x00])
            ]);

            console.log("Received C-APDU: ", capdu.toString("hex"));
            console.log("Sending R-APDU: ", rapdu.toString("hex"));

            await NativeHCEModuleIOS?.respondAPDU(rapdu.toString("hex"));

            if (capdu[0] === 0xB0 && capdu[1] === 0xFF) {
              // signal success if the C-APDU started with B0FF...
              NativeHCEModuleIOS?.setSessionAlertMessage('Final command B0FF - OK');
              await NativeHCEModuleIOS?.stopHCE('success');
              NativeHCEModuleIOS?.invalidateSession();
            } else if (capdu[0] === 0xB0 && capdu[1] === 0xEE) {
              // signal failure if the C-APDU started with B0EE...
              NativeHCEModuleIOS?.setSessionAlertMessage('Final command B0EE - ERROR');
              await NativeHCEModuleIOS?.stopHCE('failure');
              NativeHCEModuleIOS?.invalidateSession();
            } else {
              NativeHCEModuleIOS?.setSessionAlertMessage('Received ' + capdu.slice(0, 2).toString("hex") + '...');
            }
            break;
        }
      } catch (err) {
        console.error('error in event handler', err);
      }
    });
  }, []);

  async function doBeginSession() {
    try {
      await NativeHCEModuleIOS?.beginSession();
    } catch (error) {
      console.error('beginSession err', error);
      Alert.alert('Error', 'Failed to begin NFC session: ' + error)
      return;
    }
  }

  async function doAcquireExclusiveNFC() {
    try {
      await NativeHCEModuleIOS?.acquireExclusiveNFC();
    } catch (error) {
      console.error('acquireExclusiveNFC err', error);
      Alert.alert('Error', 'Failed to acquire exclusive NFC access: ' + error);
      return
    }

    Alert.alert('Information', 'Acquired exclusive NFC access for 15 seconds. ' +
        'Operating system\'s services (like background tag reading) are disabled within that period.');
  }

  return (
    <SafeAreaView style={{ flex: 1 }}>
      <Text style={styles.text}>Demo App for HCEModuleIOS</Text>
      <Text style={styles.text}>GitHub: icedevml/react-native-hce-module-ios</Text>
      <Button title="Begin HCE session&emulation" onPress={doBeginSession} />
      <Button title="Acquire exclusive NFC access" onPress={doAcquireExclusiveNFC} />
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  text: {
    margin: 10,
    fontSize: 20,
  },
  textInput: {
    margin: 10,
    height: 40,
    borderColor: 'black',
    borderWidth: 1,
    paddingLeft: 5,
    paddingRight: 5,
    borderRadius: 5,
  },
});

export default App;
