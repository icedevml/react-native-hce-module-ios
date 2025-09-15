/**
 * @format
 */

import { AppRegistry } from 'react-native';
import App from './App';
import runBackground from './background';
import { name as appName } from './app.json';

AppRegistry.registerComponent(appName, () => App);
AppRegistry.registerHeadlessTask('handleBackgroundHCECall', async () => {
  console.log('started background task');
  await runBackground();
  console.log('end of background task');
});
