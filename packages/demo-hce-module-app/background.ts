import { Buffer } from 'buffer/';

import NativeHCEModule, {
  HCEModuleBackgroundEvent,
} from '@icedevml/react-native-host-card-emulation/js/NativeHCEModule';


export default async function run() {
  console.debug('background:run()')

  let currentFile: Buffer | null = null;

  const fileE103 = Buffer.from("001720010000FF0406E10401000000", "hex");
  const fileE103Padded = Buffer.concat([
    fileE103,
    Buffer.alloc(32 - fileE103.length)
  ]);

  const fileE104 = Buffer.from("0019D101155504796F7574752E62652F6451773477395767586351", "hex");
  const fileE104Padded = Buffer.concat([
    fileE104,
    Buffer.alloc(256 - fileE104.length)
  ]);

  const files: Record<number, Buffer> = {
    0xE103: fileE103Padded,
    0xE104: fileE104Padded
  };

  let subscription = NativeHCEModule?.onBackgroundEvent(async (event: HCEModuleBackgroundEvent) => {
    try {
      console.debug('background:received event', event);

      const cmdHandlers: Record<string, (capdu: Buffer) => Buffer> = {
        // TODO better handler registration
        // TODO better APDU parsing
        // TODO custom exception to throw error codes + enum
        // TODO dynamically construct NDEF using ndeflib

        "00A4": (capdu) => {
          if (capdu.slice(0, 4).compare(Buffer.from([0x00, 0xA4, 0x04, 0x00])) === 0) {
            // select applet
            return Buffer.from([0x90, 0x00]);
          } else if (capdu.slice(0, 4).compare(Buffer.from([0x00, 0xA4, 0x00, 0x0C])) === 0) {
            // select file by ID
            if (capdu[4] !== 0x02) {
              return Buffer.from([0x67, 0x00]);
            }

            const fileId = capdu.slice(5, 7).readUInt16BE(0);
            if (!files.hasOwnProperty(fileId)) {
              return Buffer.from([0x6A, 0x82]);
            }

            currentFile = files[fileId];
            return Buffer.from([0x90, 0x00]);
          } else {
            return Buffer.from([0x6A, 0x00]);
          }
        },
        "00B0": (capdu) => {
          const offset = capdu.slice(2, 4).readUInt16BE(0);
          const le = capdu[4];

          if (currentFile === null) {
            return Buffer.from([0x69, 0x85]);
          }

          return Buffer.concat([
            currentFile.slice(offset, offset+le),
            Buffer.from([0x90, 0x00])
          ]);
        }
      }

      const handleAPDU = (capdu: Buffer): Buffer => {
        const prefix = capdu.slice(0, 2).toString("hex").toUpperCase();

        // validate class
        if (capdu[0] !== 0x00) {
          return Buffer.from([0x6E, 0x00])
        }

        // find handler for CLA/INS
        if (!cmdHandlers.hasOwnProperty(prefix)) {
          return Buffer.from([0x6D, 0x00]);
        }

        return cmdHandlers[prefix](capdu)
      }

      switch (event.type) {
        case 'received':
          const capdu = Buffer.from(event.arg!, 'hex');
          await NativeHCEModule?.respondAPDU(handleAPDU(capdu).toString("hex"));
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
