import { Buffer } from 'buffer/';

import NativeHCEModule, {
  HCEModuleBackgroundEvent,
} from '@icedevml/react-native-host-card-emulation/js/NativeHCEModule';


export default async function run() {
  console.log('background:run()')

  let subscription = NativeHCEModule?.onBackgroundEvent(async (event: HCEModuleBackgroundEvent) => {
    try {
      console.log('background:received event', event);

      switch (event.type) {
        case 'readerDeselected':
          console.log('remove subscription');
          subscription.remove();
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
          await NativeHCEModule?.respondAPDU(rapdu.toString('hex'));
          console.log('responded');
          break;
      }

      console.log('end of handler');
    } catch (err) {
      console.log('error in event handler');
      console.error('error in event handler', err);
    }

    console.log('background:end of onEvent');
  });

  await NativeHCEModule?.initBackgroundHCE();
  console.log('background:end of run');
}
