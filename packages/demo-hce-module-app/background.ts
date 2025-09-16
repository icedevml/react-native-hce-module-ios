import { Buffer } from 'buffer/';

import NativeHCEModule, {HCEModuleEvent} from '@icedevml/react-native-host-card-emulation/js/NativeHCEModule';


export default async function run() {
  // TODO un-duplicate code
  console.log('background:run()')

  let subscription = NativeHCEModule?.onEvent(async (event: HCEModuleEvent) => {
    try {
      console.log('background:received event', event);

      switch (event.type) {
        case 'readerDetected':
          NativeHCEModule?.setSessionAlertMessage('Reader detected');
          break;

        case 'readerDeselected':
          NativeHCEModule?.setSessionAlertMessage('Lost reader');
          console.log('remove subscription');
          subscription.remove();
          NativeHCEModule?.invalidateSession();
          break;

        case 'sessionStarted':
          NativeHCEModule?.setSessionAlertMessage(
            'Tap towards the reader',
          );
          await NativeHCEModule?.startHCE();
          break;

        case 'sessionInvalidated':
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

          console.log('respond apdu', NativeHCEModule?.respondAPDU);
          console.log(NativeHCEModule?.respondAPDU(rapdu.toString('hex')));
          console.log('responded');

          /* if (capdu[0] === 0xb0 && capdu[1] === 0xff) {
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
          } */
          break;
      }

      console.log('end of handler');
    } catch (err) {
      console.log('error in event handler');
      console.error('error in event handler', err);
    }

    console.log('background:end of run()')
  });

  console.log('background:beginSession() await');
  await NativeHCEModule?.beginSession();
}
