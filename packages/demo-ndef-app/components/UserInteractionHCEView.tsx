import React from 'react';
import { Alert, Button, StyleSheet, Text } from 'react-native';

import { Buffer } from 'buffer/';

import NativeHCEModule, {
  HCEModuleEvent,
} from '@icedevml/react-native-host-card-emulation/js/NativeHCEModule';
import { createNDEFType4TagApp } from '../ndefType4TagApp.ts';

interface IProps {
  setCurrentView: (view: string) => void;
}

function UserInteractionHCEView({ setCurrentView }: IProps): React.JSX.Element {
  const [recentEventsList, setRecentEventsList] = React.useState<string[]>([]);
  const [getNDEFAppState, resetNDEFAppState, handleCAPDU] = React.useMemo(
    () => createNDEFType4TagApp(),
    [],
  );

  React.useEffect(() => {
    const subscription = NativeHCEModule.onEvent(
      async (event: HCEModuleEvent) => {
        try {
          console.log('received event', event);
          setRecentEventsList(prevState => {
            const data = [...prevState, event.type + ' ' + event.arg];
            return data.slice(-15);
          });

          switch (event.type) {
            case 'readerDetected':
              resetNDEFAppState();
              NativeHCEModule.setSessionAlertMessage('Reader detected');
              break;

            case 'readerDeselected':
              if (getNDEFAppState().wasNDEFRead) {
                await NativeHCEModule.stopHCE('success');
              } else {
                NativeHCEModule.setSessionAlertMessage('Lost reader');
              }
              break;

            case 'sessionStarted':
              NativeHCEModule.setSessionAlertMessage('Tap towards the reader');
              await NativeHCEModule.startHCE();
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
              await NativeHCEModule.respondAPDU(
                null,
                handleCAPDU(capdu).toString('hex'),
              );

              if (getNDEFAppState().wasNDEFRead) {
                NativeHCEModule.setSessionAlertMessage(
                  'Successfully emulated NDEF!',
                );
              }
              break;
          }
        } catch (err) {
          console.error('error in event handler', err);
        }
      },
    );

    return () => {
      subscription.remove();
    };
  }, [getNDEFAppState, resetNDEFAppState, handleCAPDU]);

  async function doBeginSession() {
    try {
      await NativeHCEModule.beginSession();
    } catch (error) {
      console.error('beginSession err', error);
      Alert.alert('Error', 'Failed to begin NFC session: ' + error);
      return;
    }
  }

  let recentEvents = recentEventsList.join('\n');

  return (
    <>
      <Button
        title="Back to main menu"
        onPress={() => setCurrentView('main')}
      />
      <Text style={styles.headerText}>Starting HCE after user interaction</Text>
      <Text style={styles.text}>
        Tap on the button below to start the HCE session. Then, tap your
        smartphone to the reader.
      </Text>
      <Button title="Begin HCE session&emulation" onPress={doBeginSession} />
      <Text style={styles.text}>{recentEvents}</Text>
    </>
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
  headerText: {
    margin: 10,
    fontSize: 20,
    fontWeight: 'bold',
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

export default UserInteractionHCEView;
