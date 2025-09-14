/**
 * @format
 */

import { AppRegistry } from 'react-native';
import App from './App';
import { name as appName } from './app.json';

AppRegistry.registerComponent(appName, () => App);
AppRegistry.registerHeadlessTask('handleBackgroundHCECall', async () => {
  // TODO start handling HCE here
  console.log('hello from task');
  console.log('hello from task');
  console.log('hello from task');
});
