import React from 'react';
import { Alert, Button, StyleSheet, Text } from 'react-native';
import { SafeAreaProvider, SafeAreaView } from 'react-native-safe-area-context';

import { Buffer } from 'buffer/';

import NativeHCEModule, {HCEModuleEvent} from '@icedevml/react-native-host-card-emulation/js/NativeHCEModule';

function App(): React.JSX.Element {
  React.useEffect(() => {
    NativeHCEModule?.onEvent(async (event: HCEModuleEvent) => {
      try {
        console.log('received event', event);

        switch (event.type) {
          case 'readerDetected':
            NativeHCEModule?.setSessionAlertMessage('Reader detected');
            break;

          case 'readerDeselected':
            NativeHCEModule?.setSessionAlertMessage('Lost reader');
            break;

          case 'sessionStarted':
            NativeHCEModule?.setSessionAlertMessage(
              'Tap towards the reader',
            );
            await NativeHCEModule?.startHCE();
            break;

          case 'sessionInvalidated':
            if (event.arg === 'maxSessionDurationReached') {
              Alert.alert(
                'Information',
                'Session has timed out and thus was invalidated ' +
                  'by the operating system.',
              );
            }
            break;

          case 'received':
            const capdu = Buffer.from(event.arg!, 'hex');
            // for the demo, we just respond with the reversed C-APDU and success status 9000
            const rapdu = Buffer.concat([
              Buffer.from(event.arg!, 'hex').reverse(),
              Buffer.from([0x90, 0x00]),
            ]);

            console.log('Received C-APDU: ', capdu.toString('hex'));
            console.log('Sending R-APDU: ', rapdu.toString('hex'));

            await NativeHCEModule?.respondAPDU(rapdu.toString('hex'));

            if (capdu[0] === 0xb0 && capdu[1] === 0xff) {
              // signal success if the C-APDU started with B0FF...
              NativeHCEModule?.setSessionAlertMessage(
                'Final command B0FF - OK',
              );
              await NativeHCEModule?.stopHCE('success');
              NativeHCEModule?.invalidateSession();
            } else if (capdu[0] === 0xb0 && capdu[1] === 0xee) {
              // signal failure if the C-APDU started with B0EE...
              NativeHCEModule?.setSessionAlertMessage(
                'Final command B0EE - ERROR',
              );
              await NativeHCEModule?.stopHCE('failure');
              NativeHCEModule?.invalidateSession();
            } else {
              NativeHCEModule?.setSessionAlertMessage(
                'Received ' + capdu.slice(0, 2).toString('hex') + '...',
              );
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
      await NativeHCEModule?.beginSession();
    } catch (error) {
      console.error('beginSession err', error);
      Alert.alert('Error', 'Failed to begin NFC session: ' + error);
      return;
    }
  }

  async function doAcquireExclusiveNFC() {
    try {
      await NativeHCEModule?.acquireExclusiveNFC();
    } catch (error) {
      console.error('acquireExclusiveNFC err', error);
      Alert.alert('Error', 'Failed to acquire exclusive NFC access: ' + error);
      return;
    }

    Alert.alert(
      'Information',
      'Acquired exclusive NFC access for 15 seconds. ' +
        "Operating system's services (like background tag reading) are disabled within that period.",
    );
  }

  return (
    <SafeAreaProvider>
      <SafeAreaView style={styles.safeAreaView}>
        <Text style={styles.text}>Demo App for Native HCE Module</Text>
        <Text style={styles.text}>
          GitHub: icedevml/react-native-host-card-emulation
        </Text>
        <Button title="Begin HCE session&emulation" onPress={doBeginSession} />
        <Button
          title="Acquire exclusive NFC access"
          onPress={doAcquireExclusiveNFC}
        />
      </SafeAreaView>
    </SafeAreaProvider>
  );
}

const styles = StyleSheet.create({
  safeAreaView: {
    flex: 1,
    backgroundColor: 'white',
  },
  text: {
    margin: 10,
    fontSize: 20,
    color: 'black',
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
