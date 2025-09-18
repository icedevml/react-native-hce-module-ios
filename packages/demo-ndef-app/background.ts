import { Buffer } from 'buffer/';

import {
  ProcessBackgroundHCEFunc,
} from '@icedevml/react-native-host-card-emulation/js/hceBackground';
import { createNDEFApp } from './ndefApp';


export default async function runBackgroundHCETask(processBackgroundHCE: ProcessBackgroundHCEFunc) {
  const ndefApp = createNDEFApp()

  processBackgroundHCE(async (event, respondAPDU) => {
    switch (event.type) {
      case 'received':
        const capdu = Buffer.from(event.arg!, 'hex');
        await respondAPDU(ndefApp(capdu).toString("hex"));
        break;

      case 'readerDeselected':
        break;
    }
  });
}
