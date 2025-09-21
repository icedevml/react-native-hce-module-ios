import React from 'react';
import { Button, EventSubscription, StyleSheet, Text } from 'react-native';

import { Buffer } from 'buffer/';

import NativeHCEModule, {
  HCEModuleEvent,
} from '@icedevml/react-native-host-card-emulation/js/NativeHCEModule';
import { createNDEFType4TagApp } from '../ndefType4TagApp.ts';

interface IProps {
  setCurrentView: (view: string) => void;
}

function ContinousHCEView({ setCurrentView }: IProps): React.JSX.Element {
  const [recentEventsList, setRecentEventsList] = React.useState<string[]>([]);
  const [getNDEFAppState, resetNDEFAppState, handleCAPDU] = React.useMemo(
    () => createNDEFType4TagApp(),
    [],
  );

  React.useEffect(() => {
    let isMounted = true;
    let subscription: EventSubscription | null = null;

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
              resetNDEFAppState();
              break;

            case 'readerDeselected':
              break;

            case 'sessionStarted':
              await NativeHCEModule.startHCE();
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
              break;
          }
        } catch (err) {
          console.error('error in event handler', err);
        }
      });

      await NativeHCEModule.beginSession();

      if (!isMounted) {
        NativeHCEModule.invalidateSession();
      }
    })();

    return () => {
      NativeHCEModule.invalidateSession();
      subscription?.remove();
    };
  }, [getNDEFAppState, resetNDEFAppState, handleCAPDU, setCurrentView]);

  let recentEvents = recentEventsList.join('\n');

  return (
    <>
      <Button
        title="Back to main menu"
        onPress={() => setCurrentView('main')}
      />
      <Text style={styles.headerText}>Continous HCE operation (Android)</Text>
      <Text style={styles.text}>
        Tap the smartphone to the reader. The HCE will be operating continuously
        until this view is exited.
      </Text>
      <Text style={styles.text}>
        On iOS, this operation is time-constrained to 60 seconds.
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

export default ContinousHCEView;
