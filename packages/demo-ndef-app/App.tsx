import React from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { SafeAreaProvider, SafeAreaView } from 'react-native-safe-area-context';

import MainView from './components/MainView.tsx';
import UserInteractionHCEView from './components/UserInteractionHCEView.tsx';
import PresentmentHCEView from './components/PresentmentHCEView.tsx';
import ContinousHCEView from './components/ContinousHCEView.tsx';

function App(): React.JSX.Element {
  const [currentView, setCurrentView] = React.useState<string>("main");

  const renderCurrentView = () => {
    if (currentView === 'main') {
      return <MainView setCurrentView={setCurrentView} />;
    } else if (currentView === 'user-interaction') {
      return <UserInteractionHCEView setCurrentView={setCurrentView} />;
    } else if (currentView === 'ios-presentment') {
      return <PresentmentHCEView setCurrentView={setCurrentView} />;
    } else if (currentView === 'continous') {
      return <ContinousHCEView setCurrentView={setCurrentView} />;
    }
  };

  return (
    <SafeAreaProvider>
      <SafeAreaView style={styles.safeAreaView}>
        <Text style={styles.headerText}>Demo App for Native HCE Module</Text>
        <Text style={styles.headerText}>
          GitHub: icedevml/react-native-host-card-emulation
        </Text>
        <View
          style={{
            borderBottomColor: 'black',
            borderBottomWidth: StyleSheet.hairlineWidth,
          }}
        />
        {renderCurrentView()}
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

export default App;
