import React from 'react';
import { Alert, Button, StyleSheet, Text } from 'react-native';
import { SafeAreaProvider, SafeAreaView } from 'react-native-safe-area-context';

import { Buffer } from 'buffer/';

import NativeHCEModule, {HCEModuleEvent} from '@icedevml/react-native-host-card-emulation/js/NativeHCEModule';
import { createNDEFApp } from './ndefApp.ts';

function App(): React.JSX.Element {
  const [recentEventsList, setRecentEventsList] = React.useState<string[]>([]);
  const ndefApp = React.useMemo(() => createNDEFApp(), []);

  React.useEffect(() => {
    NativeHCEModule?.onEvent(async (event: HCEModuleEvent) => {
      try {
        console.log('received event', event);
        setRecentEventsList((prevState) => {
          const data = [...prevState, event.type + " " + event.arg];
          return data.slice(-15);
        });

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
            await NativeHCEModule?.respondAPDU(null, ndefApp(capdu).toString("hex"));
            break;
        }
      } catch (err) {
        console.error('error in event handler', err);
      }
    });
  }, [ndefApp]);

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

  let recentEvents = recentEventsList.join('\n');

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
        <Text style={styles.text}>{recentEvents}</Text>
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
