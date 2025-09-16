import { Buffer } from 'buffer/';

import NativeHCEModule, {
  HCEModuleBackgroundEvent,
} from '@icedevml/react-native-host-card-emulation/js/NativeHCEModule';


export default async function run() {
  console.debug('background:run()')

  let subscription = NativeHCEModule?.onBackgroundEvent(async (event: HCEModuleBackgroundEvent) => {
    try {
      console.debug('background:received event', event);

      switch (event.type) {
        case 'received':
          const capdu = Buffer.from(event.arg!, 'hex');
          // for the demo, we just respond with the reversed C-APDU and success status 9000
          const rapdu = Buffer.concat([
            Buffer.from(event.arg!, 'hex').reverse(),
            Buffer.from([0x90, 0x00]),
          ]);

          console.debug('Received C-APDU: ', capdu.toString('hex'));
          console.debug('Sending R-APDU: ', rapdu.toString('hex'));

          console.debug('respond apdu', NativeHCEModule?.respondAPDU);
          await NativeHCEModule?.respondAPDU(rapdu.toString('hex'));
          console.debug('responded');
          break;

        case 'readerDeselected':
          console.debug('remove subscription');
          subscription.remove();
          break;
      }

      console.debug('end of handler');
    } catch (err) {
      console.error('error in background handler', err);
    }

    console.debug('background:end of onEvent');
  });

  await NativeHCEModule?.initBackgroundHCE();
  console.debug('background:end of run');
}
