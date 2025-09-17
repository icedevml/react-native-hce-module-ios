import { Buffer } from 'buffer/';

import NativeHCEModule, {
  HCEModuleBackgroundEvent,
} from '@icedevml/react-native-host-card-emulation/js/NativeHCEModule';
import { createNDEFApp } from './ndefApp';


export default async function run() {
  console.debug('background:run()')
  const ndefApp = createNDEFApp()

  let subscription = NativeHCEModule?.onBackgroundEvent(async (event: HCEModuleBackgroundEvent) => {
    try {
      console.debug('background:received event', event);

      switch (event.type) {
        case 'received':
          const capdu = Buffer.from(event.arg!, 'hex');
          await NativeHCEModule?.respondAPDU(ndefApp(capdu).toString("hex"));
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
