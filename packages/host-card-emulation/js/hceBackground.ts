import NativeHCEModule, {HCEModuleBackgroundEvent} from "./NativeHCEModule";

export interface BackgroundEventHandler {
    (
        event: HCEModuleBackgroundEvent,
        respondAPDU: (rapdu: string) => Promise<void>,
    ): Promise<void>
}

export interface ProcessBackgroundHCEFunc {
    (
        handler: BackgroundEventHandler
    ): void
}

export const createBackgroundHCE = (handle: string) => {
    const respondAPDU = (rapdu: string) => {
        return NativeHCEModule.respondAPDU(handle, rapdu);
    }

    return (handler: BackgroundEventHandler) => {
        const subscription = NativeHCEModule.onBackgroundEvent(async (event) => {
            if (event.audience !== handle) {
                // ignore
                return;
            }

            try {
                await handler(event, respondAPDU);
            } catch (e) {
                throw e;
            } finally {
                if (event.type === "readerDeselected") {
                    subscription.remove();
                }
            }
        });

        NativeHCEModule.initBackgroundHCE(handle);
    }
}
