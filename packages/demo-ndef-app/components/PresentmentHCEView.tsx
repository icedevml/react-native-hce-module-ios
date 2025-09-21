import React from 'react';
import {
  Alert,
  AppState,
  Button,
  EventSubscription,
  StyleSheet,
  Text,
} from 'react-native';

import { Buffer } from 'buffer/';

import NativeHCEModule, {
  HCEModuleEvent,
} from '@icedevml/react-native-host-card-emulation/js/NativeHCEModule';
import { createNDEFType4TagApp } from '../ndefType4TagApp.ts';

interface IProps {
  setCurrentView: (view: string) => void;
}

interface ITryObtainExclusiveNFCProps {
  setCurrentView: (view: string) => void;
  setExclusiveNFCAcquired: (isAcquired: boolean) => void;
}

const tryObtainExclusiveNFC = ({
  setCurrentView,
  setExclusiveNFCAcquired,
}: ITryObtainExclusiveNFCProps) => {
  (async () => {
    console.log('try to acquire exclusive NFC access');
    try {
      await NativeHCEModule.acquireExclusiveNFC();
      setExclusiveNFCAcquired(true);
      console.log('exclusive NFC acquired');
    } catch (e) {
      console.log('failed to acquire exclusive NFC', e);
      Alert.alert(
        'Error',
        'Failed to acquire exclusive NFC access. You might be in the 15s cooldown period.\n\n' +
          (e as Error).toString(),
      );
      setCurrentView('main');
    }
  })();
};

function PresentmentHCEView({ setCurrentView }: IProps): React.JSX.Element {
  const [exclusiveNFCAcquired, setExclusiveNFCAcquired] = React.useState(false);
  const [recentEventsList, setRecentEventsList] = React.useState<string[]>([]);
  const [getNDEFAppState, resetNDEFAppState, handleCAPDU] = React.useMemo(
    () => createNDEFType4TagApp(),
    [],
  );

  // effect: release exclusive NFC access after app went to background / this view exits
  React.useEffect(() => {
    const subscription = AppState.addEventListener('change', nextAppState => {
      if (nextAppState === 'background') {
        NativeHCEModule.releaseExclusiveNFC();
        setExclusiveNFCAcquired(false);
      }
    });

    return () => {
      NativeHCEModule.releaseExclusiveNFC();
      subscription.remove();
    };
  }, []);

  // effect: try to obtain exclusive NFC access after the view is loaded
  React.useEffect(() => {
    tryObtainExclusiveNFC({ setCurrentView, setExclusiveNFCAcquired });
  }, [setCurrentView, setExclusiveNFCAcquired]);

  // effect: periodically check if we still have exclusive NFC access
  // navigate back to the main view if we don't have it anymore
  // start HCE whenever reader is detected and stop it with success whenever NDEF is read
  React.useEffect(() => {
    let isMounted = true;
    let timeoutTriggered = false;
    let hceTriggered = false;
    let subscription: EventSubscription | null = null;

    const checkIntervalId = setInterval(() => {
      if (
        !timeoutTriggered &&
        !hceTriggered &&
        !NativeHCEModule.isExclusiveNFC()
      ) {
        console.log('exclusive NFC timeout triggered');
        NativeHCEModule.releaseExclusiveNFC();
        timeoutTriggered = true;
        setCurrentView('main');
      }
    }, 100);

    (async () => {
      if (!isMounted) {
        return;
      }

      subscription = NativeHCEModule.onEvent(async (event: HCEModuleEvent) => {
        try {
          console.log('received event', event);
          setRecentEventsList(prevState => {
            const data = [...prevState, event.type + ' ' + event.arg];
            return data.slice(-15);
          });

          switch (event.type) {
            case 'readerDetected':
              hceTriggered = true;
              resetNDEFAppState();
              NativeHCEModule.setSessionAlertMessage(
                'Detected reader.',
              );
              if (!(await NativeHCEModule.isHCERunning())) {
                await NativeHCEModule.startHCE();
              }
              break;

            case 'readerDeselected':
              console.log('wasNDEFRead?', getNDEFAppState().wasNDEFRead);
              if (
                getNDEFAppState().wasNDEFRead &&
                (await NativeHCEModule.isHCERunning())
              ) {
                await NativeHCEModule.stopHCE('success');
                hceTriggered = false;
              }
              break;

            case 'sessionInvalidated':
              setCurrentView('main');
              break;

            case 'received':
              const capdu = Buffer.from(event.arg!, 'hex');
              await NativeHCEModule.respondAPDU(
                null,
                handleCAPDU(capdu).toString('hex'),
              );

              if (getNDEFAppState().wasNDEFRead) {
                NativeHCEModule.setSessionAlertMessage(
                  'Done. Untap smartphone from the reader.',
                );
              }
              break;
          }
        } catch (err) {
          console.error('error in event handler', err);
        }
      });

      await NativeHCEModule.beginSession();

      if (!isMounted) {
        NativeHCEModule.invalidateSession();
        subscription.remove();
      }
    })();

    return () => {
      isMounted = false;
      clearInterval(checkIntervalId);
      NativeHCEModule.invalidateSession();

      if (subscription) {
        subscription.remove();
      }
    };
  }, [getNDEFAppState, resetNDEFAppState, handleCAPDU, setCurrentView]);

  let recentEvents = recentEventsList.join('\n');

  if (!exclusiveNFCAcquired) {
    return <Text style={styles.text}>Please wait...</Text>;
  }

  return (
    <>
      <Button
        title="Back to main menu"
        onPress={() => setCurrentView('main')}
      />
      <Text style={styles.headerText}>
        HCE interaction with Presentment Intent (iOS)
      </Text>
      <Text style={styles.text}>
        Exclusive NFC access was acquired for 15 seconds. Tap the smartphone to
        the reader.
      </Text>
      <Text style={styles.text}>
        The HCE interaction will start automatically after the reader field is
        detected.
      </Text>
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

export default PresentmentHCEView;
