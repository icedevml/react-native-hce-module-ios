import React from 'react';
import { Button, StyleSheet, Text } from 'react-native';

interface IProps {
  setCurrentView: (view: string) => void;
}

function MainView({ setCurrentView }: IProps): React.JSX.Element {
  return (
    <>
      <Text style={styles.text}>Select demo:</Text>
      <Button
        title="Demo: Start HCE after user interaction (iOS/Android)"
        onPress={() => {
          setCurrentView('user-interaction');
        }}
      />
      <Button
        title="Demo: HCE interaction with Presentment Intent (iOS only)"
        onPress={() => {
          setCurrentView('ios-presentment');
        }}
      />
      <Button
        title="Demo: Continous HCE operation"
        onPress={() => {
          setCurrentView('continous');
        }}
      />
      <Text style={styles.text}>
        Note: On Android, you can also perform the HCE interaction even if the
        app is not running / is in background. This is handled by React
        background task implemented in background.ts file.
      </Text>
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

export default MainView;
