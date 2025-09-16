/**
 * @format
 */

import { AppRegistry } from 'react-native';
import App from './App';
import runBackground from './background';
import { name as appName } from './app.json';

AppRegistry.registerComponent(appName, () => App);
AppRegistry.registerHeadlessTask('handleBackgroundHCECall', () => {
  console.log('background:called taskProvider');

  return async () => {
    console.log('background:runBackground start');
    const res = await runBackground();
    console.log('background:runBackground done');
    return res;
  }
});
